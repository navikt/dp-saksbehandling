package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class BehovLøsningMediator(
    private val utsendingMediator: UtsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.requireKey("@løsning") }
            validate { it.requireKey("oppgaveId") }
            validate {
                it.requireAllOrAny(
                    "@behov",
                    listOf("ArkiverbartDokumentBehov", "MidlertidigJournalføringBehov", "DistribueringBehov"),
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
        val typeLøsning: String = packet.get("@behov").first().asText()

        when (typeLøsning) {
            "ArkiverbartDokumentBehov" -> {
                utsendingMediator.mottaUrnTilArkiverbartFormatAvBrev(packet.arkiverbartDokumentLøsning())
            }

            "MidlertidigJournalføringBehov" -> {
                logger.info { "Fått løsning for MidlertidigJournalføringBehov" }
            }

            "DistribueringBehov" -> {
                logger.info { "Fått løsning for DistribueringBehov" }
            }

            else -> {
                logger.info { "Fått løsning for $typeLøsning" }
            }
        }
    }
}

private fun JsonMessage.arkiverbartDokumentLøsning(): ArkiverbartBrevHendelse {
    return ArkiverbartBrevHendelse(
        oppgaveId = this["oppgaveId"].asUUID(),
        pdfUrn = this["@løsning"]["ArkiverbartDokument"]["urn"].asText().toUrn(),
    )
}
