package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

const val VEDTAKSTATUS_IVERKSATT = "IVERK"

class ArenaSinkVedtakOpprettetMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveRepository: OppgaveRepository,
    private val utsendingMediator: UtsendingMediator,
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
        val vedtakstatus = packet["vedtakstatus"].asText()

        withLoggingContext(
            "behandlingId" to "$behandlingId",
            "oppgaveId" to oppgave.oppgaveId.toString(),
            "sakId" to sakId,
        ) {
            logg.info("Mottok arenasink_vedtak_opprettet hendelse for behandlingId $behandlingId")
            sikkerlogg.info("Mottok arenasink_vedtak_opprettet hendelse ${packet.toJson()}")
            if (vedtakstatus == VEDTAKSTATUS_IVERKSATT) {
                if (utsendingMediator.utsendingFinnesForOppgave(oppgave.oppgaveId)) {
                    context.publish(lagStartUtsendingEvent(oppgave, sakId))
                    logg.info {
                        "Publiserte start_utsending hendelse for behandlingId $behandlingId og oppgaveId ${oppgave.oppgaveId}"
                    }
                } else {
                    logg.info(
                        "Fant ingen utsending for behandlingId $behandlingId og oppgaveId: ${oppgave.oppgaveId}." +
                            " Sender ikke start_utsending event",
                    )
                }
            } else {
                logg.info("Vedtakstatus fra Arena er $vedtakstatus. Sender ikke start_utsending event for behandlingId $behandlingId")
            }
        }
    }

    private fun lagStartUtsendingEvent(
        oppgave: Oppgave,
        sakId: String?,
    ) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "start_utsending",
            "behandlingId" to oppgave.behandling.behandlingId,
            "oppgaveId" to oppgave.oppgaveId,
            "ident" to oppgave.behandling.person.ident,
            "sak" to
                mapOf(
                    "id" to sakId,
                    "kontekst" to "Arena",
                ),
        ),
    ).toJson()
}
