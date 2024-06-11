package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribueringKvitteringHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalpostHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class BehovLøsningMottak(
    private val utsendingMediator: UtsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.requireKey("@løsning") }
            validate { it.requireKey("oppgaveId") }
            validate { it.interestedIn("journalpostId") }
            validate {
                it.requireAllOrAny(
                    "@behov",
                    listOf("ArkiverbartDokumentBehov", "JournalføringBehov", "DistribueringBehov"),
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
        val typeLøsning = packet.get("@behov").first().asText()

        when (typeLøsning) {
            "ArkiverbartDokumentBehov" -> {
                utsendingMediator.mottaUrnTilArkiverbartFormatAvBrev(packet.arkiverbartDokumentLøsning())
            }

            "JournalføringBehov" -> {
                utsendingMediator.mottaJournalpost(packet.journalførtLøsning())
            }

            "DistribueringBehov" -> {
                utsendingMediator.mottaDistribueringKvittering(packet.distribuertKvittering())
            }

            else -> {
                throw IllegalStateException("Ukjent behov: $typeLøsning")
            }
        }
    }
}

private fun JsonMessage.journalførtLøsning(): JournalpostHendelse {
    return JournalpostHendelse(
        oppgaveId = this["oppgaveId"].asUUID(),
        journalpostId = this["@løsning"]["JournalføringBehov"]["journalpostId"].asText(),
    )
}

private fun JsonMessage.distribuertKvittering(): DistribueringKvitteringHendelse {
    return DistribueringKvitteringHendelse(
        oppgaveId = this["oppgaveId"].asUUID(),
        distribueringId = this["@løsning"]["DistribueringBehov"]["distribueringId"].asText(),
        journalpostId = this["journalpostId"].asText(),
    )
}

private fun JsonMessage.arkiverbartDokumentLøsning(): ArkiverbartBrevHendelse {
    return ArkiverbartBrevHendelse(
        oppgaveId = this["oppgaveId"].asUUID(),
        pdfUrn = this["@løsning"]["ArkiverbartDokument"]["urn"].asText().toUrn(),
    )
}
