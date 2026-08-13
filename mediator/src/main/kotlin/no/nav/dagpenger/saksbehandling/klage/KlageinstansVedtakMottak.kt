package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.hendelser.KlageinstansVedtakHendelse
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class KlageinstansVedtakMottak(
    rapidsConnection: RapidsConnection,
    private val klageMediator: KlageMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "KlageAnkeVedtak")
                it.requireValue("kilde", "DAGPENGER")
                it.requireKey("kildeReferanse", "kabalReferanse", "type", "detaljer")
                it.interestedIn("eventId")
                it.forbid("@final")
            }
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
        logger.info { "KlageAnkeVedtak mottat" }
        val klageId = packet["kildeReferanse"].asUUID()
        val klageinstansEventId = packet["eventId"].asUUID()
        val klageinstansVedtakId = packet["kabalReferanse"].asUUID()
        val klageinstansVedtakType = packet["type"].stringValue()

        withLoggingContext(
            "klageId" to klageId.toString(),
            "klageinstansVedtakId" to klageinstansVedtakId.toString(),
            "klageinstansEventId" to klageinstansEventId.toString(),
        ) {
            sikkerlogger.info { "Mottok klageinstans vedtak med pakke: ${packet.toJson()}" }

            val vedtakType = KlageinstansVedtakHendelse.KlageinstansVedtakType.fromString(klageinstansVedtakType)
            val detaljNode = DetaljNode(packet["detaljer"]["klagebehandlingAvsluttet"])

            klageMediator.mottaKlageinstansVedtak(
                KlageinstansVedtakHendelse(
                    type = vedtakType,
                    klageId = klageId,
                    klageinstansVedtakId = klageinstansVedtakId,
                    avsluttet = detaljNode.avsluttet,
                    utfall = detaljNode.utfall,
                    journalpostIder = detaljNode.journalpostIder,
                ),
            )
        }
    }

    private class DetaljNode(
        jsonNode: JsonNode,
    ) {
        val avsluttet: LocalDateTime
        val journalpostIder: List<String>
        val utfall: String

        init {
            avsluttet = jsonNode["avsluttet"].asLocalDateTime()
            journalpostIder =
                jsonNode["journalpostReferanser"]
                    .values()
                    .map { it.stringValue() }
            utfall = jsonNode["utfall"].stringValue()
        }
    }
}
