package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

val testPerson = lagPerson()

fun lagPerson(
    ident: String = lagTilfeldigIdent(),
    addresseBeskyttelseGradering: AdressebeskyttelseGradering = UGRADERT,
    skjermesSomEgneAnsatte: Boolean = false,
) = Person(
    ident = ident,
    skjermesSomEgneAnsatte = skjermesSomEgneAnsatte,
    adressebeskyttelseGradering = addresseBeskyttelseGradering,
)

val opprettetNå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)

fun lagOppgave(
    tilstand: Oppgave.Tilstand = KlarTilBehandling,
    opprettet: LocalDateTime = opprettetNå,
    saksbehandlerIdent: String? = null,
    skjermesSomEgneAnsatte: Boolean = false,
    adressebeskyttelseGradering: AdressebeskyttelseGradering = UGRADERT,
    person: Person = lagPerson(addresseBeskyttelseGradering = adressebeskyttelseGradering, skjermesSomEgneAnsatte = skjermesSomEgneAnsatte),
    behandlingId: UUID = UUIDv7.ny(),
    behandlingType: BehandlingType = BehandlingType.RETT_TIL_DAGPENGER,
    emneknagger: Set<String> = emptySet(),
    utsattTil: LocalDate? = null,
    tilstandslogg: Tilstandslogg = Tilstandslogg(),
    oppgaveId: UUID = UUIDv7.ny(),
): Oppgave {
    return Oppgave.rehydrer(
        oppgaveId = oppgaveId,
        behandlerIdent = saksbehandlerIdent,
        opprettet = opprettet,
        emneknagger = emneknagger,
        tilstand = tilstand,
        utsattTil = utsattTil,
        tilstandslogg = tilstandslogg,
        behandlingId = behandlingId,
        behandlingType = behandlingType,
        person = person,
    )
}

fun lagBehandling(
    behandlingId: UUID = UUIDv7.ny(),
    opprettet: LocalDateTime = opprettetNå,
    hendelse: Hendelse = TomHendelse,
    type: BehandlingType = BehandlingType.RETT_TIL_DAGPENGER,
): Behandling {
    return Behandling(
        behandlingId = behandlingId,
        opprettet = opprettet,
        hendelse = hendelse,
        type = type,
    )
}

fun lagUtsending(
    tilstand: Utsending.Tilstand,
    oppgaveId: UUID,
) = Utsending(
    tilstand = tilstand,
    ident = lagTilfeldigIdent(),
    oppgaveId = oppgaveId,
    brev = "brev",
    pdfUrn = null,
    journalpostId = "journalpostId",
    distribusjonId = "distribusjonId",
    utsendingSak = null,
)

fun lagTilfeldigIdent() =
    (1..11)
        .map { Random.nextInt(0, 10) }
        .joinToString("")
