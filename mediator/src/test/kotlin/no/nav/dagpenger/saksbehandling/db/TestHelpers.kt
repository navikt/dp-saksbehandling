package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

val testPerson =
    lagPerson()

fun lagPerson(
    ident: String = "12345678901",
    addresseBeskyttelseGradering: AdressebeskyttelseGradering = UGRADERT,
) = Person(
    ident = ident,
    skjermesSomEgneAnsatte = false,
    adressebeskyttelseGradering = addresseBeskyttelseGradering,
)

val opprettetNå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)

fun lagOppgave(
    tilstand: Oppgave.Tilstand = KlarTilBehandling,
    opprettet: LocalDateTime = opprettetNå,
    saksbehandlerIdent: String? = null,
    person: Person = testPerson,
    behandling: Behandling = lagBehandling(person = person),
    emneknagger: Set<String> = emptySet(),
    utsattTil: LocalDate? = null,
    tilstandslogg: Tilstandslogg = Tilstandslogg(),
    oppgaveId: UUID = UUIDv7.ny(),
): Oppgave {
    return Oppgave.rehydrer(
        oppgaveId = oppgaveId,
        ident = person.ident,
        behandlerIdent = saksbehandlerIdent,
        behandlingId = behandling.behandlingId,
        opprettet = opprettet,
        emneknagger = emneknagger,
        tilstand = tilstand,
        behandling = behandling,
        utsattTil = utsattTil,
        tilstandslogg = tilstandslogg,
    )
}

fun lagBehandling(
    behandlingId: UUID = UUIDv7.ny(),
    opprettet: LocalDateTime = opprettetNå,
    person: Person = testPerson,
    hendelse: Hendelse = TomHendelse,
): Behandling {
    return Behandling(
        behandlingId = behandlingId,
        person = person,
        opprettet = opprettet,
        hendelse = hendelse,
    )
}
