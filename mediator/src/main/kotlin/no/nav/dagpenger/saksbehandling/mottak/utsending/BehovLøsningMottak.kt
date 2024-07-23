package no.nav.dagpenger.saksbehandling.mottak.utsending

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.UtsendingMediator
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import no.nav.dagpenger.saksbehandling.toUrn
import no.nav.dagpenger.saksbehandling.utsending.ArkiverbartBrevBehov
import no.nav.dagpenger.saksbehandling.utsending.DistribueringBehov
import no.nav.dagpenger.saksbehandling.utsending.JournalføringBehov
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
        val behovListe =
            setOf(
                ArkiverbartBrevBehov.BEHOV_NAVN,
                JournalføringBehov.BEHOV_NAVN,
                DistribueringBehov.BEHOV_NAVN,
            ).toList()
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.requireKey("@løsning") }
            validate { it.requireKey("oppgaveId") }
            validate { it.rejectKey("@final") }
            validate { it.interestedIn("journalpostId") }
            validate { it.interestedIn("urn") }
            validate {
                it.requireAllOrAny(
                    key = "@behov",
                    values = behovListe,
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
        oppgaveId = this["oppgaveId"].asUUID(),
        journalpostId = this["@løsning"][JournalføringBehov.BEHOV_NAVN]["journalpostId"].asText(),
    )
}

private fun JsonMessage.distribuertLøsning(): DistribuertHendelse {
    return DistribuertHendelse(
        oppgaveId = this["oppgaveId"].asUUID(),
        distribusjonId = this["@løsning"][DistribueringBehov.BEHOV_NAVN]["distribueringId"].asText(),
        journalpostId = this["journalpostId"].asText(),
    )
}

private fun JsonMessage.arkiverbartDokumentLøsning(): ArkiverbartBrevHendelse {
    return ArkiverbartBrevHendelse(
        oppgaveId = this["oppgaveId"].asUUID(),
        pdfUrn = this["@løsning"][ArkiverbartBrevBehov.BEHOV_NAVN]["urn"].asText().toUrn(),
    )
}
