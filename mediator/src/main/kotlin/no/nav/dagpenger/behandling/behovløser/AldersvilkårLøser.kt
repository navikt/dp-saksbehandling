package no.nav.dagpenger.behandling.behovløser

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class AldersvilkårLøser(rapidsConnection: RapidsConnection) : River.PacketListener {
    private val logger = KotlinLogging.logger {}
    private val behov = "Aldersbehov"

    init {

        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.requireKey("behandlingId", "ident", "@behovId") }
            validate { it.demandAllOrAny("@behov", listOf(behov)) }
            validate { it.forbid("@løsning") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLoggingContext("behandlingId" to packet["behandlingId"].asText()) {
            packet["@løsning"] = mapOf(behov to true)
            context.publish(packet.toJson())
            logger.info { "Løser $behov" }
        }
    }
}
