package no.nav.dagpenger.saksbehandling.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class ArenaSinkVedtakOpprettetMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveRepository: OppgaveRepository,
    private val sendStartUtsendingEvents: Boolean,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            validate { it.demandValue("@event_name", "arenasink_vedtak_opprettet") }
            validate {
                it.requireKey(
                    "søknadId",
                    "sakId",
                    "vedtakId",
                    "vedtakstatus",
                    "rettighet",
                    "utfall",
                    "kilde",
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
    ) {
        val behandlingId = packet["kilde"]["id"].asUUID()
        val oppgave = oppgaveRepository.hentOppgaveFor(behandlingId)
        val sakId = packet["sakId"].asText()

        withLoggingContext(
            "behandlingId" to "$behandlingId",
            "oppgaveId" to oppgave.oppgaveId.toString(),
            "sakId" to sakId,
        ) {
            logg.info("Mottok arenasink_vedtak_opprettet hendelse for behandlingId $behandlingId")
            sikkerlogg.info("Mottok arenasink_vedtak_opprettet hendelse ${packet.toJson()}")
            if (sendStartUtsendingEvents) {
                context.publish(
                    JsonMessage.newMessage(
                        mapOf(
                            "@event_name" to "start_utsending",
                            "behandlingId" to oppgave.behandlingId,
                            "oppgaveId" to oppgave.oppgaveId,
                            "ident" to oppgave.ident,
                            "sak" to
                                mapOf(
                                    "id" to sakId,
                                    "kontekst" to "Arena",
                                ),
                        ),
                    ).toJson(),
                )
            }
        }
    }
}
