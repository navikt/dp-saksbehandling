package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class BehovLøsningMediator(
    private val repository: UtsendingRepository,
    private val rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "behandling_opprettet") }
            validate { it.requireKey("ident", "søknadId", "behandlingId", "@Løsning") }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val typeLøsning: String = packet.get("@Løsning").toString()

        when (typeLøsning) {
            "NyJournalpost" -> {
                logger.info { "Fått løsning for NyJournalpost" }
            }

            else -> {
                logger.info { "Fått løsning for $typeLøsning" }
            }
        }
    }
}
