package no.nav.dagpenger.saksbehandling.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class VedtakFattetMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
    private val utsendingMediator: UtsendingMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "vedtak_fattet") }
            validate { it.requireKey("ident", "søknadId", "behandlingId", "fagsakId", "automatisk") }
            validate { it.rejectKey("meldingOmVedtakProdusent") }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val søknadId = packet["søknadId"].asUUID()
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val sak = packet.sak()
        val automatiskBehandlet = packet["automatisk"].asBoolean()

        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info {
                "Mottok vedtak fattet hendelse for søknadId $søknadId og behandlingId $behandlingId. " +
                    "Automatisk behandlet: $automatiskBehandlet"
            }
            oppgaveMediator.ferdigstillOppgave(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = ident,
                    sak = sak,
                ),
            )
            // Brukes for helautomatisk vedtak der Arena skal sende brev
            packet["meldingOmVedtakProdusent"] = vedtakProdusent(behandlingId)
            context.publish(packet.toJson())
        }
    }

    private fun vedtakProdusent(behandlingId: UUID): String {
        return oppgaveMediator.hentOppgaveFor(behandlingId).let { oppgave ->
            when (utsendingMediator.finnUtsendingFor(oppgave.oppgaveId)) {
                null -> "Arena"
                else -> "Dagpenger"
            }
        }
    }
}

private fun JsonMessage.sak(): Sak = Sak(id = this["fagsakId"].asText())
