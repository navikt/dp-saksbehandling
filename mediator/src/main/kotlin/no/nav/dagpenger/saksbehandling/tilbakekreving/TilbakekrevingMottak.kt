package no.nav.dagpenger.saksbehandling.tilbakekreving

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.mottak.asUUID
import no.nav.dagpenger.saksbehandling.mottak.textOrNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

internal class TilbakekrevingMottak(
    rapidsConnection: RapidsConnection,
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
                    "versjon",
                    "eksternFagsakId",
                    "hendelseOpprettet",
                    "tilbakekreving.behandlingId",
                    "tilbakekreving.sakOpprettet",
                    "tilbakekreving.behandlingsstatus",
                    "tilbakekreving.totaltFeilutbetaltBeløp",
                    "tilbakekreving.saksbehandlingURL",
                    "tilbakekreving.fullstendigPeriode.fom",
                    "tilbakekreving.fullstendigPeriode.tom",
                )
                it.interestedIn(
                    "eksternBehandlingId",
                    "tilbakekreving.varselSendt",
                    "tilbakekreving.forrigeBehandlingsstatus",
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
        val tilbakekrevingBehandlingId = packet["tilbakekreving.behandlingId"].asUUID()
        val ident =
            metadata.key ?: run {
                logger.error { "Mottok tilbakekreving-hendelse uten key (personident). Ignorerer meldingen." }
                return
            }

        val hendelse =
            TilbakekrevingHendelse(
                ident = ident,
                versjon = packet["versjon"].asInt(),
                eksternFagsakId = packet["eksternFagsakId"].asText(),
                eksternBehandlingId = packet["eksternBehandlingId"].textOrNull(),
                hendelseOpprettet = LocalDateTime.parse(packet["hendelseOpprettet"].asText()),
                tilbakekrevingBehandlingId = tilbakekrevingBehandlingId,
                sakOpprettet = LocalDateTime.parse(packet["tilbakekreving.sakOpprettet"].asText()),
                varselSendt = packet["tilbakekreving.varselSendt"].textOrNull()?.let { LocalDate.parse(it) },
                status =
                    TilbakekrevingHendelse.BehandlingStatus.valueOf(
                        packet["tilbakekreving.behandlingsstatus"].asText(),
                    ),
                forrigeStatus =
                    packet["tilbakekreving.forrigeBehandlingsstatus"].textOrNull()?.let {
                        TilbakekrevingHendelse.BehandlingStatus.valueOf(it)
                    },
                totaltFeilutbetaltBeløp = BigDecimal(packet["tilbakekreving.totaltFeilutbetaltBeløp"].asText()),
                saksbehandlingURL = packet["tilbakekreving.saksbehandlingURL"].asText(),
                fullstendigPeriode =
                    TilbakekrevingHendelse.Periode(
                        fom = LocalDate.parse(packet["tilbakekreving.fullstendigPeriode.fom"].asText()),
                        tom = LocalDate.parse(packet["tilbakekreving.fullstendigPeriode.tom"].asText()),
                    ),
            )

        withLoggingContext("tilbakekrevingBehandlingId" to "$tilbakekrevingBehandlingId") {
            when (hendelse.status) {
                TilbakekrevingHendelse.BehandlingStatus.OPPRETTET -> {
                    logger.info { "Mottok tilbakekreving opprettet hendelse" }
                    sikkerlogger.info { "Mottok tilbakekreving opprettet hendelse for ident $ident" }
                }

                TilbakekrevingHendelse.BehandlingStatus.TIL_BEHANDLING -> {
                    logger.info { "Mottok tilbakekreving til behandling hendelse" }
                }

                TilbakekrevingHendelse.BehandlingStatus.TIL_GODKJENNING -> {
                    logger.info { "Mottok tilbakekreving til godkjenning hendelse" }
                }

                TilbakekrevingHendelse.BehandlingStatus.AVSLUTTET -> {
                    logger.info { "Mottok tilbakekreving avsluttet hendelse" }
                }
            }
        }
    }
}
