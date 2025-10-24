package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

internal class HenvendelseBehovløser(
    rapidsConnection: RapidsConnection,
    private val sakMediator: SakMediator,
    private val klageMediator: KlageMediator,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        val behovNavn: String = "HåndterHenvendelse"
        private val logger = KotlinLogging.logger {}
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
                    it.interestedIn("søknadId")
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
        withLoggingContext("journalpostId" to journalpostId, "kategori" to kategori.name) {
            when (kategori) {
                Kategori.KLAGE -> {
                    val sisteSakId = sakMediator.finnSisteSakId(ident)
                    val løsning =
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
                                lagLøsning(håndtert = true, sakId = sisteSakId)
                            }

                            false -> lagLøsning(håndtert = false)
                        }
                    packet["@løsning"] =
                        løsning.also {
                            logger.info { "Laget løsning for klage: $it" }
                        }
                }

                Kategori.ANKE -> {
                    val sisteSakId = sakMediator.finnSisteSakId(ident)
                    val løsning =
                        when (sisteSakId != null) {
                            true -> {
                                // TODO Skal denne sendes rett til KA eller opprett henvendelse og ta det derfra?
                                lagLøsning(håndtert = true, sakId = sisteSakId)
                            }

                            false -> lagLøsning(håndtert = false)
                        }
                    packet["@løsning"] =
                        løsning.also {
                            logger.info { "Laget løsning for anke: $it" }
                        }
                }

                Kategori.ETTERSENDING -> {
                    if (!packet["søknadId"].isMissingNode && !packet["søknadId"].isNull) {
                        val søknadId = packet["søknadId"].asUUID()
                        if (oppgaveMediator.skalEttersendingTilSøknadVarsles(ident = ident, søknadId = søknadId)) {
                            // TODO opprett henvendelse
                        }
                    }
                    val sisteSakId = sakMediator.finnSisteSakId(ident)
                    val løsning =
                        when (sisteSakId != null) {
                            // TODO skal vi alltid svare med siste sak og fikse journalføring i ettertid? Høna og egget...
                            true -> lagLøsning(håndtert = true, sakId = sisteSakId)
                            false -> lagLøsning(håndtert = false)
                        }
                    packet["@løsning"] =
                        løsning.also {
                            logger.info { "Laget løsning for anke: $it" }
                        }
                }

                else -> {
                    packet["@løsning"] =
                        lagLøsning(false).also {
                            logger.info { "Laget løsning for ${kategori.name}: $it" }
                        }
                }
            }
            packet["@final"] = true
            context.publish(key = ident, message = packet.toJson())
        }
    }

    private fun lagLøsning(
        håndtert: Boolean,
        sakId: UUID? = null,
    ): Map<String, Any> {
        return when (sakId != null) {
            true -> {
                mapOf(
                    "sakId" to sakId,
                    "håndtert" to håndtert,
                )
            }

            false -> {
                mapOf(
                    "håndtert" to håndtert,
                )
            }
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
