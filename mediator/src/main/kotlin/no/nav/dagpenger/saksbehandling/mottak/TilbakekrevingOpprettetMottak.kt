package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.math.BigDecimal
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

internal class TilbakekrevingOpprettetMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
    private val sakMediator: SakMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("hendelsestype", "behandling_endret")
                it.requireValue("tilbakekreving.behandlingsstatus", "OPPRETTET")
            }
            validate {
                it.requireKey(
                    "eksternFagsakId",
                    "hendelseOpprettet",
                    "tilbakekreving.behandlingId",
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

        withLoggingContext("tilbakekrevingBehandlingId" to "$tilbakekrevingBehandlingId") {
            logger.info { "Mottok tilbakekreving opprettet hendelse" }
            sikkerlogger.info { "Mottok tilbakekreving opprettet hendelse for ident $ident" }

            val hendelse =
                TilbakekrevingOpprettetHendelse(
                    ident = ident,
                    eksternFagsakId = packet["eksternFagsakId"].asText(),
                    eksternBehandlingId = packet["eksternBehandlingId"].textOrNull(),
                    tilbakekrevingBehandlingId = tilbakekrevingBehandlingId,
                    totaltFeilutbetaltBeløp = BigDecimal(packet["tilbakekreving.totaltFeilutbetaltBeløp"].asText()),
                    opprettet = LocalDateTime.parse(packet["hendelseOpprettet"].asText()),
                )

            oppgaveMediator.opprettOppgaveForTilbakekreving(hendelse)
        }
    }
}
