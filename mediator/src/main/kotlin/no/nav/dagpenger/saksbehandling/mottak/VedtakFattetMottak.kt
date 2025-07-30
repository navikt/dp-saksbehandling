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
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse

private val logger = KotlinLogging.logger {}

internal class VedtakFattetMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "vedtak_fattet")
                it.requireAny(key = "behandletHendelse.type", values = listOf("Søknad", "Meldekort", "Manuell"))
                it.forbid("meldingOmVedtakProdusent")
            }
            validate {
                it.requireKey("ident", "behandlingId", "fagsakId", "automatisk")
                it.interestedIn("behandletHendelse")
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
        val id = packet.id()
        val behandlingId = packet["behandlingId"].asUUID()
        val behandletHendelseType = packet["behandletHendelse"]["type"].asText()

        withLoggingContext("id" to "$id", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok vedtak_fattet hendelse" }
            oppgaveMediator.ferdigstillOppgave(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    behandletHendelseId = id.toString(),
                    behandletHendelseType = behandletHendelseType,
                    ident = packet["ident"].asText(),
                    sak = packet.sak(),
                    automatiskBehandlet = packet["automatisk"].asBoolean(),
                ),
            )
        }
    }
}

private fun JsonMessage.sak(): UtsendingSak = UtsendingSak(id = this["fagsakId"].asText())

private fun JsonMessage.id(): String = this["behandletHendelse"]["id"].asText()
