package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.UNDER_KONTROLL
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
        meldingOmVedtakKilde: RettTilDagpenger.MeldingOmVedtak =
            RettTilDagpenger.MeldingOmVedtak(
                kilde = RettTilDagpenger.MeldingOmVedtakKilde.DP_SAK,
                kontrollertGosysBrev = RettTilDagpenger.KontrollertBrev.IKKE_RELEVANT,
            ),
    ): RettTilDagpenger {
        val tilstand =
            when (tilstandType) {
                OPPRETTET -> RettTilDagpenger.Opprettet
                KLAR_TIL_BEHANDLING -> RettTilDagpenger.KlarTilBehandling
                FERDIG_BEHANDLET -> RettTilDagpenger.FerdigBehandlet
                UNDER_BEHANDLING -> RettTilDagpenger.UnderBehandling
                PAA_VENT -> RettTilDagpenger.PåVent
                KLAR_TIL_KONTROLL -> RettTilDagpenger.KlarTilKontroll
                UNDER_KONTROLL -> RettTilDagpenger.UnderKontroll()
                AVVENTER_LÅS_AV_BEHANDLING -> RettTilDagpenger.AvventerLåsAvBehandling
                AVVENTER_OPPLÅSING_AV_BEHANDLING -> RettTilDagpenger.AvventerOpplåsingAvBehandling
                AVBRUTT -> RettTilDagpenger.Avbrutt
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
        return RettTilDagpenger.rehydrer(
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
