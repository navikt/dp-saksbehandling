package no.nav.dagpenger.saksbehandling.tilbakekreving

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.mottak.textOrNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

internal class TilbakekrevingMottak(
    rapidsConnection: RapidsConnection,
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
            metadata.key ?: run {
                logger.error { "Mottok tilbakekreving-hendelse uten key (personident). Ignorerer meldingen." }
                return
            }

        val tilbakekrevingNode = packet["tilbakekreving"]
        val dto =
            TilbakekrevingDTO(
                behandlingId = UUID.fromString(tilbakekrevingNode["behandlingId"].asText()),
                sakOpprettet = LocalDateTime.parse(tilbakekrevingNode["sakOpprettet"].asText()),
                varselSendt =
                    tilbakekrevingNode["varselSendt"]
                        .takeIf { !it.isNull && !it.isMissingNode }
                        ?.let { LocalDate.parse(it.asText()) },
                behandlingsstatus =
                    TilbakekrevingDTO.BehandlingStatus.valueOf(
                        tilbakekrevingNode["behandlingsstatus"].asText(),
                    ),
                forrigeBehandlingsstatus =
                    tilbakekrevingNode["forrigeBehandlingsstatus"]
                        .takeIf { !it.isNull && !it.isMissingNode }
                        ?.let { TilbakekrevingDTO.BehandlingStatus.valueOf(it.asText()) },
                totaltFeilutbetaltBeløp = BigDecimal(tilbakekrevingNode["totaltFeilutbetaltBeløp"].asText()),
                saksbehandlingURL = tilbakekrevingNode["saksbehandlingURL"].asText(),
                fullstendigPeriode =
                    TilbakekrevingDTO.Periode(
                        fom = LocalDate.parse(tilbakekrevingNode["fullstendigPeriode"]["fom"].asText()),
                        tom = LocalDate.parse(tilbakekrevingNode["fullstendigPeriode"]["tom"].asText()),
                    ),
            )

        val hendelse =
            TilbakekrevingHendelse(
                ident = ident,
                eksternFagsakId = packet["eksternFagsakId"].asText(),
                eksternBehandlingId = packet["eksternBehandlingId"].textOrNull(),
                hendelseOpprettet = LocalDateTime.parse(packet["hendelseOpprettet"].asText()),
                tilbakekrevingBehandlingId = dto.behandlingId,
                saksbehandlingURL = dto.saksbehandlingURL,
            )

        withLoggingContext("tilbakekrevingBehandlingId" to "${hendelse.tilbakekrevingBehandlingId}") {
            when (dto.behandlingsstatus) {
                TilbakekrevingDTO.BehandlingStatus.OPPRETTET -> {
                    logger.info { "Mottok tilbakekreving opprettet hendelse" }
                    sikkerlogger.info { "Mottok tilbakekreving opprettet hendelse for ident $ident" }
                }

                TilbakekrevingDTO.BehandlingStatus.TIL_BEHANDLING ->
                    logger.info { "Mottok tilbakekreving til behandling hendelse" }

                TilbakekrevingDTO.BehandlingStatus.TIL_GODKJENNING ->
                    logger.info { "Mottok tilbakekreving til godkjenning hendelse" }

                TilbakekrevingDTO.BehandlingStatus.AVSLUTTET ->
                    logger.info { "Mottok tilbakekreving avsluttet hendelse" }
            }
        }
    }
}

private data class TilbakekrevingDTO(
    val behandlingId: UUID,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDate?,
    val behandlingsstatus: BehandlingStatus,
    val forrigeBehandlingsstatus: BehandlingStatus?,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val saksbehandlingURL: String,
    val fullstendigPeriode: Periode,
) {
    data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
    )

    enum class BehandlingStatus {
        OPPRETTET,
        TIL_BEHANDLING,
        TIL_GODKJENNING,
        AVSLUTTET,
    }
}
