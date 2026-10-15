package no.nav.dagpenger.saksbehandling.db.oppgave

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Notat
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst.IkkeAktiv
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface OppgaveRepository {
    fun hentOppgave(oppgaveId: UUID): Oppgave

    fun lagre(
        oppgave: Oppgave,
        ctx: Transaksjonskontekst = IkkeAktiv,
    )

    fun finnOppgaverFor(
        ident: String,
        antall: Int? = 50,
    ): List<OppgaveOversikt>

    fun søk(søkeFilter: Søkefilter): OppgaveOversiktResultat

    fun tildelOgHentNesteOppgave(
        nesteOppgaveHendelse: NesteOppgaveHendelse,
        filter: TildelNesteOppgaveFilter,
    ): Oppgave?

    fun hentOppgaveIdFor(behandlingId: UUID): UUID?

    fun hentOppgaveForBehandling(behandlingId: UUID): Oppgave

    fun finnOppgaveForBehandling(behandlingId: UUID): Oppgave?

    fun finnOppgaveForSøknad(
        ident: String,
        søknadId: UUID,
    ): Oppgave?

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

    fun hentDistinkteEmneknagger(): Set<String>
}
