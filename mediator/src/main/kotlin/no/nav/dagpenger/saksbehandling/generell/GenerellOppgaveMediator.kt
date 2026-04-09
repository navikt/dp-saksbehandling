package no.nav.dagpenger.saksbehandling.generell

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.generell.GenerellOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonMediator
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

class GenerellOppgaveMediator(
    private val generellOppgaveRepository: GenerellOppgaveRepository,
    private val generellOppgaveBehandler: GenerellOppgaveBehandler,
    private val personMediator: PersonMediator,
    private val sakMediator: SakMediator,
    private val oppgaveMediator: OppgaveMediator,
) {
    fun taImot(hendelse: OpprettGenerellOppgaveHendelse): GenerellOppgave {
        val generellOppgaveId = UUIDv7.ny()
        val person = personMediator.finnEllerOpprettPerson(hendelse.ident)

        val behandling =
            Behandling(
                behandlingId = generellOppgaveId,
                opprettet = hendelse.registrertTidspunkt,
                hendelse = hendelse,
                utløstAv = UtløstAvType.GENERELL,
            )

        sakMediator.lagreBehandling(
            personId = person.id,
            behandling = behandling,
        )

        val generellOppgave =
            GenerellOppgave.opprett(
                id = generellOppgaveId,
                person = person,
                tittel = hendelse.tittel,
                beskrivelse = hendelse.beskrivelse,
                strukturertData = hendelse.strukturertData,
                opprettet = hendelse.registrertTidspunkt,
            )

        generellOppgaveRepository.lagre(generellOppgave)

        oppgaveMediator.lagOppgaveForGenerellOppgave(
            hendelse = hendelse,
            behandling = behandling,
            person = person,
        )

        logger.info { "Opprettet generell oppgave ${generellOppgave.id} med emneknagg ${hendelse.emneknagg}" }

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
