package no.nav.dagpenger.saksbehandling.tilbakekreving

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
        eksternBehandlingId =
            packet["eksternBehandlingId"]
                .takeIf { !it.isNull && !it.isMissingNode }
                ?.asUUID()
                ?: throw IllegalArgumentException("eksternBehandlingId mangler for tilbakekreving dagpenger"),
        hendelseOpprettet = LocalDateTime.parse(packet["hendelseOpprettet"].asText()),
        tilbakekreving =
            Tilbakekreving(
                behandlingId = UUID.fromString(tilbakekrevingNode["behandlingId"].asText()),
                sakOpprettet = LocalDateTime.parse(tilbakekrevingNode["sakOpprettet"].asText()),
                varselSendt =
                    tilbakekrevingNode["varselSendt"]
                        .takeIf { !it.isNull && !it.isMissingNode }
                        ?.let { LocalDate.parse(it.asText()) },
                behandlingsstatus =
                    BehandlingStatus.valueOf(tilbakekrevingNode["behandlingsstatus"].asText()),
                forrigeBehandlingsstatus =
                    tilbakekrevingNode["forrigeBehandlingsstatus"]
                        .takeIf { !it.isNull && !it.isMissingNode }
                        ?.let { BehandlingStatus.valueOf(it.asText()) },
                totaltFeilutbetaltBeløp = BigDecimal(tilbakekrevingNode["totaltFeilutbetaltBeløp"].asText()),
                saksbehandlingURL = tilbakekrevingNode["saksbehandlingURL"].asText(),
                fullstendigPeriode =
                    Periode(
                        fom = LocalDate.parse(tilbakekrevingNode["fullstendigPeriode"]["fom"].asText()),
                        tom = LocalDate.parse(tilbakekrevingNode["fullstendigPeriode"]["tom"].asText()),
                    ),
            ),
    )
}
