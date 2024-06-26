package no.nav.dagpenger.saksbehandling.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.UtsendingMediator
import no.nav.dagpenger.saksbehandling.mottak.BehovLøsningMottak.Companion.ARKIVERBART_DOKUMENT_BEHOV
import no.nav.dagpenger.saksbehandling.mottak.BehovLøsningMottak.Companion.DISTRIBUERING_BEHOV
import no.nav.dagpenger.saksbehandling.mottak.BehovLøsningMottak.Companion.JOURNALFØRING_BEHOV
import no.nav.dagpenger.saksbehandling.toUrn
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalførtHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class BehovLøsningMottak(
    rapidsConnection: RapidsConnection,
    private val utsendingMediator: UtsendingMediator,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogger = KotlinLogging.logger("tjenestekall")
        const val ARKIVERBART_DOKUMENT_BEHOV = "ArkiverbartDokumentBehov"
        const val JOURNALFØRING_BEHOV = "JournalføringBehov"
        const val DISTRIBUERING_BEHOV = "DistribueringBehov"
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.requireKey("@løsning") }
            validate { it.requireKey("oppgaveId") }
            validate { it.rejectKey("@final") }
            validate { it.interestedIn("journalpostId") }
            validate { it.interestedIn("urn") }
            validate {
                it.requireAllOrAny(
                    "@behov",
                    listOf(ARKIVERBART_DOKUMENT_BEHOV, JOURNALFØRING_BEHOV, DISTRIBUERING_BEHOV),
                )
            }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val oppgaveId = packet["oppgaveId"].asText()
        withLoggingContext(
            "oppgaveId" to oppgaveId,
        ) {
            if (oppgaveId in setOf("01904f1f-0c8d-7216-890b-8fd8b6ae2494")) {
                return
            }

            val typeLøsning =
                packet.get("@behov").first().asText().also {
                    logger.info { "Mottok løsning for behov: $it" }
                }
            try {
                when (typeLøsning) {
                    ARKIVERBART_DOKUMENT_BEHOV -> {
                        utsendingMediator.mottaUrnTilArkiverbartFormatAvBrev(packet.arkiverbartDokumentLøsning())
                    }

                    JOURNALFØRING_BEHOV -> {
                        utsendingMediator.mottaJournalførtKvittering(packet.journalførtLøsning())
                    }

                    DISTRIBUERING_BEHOV -> {
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
        oppgaveId = this["oppgaveId"].asUUID(),
        journalpostId = this["@løsning"][JOURNALFØRING_BEHOV]["journalpostId"].asText(),
    )
}

private fun JsonMessage.distribuertLøsning(): DistribuertHendelse {
    return DistribuertHendelse(
        oppgaveId = this["oppgaveId"].asUUID(),
        distribusjonId = this["@løsning"][DISTRIBUERING_BEHOV]["distribueringId"].asText(),
        journalpostId = this["journalpostId"].asText(),
    )
}

private fun JsonMessage.arkiverbartDokumentLøsning(): ArkiverbartBrevHendelse {
    return ArkiverbartBrevHendelse(
        oppgaveId = this["oppgaveId"].asUUID(),
        pdfUrn = this["@løsning"][ARKIVERBART_DOKUMENT_BEHOV]["urn"].asText().toUrn(),
    )
}
