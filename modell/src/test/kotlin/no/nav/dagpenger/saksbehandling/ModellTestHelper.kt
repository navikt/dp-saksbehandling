package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVBRUTT_MASKINELT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.hendelser.DpBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import java.time.LocalDateTime

object ModellTestHelper {
    internal const val PERSON_IDENT = "12345612345"

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
                AVBRUTT_MASKINELT -> Oppgave.AvbruttMaskinelt
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
                utløstAv = HendelseBehandler.DpBehandling.Søknad,
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
        ident = PERSON_IDENT,
        skjermesSomEgneAnsatte = skjermesSomEgneAnsatte,
        adressebeskyttelseGradering = adressebeskyttelseGradering,
    )

    internal val søknadId = UUIDv7.ny()
    internal val søknadBehandlingId = UUIDv7.ny()
    internal val ferietilleggBehandlingId = UUIDv7.ny()
    internal val søknadsbehandlingOpprettetHendelse =
        SøknadsbehandlingOpprettetHendelse(
            søknadId = søknadId,
            behandlingId = søknadBehandlingId,
            ident = PERSON_IDENT,
            opprettet = LocalDateTime.now(),
            basertPåBehandling = null,
            behandlingskjedeId = søknadBehandlingId,
        )
    internal val ferietilleggOpprettetHendelse =
        DpBehandlingOpprettetHendelse(
            behandlingId = ferietilleggBehandlingId,
            ident = PERSON_IDENT,
            opprettet = LocalDateTime.now(),
            basertPåBehandling = null,
            behandlingskjedeId = ferietilleggBehandlingId,
            type = HendelseBehandler.DpBehandling.Ferietillegg,
            eksternId = UUIDv7.ny().toString(),
        )
    internal val søknadBehandling = lagSøknadBehandling()
    internal val ferietilleggBehandling = lagFerietilleggBehandling()
    internal val dagpengeSak = lagDagpengeSak()
    internal val ferietilleggSak = lagFerietilleggSak()

    internal fun lagSøknadBehandling(hendelse: SøknadsbehandlingOpprettetHendelse = søknadsbehandlingOpprettetHendelse) =
        Behandling(
            behandlingId = søknadBehandlingId,
            opprettet = hendelse.opprettet,
            utløstAv = HendelseBehandler.DpBehandling.Søknad,
            hendelse = hendelse,
        )

    internal fun lagFerietilleggBehandling(
        ferietilleggOpprettetHendelse: DpBehandlingOpprettetHendelse = ModellTestHelper.ferietilleggOpprettetHendelse,
    ) = Behandling(
        behandlingId = ferietilleggOpprettetHendelse.behandlingId,
        opprettet = ferietilleggOpprettetHendelse.opprettet,
        utløstAv = ferietilleggOpprettetHendelse.type,
        hendelse = ferietilleggOpprettetHendelse,
    )

    internal fun lagDagpengeSak(behandlinger: Set<Behandling> = setOf(søknadBehandling)): Sak =
        Sak(
            sakId = behandlinger.first().behandlingId,
            opprettet = behandlinger.first().opprettet,
            behandlinger = behandlinger.toMutableSet(),
        )

    internal fun lagFerietilleggSak(behandlinger: Set<Behandling> = setOf(ferietilleggBehandling)): Sak =
        Sak(
            sakId =
                behandlinger
                    .single {
                        it.utløstAv == HendelseBehandler.DpBehandling.Ferietillegg &&
                            it.hendelse is DpBehandlingOpprettetHendelse &&
                            it.hendelse.basertPåBehandling == null
                    }.behandlingId,
            opprettet = behandlinger.first().opprettet,
            behandlinger = behandlinger.toMutableSet(),
        )

    internal fun lagSakHistorikk(
        person: Person = lagPerson(),
        saker: Set<Sak> = setOf(dagpengeSak, ferietilleggSak),
    ): SakHistorikk =
        SakHistorikk(
            person = person,
            saker = saker.toMutableSet(),
        )
}
