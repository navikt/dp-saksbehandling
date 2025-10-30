package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import java.time.LocalDateTime

object ModellTestHelper {
    internal fun lagOppgave(
        tilstandType: Type = KLAR_TIL_BEHANDLING,
        behandler: Saksbehandler? = null,
        skjermesSomEgneAnsatte: Boolean = false,
        adressebeskyttelseGradering: AdressebeskyttelseGradering = UGRADERT,
        tilstandslogg: Tilstandslogg = Tilstandslogg(),
        emneknagger: Set<String> = emptySet(),
        hendelse: Hendelse = TomHendelse,
        meldingOmVedtakKilde: Oppgave.MeldingOmVedtak =
            Oppgave.MeldingOmVedtak(
                kilde = Oppgave.MeldingOmVedtakKilde.DP_SAK,
                kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
            ),
    ): Oppgave {
        val tilstand =
            when (tilstandType) {
                OPPRETTET -> Oppgave.Opprettet
                KLAR_TIL_BEHANDLING -> Oppgave.KlarTilBehandling
                FERDIG_BEHANDLET -> Oppgave.FerdigBehandlet
                UNDER_BEHANDLING -> Oppgave.UnderBehandling
                PAA_VENT -> Oppgave.PåVent
                KLAR_TIL_KONTROLL -> Oppgave.KlarTilKontroll
                UNDER_KONTROLL -> Oppgave.UnderKontroll()
                AVVENTER_LÅS_AV_BEHANDLING -> Oppgave.AvventerLåsAvBehandling
                AVVENTER_OPPLÅSING_AV_BEHANDLING -> Oppgave.AvventerOpplåsingAvBehandling
                AVBRUTT -> Oppgave.Avbrutt
            }
        val person =
            lagPerson(
                skjermesSomEgneAnsatte = skjermesSomEgneAnsatte,
                adressebeskyttelseGradering = adressebeskyttelseGradering,
            )
        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                opprettet = LocalDateTime.now(),
                utløstAv = UtløstAvType.SØKNAD,
                hendelse = hendelse,
            )
        return Oppgave.rehydrer(
            oppgaveId = UUIDv7.ny(),
            behandlerIdent = behandler?.navIdent,
            opprettet = LocalDateTime.now(),
            emneknagger = emneknagger,
            tilstand = tilstand,
            utsattTil = null,
            tilstandslogg = tilstandslogg,
            person = person,
            behandling = behandling,
            meldingOmVedtak = meldingOmVedtakKilde,
        )
    }

    internal fun lagSaksbehandler(saksbehandlerTilgang: TilgangType = TilgangType.SAKSBEHANDLER) =
        Saksbehandler(
            navIdent = "saksbehandler",
            grupper = setOf(),
            tilganger = setOf(saksbehandlerTilgang),
        )

    internal fun lagPerson(
        skjermesSomEgneAnsatte: Boolean = false,
        adressebeskyttelseGradering: AdressebeskyttelseGradering = UGRADERT,
    ) = Person(
        id = UUIDv7.ny(),
        ident = "12345678910",
        skjermesSomEgneAnsatte = skjermesSomEgneAnsatte,
        adressebeskyttelseGradering = adressebeskyttelseGradering,
    )
}
