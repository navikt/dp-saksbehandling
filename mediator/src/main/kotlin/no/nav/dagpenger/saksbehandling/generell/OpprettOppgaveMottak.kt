package no.nav.dagpenger.saksbehandling.generell

import PersonMediator
import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.GenerellOppgaveData
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.generell.GenerellOppgaveDataRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

internal class OpprettOppgaveMottak(
    rapidsConnection: RapidsConnection,
    private val personMediator: PersonMediator,
    private val oppgaveRepository: OppgaveRepository,
    private val sakRepository: SakRepository,
    private val generellOppgaveDataRepository: GenerellOppgaveDataRepository,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "opprett_oppgave")
            }
            validate {
                it.requireKey("ident", "oppgaveType", "tittel")
            }
            validate {
                it.interestedIn("beskrivelse", "strukturertData")
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
        val ident = packet["ident"].asText()
        val oppgaveType = packet["oppgaveType"].asText()
        val tittel = packet["tittel"].asText()
        val beskrivelse = packet["beskrivelse"].takeIf { !it.isMissingOrNull() }?.asText()
        val strukturertData = packet["strukturertData"].takeIf { !it.isMissingOrNull() }

        withLoggingContext("oppgaveType" to oppgaveType) {
            logger.info { "Mottok opprett_oppgave hendelse med type $oppgaveType" }

            val hendelse =
                OpprettGenerellOppgaveHendelse(
                    ident = ident,
                    oppgaveType = oppgaveType,
                    tittel = tittel,
                    beskrivelse = beskrivelse,
                    strukturertData = strukturertData,
                )

            opprettGenerellOppgave(hendelse)
        }
    }

    private fun opprettGenerellOppgave(hendelse: OpprettGenerellOppgaveHendelse) {
        val person = personMediator.finnEllerOpprettPerson(hendelse.ident)
        val nå = LocalDateTime.now()

        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                opprettet = nå,
                hendelse = hendelse,
                utløstAv = UtløstAvType.GENERELL,
            )

        sakRepository.lagreBehandling(
            personId = person.id,
            sakId = null,
            behandling = behandling,
        )

        val oppgave =
            Oppgave(
                emneknagger = setOf(hendelse.oppgaveType),
                opprettet = nå,
                behandling = behandling,
                person = person,
                meldingOmVedtak =
                    Oppgave.MeldingOmVedtak(
                        kilde = Oppgave.MeldingOmVedtakKilde.INGEN,
                        kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                    ),
            ).also {
                it.settKlarTilBehandling(hendelse)
            }

        oppgaveRepository.lagre(oppgave)

        generellOppgaveDataRepository.lagre(
            GenerellOppgaveData(
                oppgaveId = oppgave.oppgaveId,
                oppgaveType = hendelse.oppgaveType,
                tittel = hendelse.tittel,
                beskrivelse = hendelse.beskrivelse,
                strukturertData = hendelse.strukturertData,
            ),
        )

        logger.info { "Opprettet generell oppgave ${oppgave.oppgaveId} med type ${hendelse.oppgaveType}" }
    }

    private fun JsonNode.isMissingOrNull(): Boolean = this.isMissingNode || this.isNull
}
