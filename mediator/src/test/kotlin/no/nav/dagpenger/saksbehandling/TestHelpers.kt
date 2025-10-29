package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

val TEST_IDENT = "41952264877"
val testPerson = lagPerson(ident = TEST_IDENT)
val opprettet = LocalDateTime.now()
val oppgaveId = UUIDv7.ny()
const val SAKSBEHANDLER_IDENT = "SaksbehandlerIdent"
val defaultSaksbehandlerADGruppe = listOf("SaksbehandlerADGruppe")
const val BESLUTTER_IDENT = "BeslutterIdent"
val SOKNAD_ID = "01953789-f215-744e-9f6e-a55509bae78b".toUUID()

fun lagTilstandLogg(): Tilstandslogg {
    return Tilstandslogg.rehydrer(
        listOf(
            Tilstandsendring(
                tilstand = KLAR_TIL_BEHANDLING,
                hendelse =
                    ForslagTilVedtakHendelse(
                        ident = testPerson.ident,
                        behandletHendelseId = SOKNAD_ID.toString(),
                        behandletHendelseType = "Søknad",
                        behandlingId = UUID.randomUUID(),
                    ),
                tidspunkt = opprettet,
            ),
            Tilstandsendring(
                tilstand = UNDER_BEHANDLING,
                hendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgaveId,
                        ansvarligIdent = SAKSBEHANDLER_IDENT,
                        utførtAv = Saksbehandler(SAKSBEHANDLER_IDENT, emptySet()),
                    ),
                tidspunkt = opprettet.minusDays(2),
            ),
            Tilstandsendring(
                tilstand = UNDER_KONTROLL,
                hendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgaveId,
                        ansvarligIdent = BESLUTTER_IDENT,
                        utførtAv = Saksbehandler(BESLUTTER_IDENT, emptySet()),
                    ),
                tidspunkt = opprettet.minusDays(1),
            ),
        ),
    )
}

fun lagPerson(
    ident: String = TEST_IDENT,
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
    person: Person =
        lagPerson(
            addresseBeskyttelseGradering = adressebeskyttelseGradering,
            skjermesSomEgneAnsatte = skjermesSomEgneAnsatte,
        ),
    behandling: Behandling = lagBehandling(opprettet = opprettet),
    emneknagger: Set<String> = emptySet(),
    utsattTil: LocalDate? = null,
    tilstandslogg: Tilstandslogg = Tilstandslogg(),
    oppgaveId: UUID = no.nav.dagpenger.saksbehandling.oppgaveId,
): Oppgave {
    return Oppgave.rehydrer(
        oppgaveId = oppgaveId,
        behandlerIdent = saksbehandlerIdent,
        opprettet = opprettet,
        emneknagger = emneknagger,
        tilstand = tilstand,
        utsattTil = utsattTil,
        tilstandslogg = tilstandslogg,
        person = person,
        behandling = behandling,
        meldingOmVedtak =
            Oppgave.MeldingOmVedtak(
                kilde = DP_SAK,
                kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
            ),
    )
}

fun lagBehandling(
    behandlingId: UUID = UUIDv7.ny(),
    opprettet: LocalDateTime = opprettetNå,
    hendelse: Hendelse = TomHendelse,
    utløstAvType: UtløstAvType = UtløstAvType.SØKNAD,
): Behandling {
    return Behandling(
        behandlingId = behandlingId,
        opprettet = opprettet,
        hendelse = hendelse,
        utløstAv = utløstAvType,
    )
}

fun lagUtsending(
    tilstand: Utsending.Tilstand,
    behandlingId: UUID,
) = Utsending(
    tilstand = tilstand,
    ident = TEST_IDENT,
    behandlingId = behandlingId,
    brev = "brev",
    pdfUrn = null,
    journalpostId = "journalpostId",
    distribusjonId = "distribusjonId",
    utsendingSak = null,
)
