package no.nav.dagpenger.saksbehandling.db.oppgave

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Notat
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import java.time.LocalDateTime
import java.util.UUID

interface OppgaveRepository {
    fun finnPerson(ident: String): Person?

    fun hentPerson(ident: String): Person

    fun lagre(person: Person)

    fun finnBehandling(behandlingId: UUID): Behandling?

    fun hentBehandling(behandlingId: UUID): Behandling

    fun lagre(behandling: Behandling)

    fun slettBehandling(behandlingId: UUID)

    fun hentOppgave(oppgaveId: UUID): Oppgave

    fun lagre(oppgave: Oppgave)

    fun finnOppgaverFor(ident: String): List<Oppgave>

    fun søk(
        søkeFilter: Søkefilter,
        orderByOpprettet: Boolean = false,
    ): List<Oppgave>

    fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave>

    fun tildelOgHentNesteOppgave(
        nesteOppgaveHendelse: NesteOppgaveHendelse,
        filter: TildelNesteOppgaveFilter,
    ): Oppgave?

    fun hentOppgaveIdFor(behandlingId: UUID): UUID?

    fun hentOppgaveFor(behandlingId: UUID): Oppgave

    fun finnOppgaveFor(behandlingId: UUID): Oppgave?

    fun fjerneEmneknagg(
        behandlingId: UUID,
        ikkeRelevantEmneknagg: String,
    ): Boolean

    fun personSkjermesSomEgneAnsatte(oppgaveId: UUID): Boolean?

    fun adresseGraderingForPerson(oppgaveId: UUID): AdressebeskyttelseGradering

    fun finnNotat(oppgaveTilstandLoggId: UUID): Notat?

    fun lagreNotatFor(oppgave: Oppgave): LocalDateTime
}
