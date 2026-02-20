package no.nav.dagpenger.saksbehandling.db.oppgave

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Notat
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface OppgaveRepository {
    fun hentOppgave(oppgaveId: UUID): Oppgave

    fun lagre(oppgave: Oppgave)

    fun finnOppgaverFor(
        ident: String,
        antall: Int? = 50,
    ): List<Oppgave>

    fun søk(søkeFilter: Søkefilter): PostgresOppgaveRepository.OppgaveSøkResultat

    fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave>

    fun tildelOgHentNesteOppgave(
        nesteOppgaveHendelse: NesteOppgaveHendelse,
        filter: TildelNesteOppgaveFilter,
    ): Oppgave?

    fun hentOppgaveIdFor(behandlingId: UUID): UUID?

    fun hentOppgaveFor(behandlingId: UUID): Oppgave

    fun finnOppgaveFor(behandlingId: UUID): Oppgave?

    fun personSkjermesSomEgneAnsatte(oppgaveId: UUID): Boolean?

    fun adresseGraderingForPerson(oppgaveId: UUID): AdressebeskyttelseGradering

    fun finnNotat(oppgaveTilstandLoggId: UUID): Notat?

    fun lagreNotatFor(oppgave: Oppgave): LocalDateTime

    fun slettNotatFor(oppgave: Oppgave)

    fun finnOppgaverPåVentMedUtgåttFrist(frist: LocalDate): List<UUID>

    fun oppgaveTilstandForSøknad(
        ident: String,
        søknadId: UUID,
    ): Oppgave.Tilstand.Type?
}
