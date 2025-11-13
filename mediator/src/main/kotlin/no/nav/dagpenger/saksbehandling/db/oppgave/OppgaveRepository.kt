package no.nav.dagpenger.saksbehandling.db.oppgave

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Notat
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.RettTilDagpengerOppgave
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface OppgaveRepository {
    fun hentOppgave(oppgaveId: UUID): RettTilDagpengerOppgave

    fun lagre(oppgave: Oppgave)

    fun finnOppgaverFor(ident: String): List<RettTilDagpengerOppgave>

    fun søk(søkeFilter: Søkefilter): PostgresOppgaveRepository.OppgaveSøkResultat

    fun hentAlleOppgaverMedTilstand(tilstand: RettTilDagpengerOppgave.Tilstand.Type): List<RettTilDagpengerOppgave>

    fun tildelOgHentNesteOppgave(
        nesteOppgaveHendelse: NesteOppgaveHendelse,
        filter: TildelNesteOppgaveFilter,
    ): RettTilDagpengerOppgave?

    fun hentOppgaveIdFor(behandlingId: UUID): UUID?

    fun hentOppgaveFor(behandlingId: UUID): RettTilDagpengerOppgave

    fun finnOppgaveFor(behandlingId: UUID): RettTilDagpengerOppgave?

    fun personSkjermesSomEgneAnsatte(oppgaveId: UUID): Boolean?

    fun adresseGraderingForPerson(oppgaveId: UUID): AdressebeskyttelseGradering

    fun finnNotat(oppgaveTilstandLoggId: UUID): Notat?

    fun lagreNotatFor(oppgave: RettTilDagpengerOppgave): LocalDateTime

    fun slettNotatFor(oppgave: RettTilDagpengerOppgave)

    fun finnOppgaverPåVentMedUtgåttFrist(frist: LocalDate): List<UUID>

    fun oppgaveTilstandForSøknad(
        ident: String,
        søknadId: UUID,
    ): RettTilDagpengerOppgave.Tilstand.Type?
}
