package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
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

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        super.onError(problems, context, metadata)
    }

    override fun onPreconditionError(
        error: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        super.onPreconditionError(error, context, metadata)
    }

    override fun onSevere(
        error: MessageProblems.MessageException,
        context: MessageContext,
    ) {
        super.onSevere(error, context)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["kilde"]["id"].asUUID()
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
                utsendingMediator.startUtsendingForVedtakFattet(
                    VedtakFattetHendelse(
                        behandlingId = behandlingId,
                        ident = ident,
                        sak =
                            UtsendingSak(
                                id = sakId,
                                kontekst = "Arena",
                            ),
                    ),
                )
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
