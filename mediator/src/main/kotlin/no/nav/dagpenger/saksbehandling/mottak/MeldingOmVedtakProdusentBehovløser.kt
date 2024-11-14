package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.River.PacketListener
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import java.util.UUID

internal class MeldingOmVedtakProdusentBehovløser(
    rapidsConnection: RapidsConnection,
    private val utsendingMediator: UtsendingMediator,
) : PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAll("@behov", listOf("MeldingOmVedtakProdusent")) }
            validate { it.requireKey("ident", "behandlingId") }
            validate { it.rejectKey("@løsning") }
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
        val ident = packet["ident"].asText()

        utsendingMediator.utsendingFinnesForBehandling(behandlingId).let {
            when (it) {
                true -> packet["@løsning"] = mapOf("MeldingOmVedtakProdusent" to "Dagpenger")
                false -> packet["@løsning"] = mapOf("MeldingOmVedtakProdusent" to "Arena")
            }
        }
        context.publish(packet.toJson())
    }
}
