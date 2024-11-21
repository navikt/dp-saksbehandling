package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class VedtakFattetMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
    private val utsendingMediator: UtsendingMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "vedtak_fattet")
                it.forbid("meldingOmVedtakProdusent")
            }
            validate { it.requireKey("ident", "søknadId", "behandlingId", "fagsakId") }
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
        val søknadId = packet["søknadId"].asUUID()
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val sak = packet.sak()

        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info {
                "Mottok vedtak_fattet hendelse for søknadId $søknadId og behandlingId $behandlingId. "
            }
            oppgaveMediator.ferdigstillOppgave(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = ident,
                    sak = sak,
                ),
            )
            packet["@event_name"] = "vedtak_fattet_til_arena"
            packet["meldingOmVedtakProdusent"] = vedtakProdusent(behandlingId)
            context.publish(packet.toJson())
            logger.info {
                "Publiserte vedtak_fattet_til_arena hendelse for søknadId $søknadId og behandlingId $behandlingId"
            }
        }
    }

    private fun vedtakProdusent(behandlingId: UUID): String =
        when (utsendingMediator.utsendingFinnesForBehandling(behandlingId)) {
            true -> "Dagpenger"
            false -> "Arena"
        }
}

private fun JsonMessage.sak(): Sak = Sak(id = this["fagsakId"].asText())
