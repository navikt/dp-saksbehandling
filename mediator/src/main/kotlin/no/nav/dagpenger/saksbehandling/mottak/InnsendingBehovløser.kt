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
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.innsending.HåndterInnsendingResultat
import no.nav.dagpenger.saksbehandling.innsending.InnsendingMediator
import java.util.UUID

internal class InnsendingBehovløser(
    rapidsConnection: RapidsConnection,
    private val innsendingMediator: InnsendingMediator,
) : River.PacketListener {
    companion object {
        val behovNavn: String = "HåndterInnsending"
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
            val innsendingMottattHendelse =
                InnsendingMottattHendelse(
                    ident = ident,
                    journalpostId = journalpostId,
                    registrertTidspunkt = packet["registrertDato"].asLocalDateTime(),
                    søknadId = søknadId,
                    skjemaKode = packet["skjemaKode"].asText(),
                    kategori = Kategori.valueOf(packet["kategori"].asText()),
                )
            logger.info { "Skal løse behov $behovNavn for journalpostId $journalpostId og kategori $kategori" }

            val håndterInnsendingResultat = innsendingMediator.taImotInnsending(innsendingMottattHendelse)
            when (håndterInnsendingResultat) {
                is HåndterInnsendingResultat.HåndtertInnsending -> {
                    packet["@løsning"] =
                        mapOf(
                            "$behovNavn" to
                                mapOf(
                                    "sakId" to håndterInnsendingResultat.sakId,
                                    "håndtert" to true,
                                ),
                        )
                }

                HåndterInnsendingResultat.UhåndtertInnsending -> {
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
