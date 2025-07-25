package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import java.util.UUID

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
            precondition {
                it.requireValue("@event_name", "arenasink_vedtak_opprettet")
            }
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

    // Se https://nav-it.slack.com/archives/C063581H0PR/p1753450480334419
    private val skipBehandlinger =
        setOf(
            "01984174-1dd2-7ebc-86d1-c85702ffaaf4",
        ).map { UUID.fromString(it) }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["kilde"]["id"].asUUID()
        if (behandlingId in skipBehandlinger) {
            logg.info { "Skipper behandling med id $behandlingId" }
            return
        }
        val oppgave = oppgaveRepository.hentOppgaveFor(behandlingId)
        val ident = oppgave.personIdent()
        val sakId = packet["sakId"].asText()
        val vedtakstatus = packet["vedtakstatus"].asText()

        withLoggingContext(
            "behandlingId" to "$behandlingId",
            "oppgaveId" to oppgave.oppgaveId.toString(),
            "sakId" to sakId,
        ) {
            logg.info("Mottok arenasink_vedtak_opprettet hendelse")
            sikkerlogg.info("Mottok arenasink_vedtak_opprettet hendelse ${packet.toJson()}")
            if (vedtakstatus == VEDTAKSTATUS_IVERKSATT) {
                if (utsendingMediator.utsendingFinnesForOppgave(oppgave.oppgaveId)) {
                    context.publish(key = ident, message = lagStartUtsendingEvent(oppgave, sakId))
                    logg.info("Publiserte start_utsending hendelse")
                } else {
                    logg.info("Fant ingen utsending for behandlingen. Sender ikke start_utsending event")
                }
            } else {
                logg.info("Vedtakstatus fra Arena er $vedtakstatus. Sender ikke start_utsending event for behandlingen")
            }
        }
    }

    private fun lagStartUtsendingEvent(
        oppgave: Oppgave,
        sakId: String?,
    ) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "start_utsending",
            "behandlingId" to oppgave.behandlingId,
            "oppgaveId" to oppgave.oppgaveId,
            "ident" to oppgave.personIdent(),
            "sak" to
                mapOf(
                    "id" to sakId,
                    "kontekst" to "Arena",
                ),
        ),
    ).toJson()
}
