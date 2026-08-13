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
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.KlageinstansVedtakHendelse
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class KlageinstansVedtakMottakForOppgave(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
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
        logger.info { "KlageAnkeVedtak mottat for oppdatering av oppgave" }

        val klageId = packet["kildeReferanse"].asUUID()
        val klageinstansEventId = packet["eventId"].asUUID()
        val klageinstansVedtakId = packet["kabalReferanse"].asUUID()
        val klageinstansVedtakType = packet["type"].stringValue()
        val vedtakType = KlageinstansVedtakHendelse.KlageinstansVedtakType.fromString(klageinstansVedtakType)
        withLoggingContext(
            "klageId" to klageId.toString(),
            "klageinstansVedtakId" to klageinstansVedtakId.toString(),
            "klageinstansEventId" to klageinstansEventId.toString(),
        ) {
            sikkerlogger.info { "KlageinstansVedtakMottakForOppgave mottatt: ${packet.toJson()}" }

            when (vedtakType) {
                KlageinstansVedtakHendelse.KlageinstansVedtakType.KLAGE -> {
                    DetaljNode(packet["detaljer"]["klagebehandlingAvsluttet"])
                }
            }?.let { klagebehandlingAvsluttetNode ->
                oppgaveMediator.håndterUtfallFraKlageinstans(
                    KlageinstansVedtakHendelse(
                        type = vedtakType,
                        klageId = klageId,
                        klageinstansVedtakId = klageinstansVedtakId,
                        avsluttet = klagebehandlingAvsluttetNode.avsluttet,
                        utfall = klagebehandlingAvsluttetNode.utfall,
                        journalpostIder = klagebehandlingAvsluttetNode.journalpostIder,
                    ),
                )
            }
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
