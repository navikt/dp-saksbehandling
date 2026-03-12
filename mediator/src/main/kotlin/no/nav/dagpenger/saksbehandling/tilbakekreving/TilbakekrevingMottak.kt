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
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import no.nav.dagpenger.saksbehandling.mottak.textOrNull
import java.math.BigDecimal
import java.time.LocalDateTime

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
                it.requireAny(
                    "tilbakekreving.behandlingsstatus",
                    listOf("OPPRETTET", "TIL_BEHANDLING", "TIL_GODKJENNING", "AVSLUTTET"),
                )
            }
            validate {
                it.requireKey(
                    "eksternFagsakId",
                    "hendelseOpprettet",
                    "tilbakekreving.behandlingId",
                    "tilbakekreving.behandlingsstatus",
                    "tilbakekreving.totaltFeilutbetaltBeløp",
                )
                it.interestedIn("eksternBehandlingId")
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
        val tilbakekrevingBehandlingId = packet["tilbakekreving.behandlingId"].asUUID()
        val ident =
            metadata.key ?: run {
                logger.error { "Mottok tilbakekreving-hendelse uten key (personident). Ignorerer meldingen." }
                return
            }
        val status = packet["tilbakekreving.behandlingsstatus"].asText()

        withLoggingContext("tilbakekrevingBehandlingId" to "$tilbakekrevingBehandlingId") {
            val hendelse = lagHendelse(packet, ident, status)

            when (hendelse) {
                is TilbakekrevingHendelse.Opprettet -> {
                    logger.info { "Mottok tilbakekreving opprettet hendelse" }
                    sikkerlogger.info { "Mottok tilbakekreving opprettet hendelse for ident $ident" }
                    oppgaveMediator.opprettOppgaveForTilbakekreving(hendelse)
                }

                is TilbakekrevingHendelse.TilBehandling -> {
                    logger.info { "Mottok tilbakekreving til behandling hendelse" }
                }

                is TilbakekrevingHendelse.TilGodkjenning -> {
                    logger.info { "Mottok tilbakekreving til godkjenning hendelse" }
                }

                is TilbakekrevingHendelse.Avsluttet -> {
                    logger.info { "Mottok tilbakekreving avsluttet hendelse" }
                }
            }
        }
    }

    private fun lagHendelse(
        packet: JsonMessage,
        ident: String,
        status: String,
    ): TilbakekrevingHendelse {
        val felter =
            FellesFelter(
                ident = ident,
                eksternFagsakId = packet["eksternFagsakId"].asText(),
                eksternBehandlingId = packet["eksternBehandlingId"].textOrNull(),
                tilbakekrevingBehandlingId = packet["tilbakekreving.behandlingId"].asUUID(),
                totaltFeilutbetaltBeløp = BigDecimal(packet["tilbakekreving.totaltFeilutbetaltBeløp"].asText()),
                opprettet = LocalDateTime.parse(packet["hendelseOpprettet"].asText()),
            )

        return when (status) {
            "OPPRETTET" ->
                TilbakekrevingHendelse.Opprettet(
                    ident = felter.ident,
                    eksternFagsakId = felter.eksternFagsakId,
                    eksternBehandlingId = felter.eksternBehandlingId,
                    tilbakekrevingBehandlingId = felter.tilbakekrevingBehandlingId,
                    totaltFeilutbetaltBeløp = felter.totaltFeilutbetaltBeløp,
                    opprettet = felter.opprettet,
                )
            "TIL_BEHANDLING" ->
                TilbakekrevingHendelse.TilBehandling(
                    ident = felter.ident,
                    eksternFagsakId = felter.eksternFagsakId,
                    eksternBehandlingId = felter.eksternBehandlingId,
                    tilbakekrevingBehandlingId = felter.tilbakekrevingBehandlingId,
                    totaltFeilutbetaltBeløp = felter.totaltFeilutbetaltBeløp,
                    opprettet = felter.opprettet,
                )
            "TIL_GODKJENNING" ->
                TilbakekrevingHendelse.TilGodkjenning(
                    ident = felter.ident,
                    eksternFagsakId = felter.eksternFagsakId,
                    eksternBehandlingId = felter.eksternBehandlingId,
                    tilbakekrevingBehandlingId = felter.tilbakekrevingBehandlingId,
                    totaltFeilutbetaltBeløp = felter.totaltFeilutbetaltBeløp,
                    opprettet = felter.opprettet,
                )
            "AVSLUTTET" ->
                TilbakekrevingHendelse.Avsluttet(
                    ident = felter.ident,
                    eksternFagsakId = felter.eksternFagsakId,
                    eksternBehandlingId = felter.eksternBehandlingId,
                    tilbakekrevingBehandlingId = felter.tilbakekrevingBehandlingId,
                    totaltFeilutbetaltBeløp = felter.totaltFeilutbetaltBeløp,
                    opprettet = felter.opprettet,
                )
            else -> error("Ukjent tilbakekreving behandlingsstatus: $status")
        }
    }

    private data class FellesFelter(
        val ident: String,
        val eksternFagsakId: String,
        val eksternBehandlingId: String?,
        val tilbakekrevingBehandlingId: java.util.UUID,
        val totaltFeilutbetaltBeløp: BigDecimal,
        val opprettet: LocalDateTime,
    )
}
