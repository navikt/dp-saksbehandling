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
import java.util.UUID

internal class HenvendelseBehovløser(
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
                        "registrertDato",
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
                val sisteSakId = sakMediator.finnSisteSakId(packet["fødselsnummer"].asText())
                when (sisteSakId != null) {
                    true -> {
                        klageMediator.opprettKlage(
                            KlageMottattHendelse(
                                ident = ident,
                                opprettet = registrertDato,
                                journalpostId = journalpostId,
                                sakId = sisteSakId,
                                utførtAv = Applikasjon("dp-saksbehandling"),
                            ),
                        )
                        packet.lagLøsning(håndtert = true, sakId = sisteSakId)
                    }
                    false -> packet.lagLøsning(håndtert = false)
                }
            }

//            Kategori.NY_SØKNAD -> {
//                val sisteSakId = sakMediator.finnSisteSakId(packet["fødselsnummer"].asText())
//                when (sisteSakId != null) {
//                    true -> {
//                        // TODO opprett henvendelse??
//                        // TODO skal vi alltid svare med siste sak og fikse journalføring i ettertid? Høna og egget...
//                        packet.lagLøsning(håndtert = true, sakId = sisteSakId)
//                    }
//                    false -> packet.lagLøsning(håndtert = false)
//                }
//            }
            else -> packet.lagLøsning(håndtert = false)
        }
        context.publish(key = ident, message = packet.toJson())
    }

    private fun JsonMessage.lagLøsning(
        håndtert: Boolean,
        sakId: UUID? = null,
    ) {
        when (sakId != null) {
            true ->
                this["@løsning"] =
                    mapOf(
                        "sakId" to sakId,
                        "håndtert" to håndtert,
                    )
            false ->
                this["@løsning"] =
                    mapOf(
                        "håndtert" to håndtert,
                    )
        }
    }

    enum class Kategori {
        NY_SØKNAD,
        GJENOPPTAK,
        GENERELL,
        UTDANNING,
        ETABLERING,
        KLAGE,
        ANKE,
        KLAGE_FORSKUDD,
        ETTERSENDING,
        UKJENT_SKJEMA_KODE,
        UTEN_BRUKER,
    }
}
