package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator

internal class HenvendelseMottak(
    rapidsConnection: RapidsConnection,
    private val sakMediator: SakMediator,
    private val klageMediator: KlageMediator,
) : River.PacketListener {

    companion object {
        val behovNavn: String = "HåndterHenvendelse"
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition {
                    it.requireAllOrAny(
                        "@behov",
                        listOf(behovNavn),
                    )
                    it.forbid("@løsning")
                    it.forbid("@feil")
                }
                validate {
                    it.requireKey(
                        "@behovId",
                        "journalpostId",
                        "fødselsnummer",
                        "kategori",
                        "registrertDato"
                    )
                    it.interestedIn("søknadsId")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {

        val kategori: Kategori = Kategori.valueOf(packet["kategori"].asText())
        val ident = packet["fødselsnummer"].asText()
        val registrertDato = packet["registrertDato"].asLocalDateTime()
        val journalpostId = packet["journalpostId"].asText()


        when (kategori) {
            Kategori.KLAGE -> {
                val finnSisteSakId = sakMediator.finnSisteSakId(packet["fødselsnummer"].asText())
                when (finnSisteSakId != null) {
                    true -> {
                        klageMediator.opprettKlage(
                            KlageMottattHendelse(
                                ident = ident,
                                opprettet = registrertDato,
                                journalpostId = journalpostId,
                                sakId = finnSisteSakId,
                                utførtAv = Applikasjon("dp-saksbehandling"),
                            )
                        )
                        packet["@løsning"] = JsonMessage.newMessage(
                            mapOf(
                                "sakId" to finnSisteSakId,
                                "håndtert" to true
                            ),
                        )
                    }

                    false -> {
                        packet["@løsning"] = JsonMessage.newMessage(
                            mapOf(
                                "håndtert" to false
                            ),
                        )
                    }
                }
            }

            Kategori.NY_SØKNAD -> {
                val finnSisteSakId = sakMediator.finnSisteSakId(packet["fødselsnummer"].asText())
                when(finnSisteSakId != null) {
                    true -> TODO()
                    false -> TODO()
                }
            }




            else -> {
                packet["@løsning"] = JsonMessage.newMessage(
                    mapOf(
                        "håndtert" to false
                    ),
                )
            }
        }
        context.publish(packet.toJson())

    }

    enum class Kategori {
        KLAGE,
        ANKE,
        KLAGE_FORSKUDD,
        ETTERSENDING,
        UKJENT_SKJEMA_KODE,
        UTEN_BRUKER,
        NY_SØKNAD,
    }
}