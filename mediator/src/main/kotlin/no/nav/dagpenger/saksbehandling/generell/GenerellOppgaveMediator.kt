package no.nav.dagpenger.saksbehandling.generell

import PersonMediator
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.generell.GenerellOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

class GenerellOppgaveMediator(
    private val generellOppgaveRepository: GenerellOppgaveRepository,
    private val personMediator: PersonMediator,
    private val generellOppgaveBehandler: GenerellOppgaveBehandler,
    private val sakMediator: SakMediator,
    private val sakRepository: SakRepository,
    private val oppgaveMediator: OppgaveMediator,
) {
    fun taImot(hendelse: OpprettGenerellOppgaveHendelse): GenerellOppgave {
        val person = personMediator.finnEllerOpprettPerson(hendelse.ident)

        val generellOppgave =
            GenerellOppgave.opprett(
                person = person,
                emneknagg = hendelse.emneknagg,
                tittel = hendelse.tittel,
                beskrivelse = hendelse.beskrivelse,
                strukturertData = hendelse.strukturertData,
            )

        generellOppgaveRepository.lagre(generellOppgave)

        val behandling =
            Behandling(
                behandlingId = generellOppgave.id,
                opprettet = generellOppgave.opprettet,
                hendelse = hendelse,
                utløstAv = UtløstAvType.GENERELL,
            )

        sakRepository.lagreBehandling(
            personId = person.id,
            sakId = null,
            behandling = behandling,
        )

        oppgaveMediator.lagOppgaveForGenerellOppgave(
            hendelse = hendelse,
            behandling = behandling,
            person = person,
            emneknagg = generellOppgave.emneknagg,
        )

        logger.info { "Opprettet generell oppgave ${generellOppgave.id} med emneknagg ${generellOppgave.emneknagg}" }

        return generellOppgave
    }

    fun ferdigstill(hendelse: FerdigstillGenerellOppgaveHendelse) {
        val generellOppgave = hent(id = hendelse.generellOppgaveId, saksbehandler = hendelse.utførtAv)

        generellOppgave.startFerdigstilling(
            vurdering = hendelse.vurdering,
            valgtSakId = hendelse.aksjon.valgtSakId,
        )
        generellOppgaveRepository.lagre(generellOppgave)

        val ferdigstiltHendelse = generellOppgaveBehandler.utførAksjon(generellOppgave, hendelse)
        logger.info { "Ferdigstiller generell oppgave ${generellOppgave.id} med aksjon ${hendelse.aksjon.type}" }

        generellOppgave.ferdigstill(
            aksjonType = ferdigstiltHendelse.aksjonType,
            opprettetBehandlingId = ferdigstiltHendelse.opprettetBehandlingId,
        )
        generellOppgaveRepository.lagre(generellOppgave)

        oppgaveMediator.ferdigstillOppgave(ferdigstiltHendelse)
    }

    fun hent(
        id: UUID,
        saksbehandler: Saksbehandler,
    ): GenerellOppgave =
        generellOppgaveRepository.hent(id).also { oppgave ->
            oppgave.person.harTilgang(saksbehandler)
        }

    fun hentLovligeSaker(ident: String): List<Sak> = sakMediator.finnSakHistorikk(ident)?.saker() ?: emptyList()
}
