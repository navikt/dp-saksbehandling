package no.nav.dagpenger.saksbehandling.tilbakekreving

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import no.nav.dagpenger.saksbehandling.serder.asUUID
import tools.jackson.databind.JsonNode
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerLogger = KotlinLogging.logger("tjenestekall")

internal class TilbakekrevingMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("hendelsestype", "behandling_endret")
                it.requireValue("versjon", 1)
            }
            validate {
                it.requireKey(
                    "eksternBehandlingId",
                    "hendelseOpprettet",
                    "tilbakekreving",
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
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        sikkerLogger.info { "Mottok tilbakekreving hendelse: ${packet.toJson()}" }
        val behandlingIdAsString = packet["eksternBehandlingId"].stringValue()
        val behandlingId = behandlingIdAsString.asOptionalUUID()
        if (behandlingId == null) {
            // Team Tilbake sender også hendelser generert av burde-forstaatt appen sin. Disse skal vi ignorere i dev.
            if (Configuration.isDev) {
                logger.info {
                    "Mottok tilbakekrevingHendelse med eksternBehandlingId i feil format: $behandlingIdAsString"
                }
                return
            } else {
                throw IllegalArgumentException(
                    "Mottok tilbakekrevingHendelse med eksternBehandlingId i feil format: $behandlingIdAsString",
                )
            }
        }

        val hendelse = tilbakekrevingHendelseFraPacket(packet)
        withLoggingContext(
            "tilbakekrevingBehandlingId" to "${hendelse.tilbakekreving.behandlingId}",
            "behandlingId" to "${hendelse.eksternBehandlingId}",
        ) {
            logger.info { "Mottok tilbakekreving hendelse med status ${hendelse.tilbakekreving.behandlingsstatus}" }
            oppgaveMediator.håndter(hendelse)
        }
    }
}

private fun String.asOptionalUUID(): UUID? =
    runCatching {
        UUID.fromString(this)
    }.onFailure {
        logger.warn { "Kunne ikke parse til UUID: $this" }
    }.getOrNull()

private fun tilbakekrevingHendelseFraPacket(packet: JsonMessage): TilbakekrevingHendelse {
    val tilbakekrevingNode: JsonNode = packet["tilbakekreving"]
    return TilbakekrevingHendelse(
        eksternBehandlingId = packet["eksternBehandlingId"].asUUID(),
        hendelseOpprettet = OffsetDateTime.parse(packet["hendelseOpprettet"].stringValue()).toLocalDateTime(),
        tilbakekreving =
            TilbakekrevingHendelse.Tilbakekreving(
                behandlingId = tilbakekrevingNode["behandlingId"].asUUID(),
                opprettet = OffsetDateTime.parse(tilbakekrevingNode["sakOpprettet"].stringValue()).toLocalDateTime(),
                avventBehandlingTilDato = tilbakekrevingNode.get("venter")?.get("gjenopptas")?.asOptionalLocalDate(),
                varselSendt = tilbakekrevingNode["varselSendt"]?.asOptionalLocalDate(),
                behandlingsstatus =
                    TilbakekrevingHendelse.BehandlingStatus.valueOf(tilbakekrevingNode["behandlingsstatus"].stringValue()),
                forrigeBehandlingsstatus =
                    tilbakekrevingNode["forrigeBehandlingsstatus"]
                        ?.takeIf(JsonNode::isString)
                        ?.let { TilbakekrevingHendelse.BehandlingStatus.valueOf(it.stringValue()) },
                totaltFeilutbetaltBeløp = BigDecimal(tilbakekrevingNode["totaltFeilutbetaltBeløp"].stringValue()),
                saksbehandlingURL = tilbakekrevingNode["saksbehandlingURL"].stringValue(),
                fullstendigPeriode =
                    TilbakekrevingHendelse.Periode(
                        fom = tilbakekrevingNode["fullstendigPeriode"]["fom"].asLocalDate(),
                        tom = tilbakekrevingNode["fullstendigPeriode"]["tom"].asLocalDate(),
                    ),
            ),
    )
}
