package no.nav.dagpenger.saksbehandling.utsending.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import no.nav.dagpenger.saksbehandling.toUrn
import no.nav.dagpenger.saksbehandling.utsending.ArkiverbartBrevBehov
import no.nav.dagpenger.saksbehandling.utsending.DistribueringBehov
import no.nav.dagpenger.saksbehandling.utsending.JournalføringBehov
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalførtHendelse

class UtsendingBehovLøsningMottak(
    rapidsConnection: RapidsConnection,
    private val utsendingMediator: UtsendingMediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogger = KotlinLogging.logger("tjenestekall")
        val behovListe =
            setOf(
                ArkiverbartBrevBehov.BEHOV_NAVN,
                JournalføringBehov.BEHOV_NAVN,
                DistribueringBehov.BEHOV_NAVN,
            ).toList()
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireAllOrAny(
                    key = "@behov",
                    values = behovListe,
                )
                it.forbid("@final")
            }
            validate { it.requireKey("@løsning") }
            validate { it.requireKey("behandlingId") }
            validate { it.interestedIn("journalpostId") }
            validate { it.interestedIn("urn") }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asText()
        withLoggingContext(
            "behandlingId" to behandlingId,
        ) {
            val typeLøsning =
                packet.get("@behov").first().asText().also {
                    logger.info { "Mottok løsning for behov: $it" }
                }
            try {
                when (typeLøsning) {
                    ArkiverbartBrevBehov.BEHOV_NAVN -> {
                        utsendingMediator.mottaUrnTilArkiverbartFormatAvBrev(packet.arkiverbartDokumentLøsning())
                    }

                    JournalføringBehov.BEHOV_NAVN -> {
                        utsendingMediator.mottaJournalførtKvittering(packet.journalførtLøsning())
                    }

                    DistribueringBehov.BEHOV_NAVN -> {
                        utsendingMediator.mottaDistribuertKvittering(packet.distribuertLøsning())
                    }

                    else -> {
                        throw IllegalStateException("Ukjent behov: $typeLøsning")
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Uhåndtert feil: $e" }
                sikkerlogger.error(e) { "Uhåndtert feil ved mottak av: ${packet.toJson()}" }
                throw e
            }
        }
    }
}

private fun JsonMessage.journalførtLøsning(): JournalførtHendelse {
    return JournalførtHendelse(
        behandlingId = this["behandlingId"].asUUID(),
        journalpostId = this["@løsning"][JournalføringBehov.BEHOV_NAVN]["journalpostId"].asText(),
    )
}

private fun JsonMessage.distribuertLøsning(): DistribuertHendelse {
    return DistribuertHendelse(
        behandlingId = this["behandlingId"].asUUID(),
        distribusjonId = this["@løsning"][DistribueringBehov.BEHOV_NAVN]["distribueringId"].asText(),
        journalpostId = this["journalpostId"].asText(),
    )
}

private fun JsonMessage.arkiverbartDokumentLøsning(): ArkiverbartBrevHendelse {
    return ArkiverbartBrevHendelse(
        behandlingId = this["behandlingId"].asUUID(),
        pdfUrn = this["@løsning"][ArkiverbartBrevBehov.BEHOV_NAVN]["urn"].asText().toUrn(),
    )
}
