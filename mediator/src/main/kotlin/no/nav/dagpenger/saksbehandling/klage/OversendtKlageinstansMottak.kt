package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
import no.nav.dagpenger.saksbehandling.mottak.asUUID

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class OversendtKlageinstansMottak(
    rapidsConnection: RapidsConnection,
    private val klageMediator: KlageMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireAllOrAny(
                    key = "@behov",
                    values = listOf("OversendelseKlageinstans"),
                )
                it.forbid("@final")
            }
            validate { it.requireKey("@løsning") }
            validate { it.requireKey("behandlingId") }
            validate { it.requireKey("fagsakId") }
            validate { it.requireKey("ident") }
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
        val behandlingId = packet["behandlingId"].asUUID()
        logger.info { "Mottok løsning på behov for oversending til klageinstans" }
        withLoggingContext("behandlingId" to behandlingId.toString()) {
            logger.info { "Mottok løsning på behov for oversending til klageinstans" }

            val oversendtKlageinstans: Boolean = packet["@løsning"]["OversendelseKlageinstans"].asText() == "OK"
            if (oversendtKlageinstans) {
                logger.info { "Oversendelse til klageinstans er OK" }
                klageMediator.oversendtTilKlageinstans(OversendtKlageinstansHendelse(behandlingId = behandlingId))
            } else {
                logger.error { "Oversendelse til klageinstans feilet?" }
                sikkerlogger.error { "Oversendelse til klageinstans feilet for packet: $packet" }
            }
        }
    }
}
