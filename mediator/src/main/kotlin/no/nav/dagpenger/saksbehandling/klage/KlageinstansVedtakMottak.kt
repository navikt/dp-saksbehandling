package no.nav.dagpenger.saksbehandling.klage

import com.fasterxml.jackson.databind.JsonNode
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
import java.time.LocalDateTime
import java.util.UUID

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
        val klageInstansEventId = packet["eventId"].asUUID()
        val klageId = packet["kildeReferanse"].asUUID()
        val klageinstansVedtakId = packet["kabalReferanse"].asUUID()
        val klageInstansVedtakType = packet["type"].asText()
        val type = KlageinstansVedtakHendelse.KlageVedtakType.fromString(klageInstansVedtakType)
        withLoggingContext(
            "klageId" to klageId.toString(),
            "klageinstansVedtakId" to klageinstansVedtakId.toString(),
            "klageInstansEventId" to klageInstansEventId.toString(),
            "klageInstansVedtakType" to klageInstansVedtakType,
        ) {
            sikkerlogger.info { "Mottok klageinstans vedtak med pakke: ${packet.toJson()}" }

            if (klageInstansEventId in
                setOf(
                    UUID.fromString("ddedbbad-94de-4aac-8ab8-f6d0c422230d"),
                )
            ) {
                logger.warn { "Skipper klageinstans vedtak med eventId $klageInstansEventId" }
                return
            }

            val detaljeNode =
                when (type) {
                    KlageinstansVedtakHendelse.KlageVedtakType.KLAGE -> {
                        DetaljeNode(packet["detaljer"]["klagebehandlingAvsluttet"])
                    }
                }
            klageMediator.mottaKlageinstansVedtak(
                KlageinstansVedtakHendelse(
                    type = type,
                    klageId = klageId,
                    klageinstansVedtakId = klageinstansVedtakId,
                    avsluttet = detaljeNode.avsluttet,
                    utfall = detaljeNode.utfall,
                    journalpostIder = detaljeNode.journalpostIder,
                ),
            )
        }
    }

    private class DetaljeNode(
        jsonNode: JsonNode,
    ) {
        val avsluttet: LocalDateTime
        val journalpostIder: List<String>
        val utfall: String

        init {
            avsluttet = jsonNode["avsluttet"].asLocalDateTime()
            journalpostIder =
                jsonNode["journalpostReferanser"].map {
                    it.asText()
                }
            utfall = jsonNode["utfall"].asText()
        }
    }
}
