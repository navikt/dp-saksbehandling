package no.nav.dagpenger.saksbehandling.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private val logger = KotlinLogging.logger {}

internal class VedtakFattetMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "vedtak_fattet") }
            validate { it.requireKey("ident", "søknadId", "behandlingId", "sakId") }
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
        val sakId = packet.sakId()

        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok vedtak fattet hendelse for søknadId $søknadId og behandlingId $behandlingId" }
            val oppgave =
                oppgaveMediator.ferdigstillOppgave(
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        søknadId = søknadId,
                        ident = ident,
                        sakId = sakId,
                    ),
                )

            context.publish(
                JsonMessage.newMessage(
                    map =
                        mapOf(
                            "@event_name" to "start_utsending",
                            "oppgaveId" to oppgave.oppgaveId.toString(),
                            "behandlingId" to oppgave.behandlingId.toString(),
                            "ident" to oppgave.ident,
                            "sakId" to sakId.toMap(),
                        ),
                ).toJson(),
            )
        }
    }
}

private fun JsonMessage.sakId(): VedtakFattetHendelse.SakId {
    return VedtakFattetHendelse.SakId(
        id = this["sakId"]["id"].asText(),
        kontekst = this["sakId"]["kontekst"].asText(),
    )
}
