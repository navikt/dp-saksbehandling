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
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.henvendelse.HenvendelseMediator
import no.nav.dagpenger.saksbehandling.henvendelse.HåndterHenvendelseResultat
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

internal class HenvendelseBehovløser(
    rapidsConnection: RapidsConnection,
    private val sakMediator: SakMediator,
    private val klageMediator: KlageMediator,
    private val oppgaveMediator: OppgaveMediator,
    private val henvendelseMediator: HenvendelseMediator,
) : River.PacketListener {
    companion object {
        val behovNavn: String = "HåndterHenvendelse"
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
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
                        "skjemaKode",
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
        val ident = packet["fødselsnummer"].asText()
        val kategori: Kategori = Kategori.valueOf(packet["kategori"].asText())
        val journalpostId = packet["journalpostId"].asText()

        withLoggingContext("journalpostId" to journalpostId, "kategori" to kategori.name) {
            val søknadId: UUID? =
                if (!packet["søknadId"].isMissingNode && !packet["søknadId"].isNull) {
                    packet["søknadId"].asUUID()
                } else {
                    null
                }
            val henvendelseMottattHendelse =
                HenvendelseMottattHendelse(
                    ident = ident,
                    journalpostId = journalpostId,
                    registrertTidspunkt = packet["registrertDato"].asLocalDateTime(),
                    søknadId = søknadId,
                    skjemaKode = packet["skjemaKode"].asText(),
                    kategori = Kategori.valueOf(packet["kategori"].asText()),
                )
            logger.info { "Skal løse behov $behovNavn for journalpostId $journalpostId og kategori $kategori" }

            val håndterHenvendelseResultat = henvendelseMediator.taImotHenvendelse(henvendelseMottattHendelse)
            when (håndterHenvendelseResultat) {
                is HåndterHenvendelseResultat.HåndtertHenvendelse -> {
                    packet["@løsning"] =
                        mapOf(
                            "$behovNavn" to
                                mapOf(
                                    "sakId" to håndterHenvendelseResultat.sakId,
                                    "håndtert" to true,
                                ),
                        )
                }

                HåndterHenvendelseResultat.UhåndtertHenvendelse -> {
                    packet["@løsning"] =
                        mapOf(
                            "$behovNavn" to
                                mapOf(
                                    "håndtert" to false,
                                ),
                        )
                }
            }
            packet["@final"] = true
            context.publish(
                key = ident,
                message =
                    packet.toJson().also {
                        sikkerlogg.info { "Publiserte løsning for behov $behovNavn: $it" }
                    },
            ).also {
                logger.info { "Løste behov $behovNavn for journalpostId $journalpostId og kategori $kategori" }
            }
        }
    }
}
