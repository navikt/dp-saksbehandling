package no.nav.dagpenger.saksbehandling.tilbakekreving

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse.BehandlingStatus
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse.Periode
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse.Tilbakekreving
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

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
                    "eksternFagsakId",
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
        val ident =
            metadata.key ?: throw IllegalArgumentException("Kan ikke hente ut tilbakekreving-endret metadata").also {
                logger.error { "Kan ikke hente ut tilbakekreving-endret metadata" }
            }

        val hendelse = tilbakekrevingHendelseFraPacket(packet, ident)

        withLoggingContext(
            "tilbakekrevingBehandlingId" to "${hendelse.tilbakekreving.behandlingId}",
            "behandlingId" to "${hendelse.eksternBehandlingId}",
        ) {
            logger.info { "Mottok tilbakekreving hendelse med status ${hendelse.tilbakekreving.behandlingsstatus}" }
            oppgaveMediator.håndter(hendelse)
        }
    }
}

private fun tilbakekrevingHendelseFraPacket(
    packet: JsonMessage,
    ident: String,
): TilbakekrevingHendelse {
    val tilbakekrevingNode = packet["tilbakekreving"]
    return TilbakekrevingHendelse(
        ident = ident,
        eksternFagsakId = packet["eksternFagsakId"].asText(),
        eksternBehandlingId = packet["eksternBehandlingId"].asUUID(),
        hendelseOpprettet = packet["hendelseOpprettet"].asLocalDateTime(),
        tilbakekreving =
            Tilbakekreving(
                behandlingId = tilbakekrevingNode["behandlingId"].asUUID(),
                sakOpprettet = tilbakekrevingNode["sakOpprettet"].asLocalDateTime(),
                varselSendt = tilbakekrevingNode["varselSendt"]?.asOptionalLocalDate(),
                behandlingsstatus =
                    BehandlingStatus.valueOf(tilbakekrevingNode["behandlingsstatus"].asText()),
                forrigeBehandlingsstatus =
                    tilbakekrevingNode["forrigeBehandlingsstatus"]
                        ?.takeIf(JsonNode::isTextual)
                        ?.let { BehandlingStatus.valueOf(it.asText()) },
                totaltFeilutbetaltBeløp = BigDecimal(tilbakekrevingNode["totaltFeilutbetaltBeløp"].asText()),
                saksbehandlingURL = tilbakekrevingNode["saksbehandlingURL"].asText(),
                fullstendigPeriode =
                    Periode(
                        fom = tilbakekrevingNode["fullstendigPeriode"]["fom"].asLocalDate(),
                        tom = tilbakekrevingNode["fullstendigPeriode"]["tom"].asLocalDate(),
                    ),
            ),
    )
}
