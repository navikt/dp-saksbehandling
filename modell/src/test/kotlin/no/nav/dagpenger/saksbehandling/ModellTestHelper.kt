package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.KontrollertBrev.IKKE_RELEVANT
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import java.time.LocalDateTime

object ModellTestHelper {
    internal fun lagOppgave(
        tilstandType: Type = KLAR_TIL_BEHANDLING,
        behandler: Saksbehandler? = null,
        skjermesSomEgneAnsatte: Boolean = false,
        adressebeskyttelseGradering: AdressebeskyttelseGradering = UGRADERT,
        tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
        emneknagger: Set<String> = emptySet(),
        hendelse: Hendelse = TomHendelse,
        meldingOmVedtakKilde: Oppgave.MeldingOmVedtak =
            Oppgave.MeldingOmVedtak(
                kilde = DP_SAK,
                kontrollertGosysBrev = IKKE_RELEVANT,
            ),
    ): RettTilDagpengerOppgave {
        val tilstand =
            when (tilstandType) {
                Type.OPPRETTET -> RettTilDagpengerOppgave.Opprettet
                KLAR_TIL_BEHANDLING -> RettTilDagpengerOppgave.KlarTilBehandling
                Type.FERDIG_BEHANDLET -> RettTilDagpengerOppgave.FerdigBehandlet
                Type.UNDER_BEHANDLING -> RettTilDagpengerOppgave.UnderBehandling
                Type.PAA_VENT -> RettTilDagpengerOppgave.PåVent
                Type.KLAR_TIL_KONTROLL -> RettTilDagpengerOppgave.KlarTilKontroll
                Type.UNDER_KONTROLL -> RettTilDagpengerOppgave.UnderKontroll()
                Type.AVVENTER_LÅS_AV_BEHANDLING -> RettTilDagpengerOppgave.AvventerLåsAvBehandling
                Type.AVVENTER_OPPLÅSING_AV_BEHANDLING -> RettTilDagpengerOppgave.AvventerOpplåsingAvBehandling
                Type.AVBRUTT -> RettTilDagpengerOppgave.Avbrutt
            }
        val person =
            lagPerson(
                skjermesSomEgneAnsatte = skjermesSomEgneAnsatte,
                adressebeskyttelseGradering = adressebeskyttelseGradering,
            )
        val behandling =
            RettTilDagpengerBehandling(
                behandlingId = UUIDv7.ny(),
                opprettet = LocalDateTime.now(),
                utløstAv = UtløstAvType.SØKNAD,
                hendelse = hendelse,
            )
        return RettTilDagpengerOppgave.rehydrer(
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
