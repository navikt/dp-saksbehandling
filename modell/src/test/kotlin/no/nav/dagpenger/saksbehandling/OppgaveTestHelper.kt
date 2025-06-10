package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.BEHANDLES_I_ARENA
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import java.time.LocalDateTime

object OppgaveTestHelper {
    internal fun lagOppgave(
        tilstandType: Type = KLAR_TIL_BEHANDLING,
        behandler: Saksbehandler? = null,
        skjermesSomEgneAnsatte: Boolean = false,
        adressebeskyttelseGradering: AdressebeskyttelseGradering = UGRADERT,
        tilstandslogg: Tilstandslogg = Tilstandslogg(),
        emneknagger: Set<String> = emptySet(),
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
                BEHANDLES_I_ARENA -> Oppgave.BehandlesIArena
            }
        val person =
            Person(
                id = UUIDv7.ny(),
                ident = "12345678910",
                skjermesSomEgneAnsatte = skjermesSomEgneAnsatte,
                adressebeskyttelseGradering = adressebeskyttelseGradering,
            )
        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                opprettet = LocalDateTime.now(),
            )
        return Oppgave.rehydrer(
            oppgaveId = UUIDv7.ny(),
            behandlerIdent = behandler?.navIdent,
            opprettet = LocalDateTime.now(),
            emneknagger = emneknagger,
            tilstand = tilstand,
            utsattTil = null,
            tilstandslogg = tilstandslogg,
            behandlingId = behandling.behandlingId,
            behandlingType = behandling.type,
            person = person,
        )
    }

    internal fun lagSaksbehandler(saksbehandlerTilgang: TilgangType) =
        Saksbehandler(
            navIdent = "saksbehandler",
            grupper = setOf(),
            tilganger = setOf(saksbehandlerTilgang),
        )
}
