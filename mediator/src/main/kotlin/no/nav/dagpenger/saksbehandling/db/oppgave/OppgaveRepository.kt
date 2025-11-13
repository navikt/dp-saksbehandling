package no.nav.dagpenger.saksbehandling.db.oppgave

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Notat
import no.nav.dagpenger.saksbehandling.RettTilDagpenger
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface OppgaveRepository {
    fun hentOppgave(oppgaveId: UUID): RettTilDagpenger

    fun lagre(oppgave: RettTilDagpenger)

    fun finnOppgaverFor(ident: String): List<RettTilDagpenger>

    fun søk(søkeFilter: Søkefilter): PostgresOppgaveRepository.OppgaveSøkResultat

    fun hentAlleOppgaverMedTilstand(tilstand: RettTilDagpenger.Tilstand.Type): List<RettTilDagpenger>

    fun tildelOgHentNesteOppgave(
        nesteOppgaveHendelse: NesteOppgaveHendelse,
        filter: TildelNesteOppgaveFilter,
    ): RettTilDagpenger?

    fun hentOppgaveIdFor(behandlingId: UUID): UUID?

    fun hentOppgaveFor(behandlingId: UUID): RettTilDagpenger

    fun finnOppgaveFor(behandlingId: UUID): RettTilDagpenger?

    fun personSkjermesSomEgneAnsatte(oppgaveId: UUID): Boolean?

    fun adresseGraderingForPerson(oppgaveId: UUID): AdressebeskyttelseGradering

    fun finnNotat(oppgaveTilstandLoggId: UUID): Notat?

    fun lagreNotatFor(oppgave: RettTilDagpenger): LocalDateTime

    fun slettNotatFor(oppgave: RettTilDagpenger)

    fun finnOppgaverPåVentMedUtgåttFrist(frist: LocalDate): List<UUID>

    fun oppgaveTilstandForSøknad(
        ident: String,
        søknadId: UUID,
    ): RettTilDagpenger.Tilstand.Type?
}
