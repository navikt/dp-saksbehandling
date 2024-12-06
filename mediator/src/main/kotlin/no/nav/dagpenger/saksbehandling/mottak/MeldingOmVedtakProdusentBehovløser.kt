package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.River.PacketListener
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class MeldingOmVedtakProdusentBehovløser(
    rapidsConnection: RapidsConnection,
    private val utsendingMediator: UtsendingMediator,
) : PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireAll("@behov", listOf("MeldingOmVedtakProdusent"))
                it.forbid("@løsning")
            }
            validate { it.requireKey("ident", "behandlingId") }
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
        val behandlingId = packet["behandlingId"].asText().let { UUID.fromString(it) }
        withLoggingContext("behandlingId" to "$behandlingId") {
            utsendingMediator.utsendingFinnesForBehandling(behandlingId).let {
                when (it) {
                    true -> {
                        packet["@løsning"] = mapOf("MeldingOmVedtakProdusent" to "Dagpenger")
                        logger.info { "MeldingOmVedtakProdusent er Dagpenger" }
                    }
                    false -> {
                        packet["@løsning"] = mapOf("MeldingOmVedtakProdusent" to "Arena")
                        logger.info { "MeldingOmVedtakProdusent er Arena" }
                    }
                }
            }
            context.publish(packet.toJson())
        }
    }
}
