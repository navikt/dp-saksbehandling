package no.nav.dagpenger.saksbehandling.api

import PersonMediator
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerLåsAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerOpplåsingAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.BehandlesIArena
import no.nav.dagpenger.saksbehandling.Oppgave.FerdigBehandlet
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilKontroll
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Oppgave.Opprettet
import no.nav.dagpenger.saksbehandling.Oppgave.PåVent
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
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.UnderKontroll
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.SikkerhetstiltakIntern
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal object OppgaveApiTestHelper {
    const val TEST_IDENT = "12345612345"
    val TEST_UUID = UUIDv7.ny()
    const val SAKSBEHANDLER_IDENT = "SaksbehandlerIdent"
    val defaultSaksbehandlerADGruppe = listOf("SaksbehandlerADGruppe")
    const val BESLUTTER_IDENT = "BeslutterIdent"
    val SOKNAD_ID = "01953789-f215-744e-9f6e-a55509bae78b".toUUID()
    private val mockAzure = mockAzure()
    private val fødselsdato = LocalDate.of(2000, 1, 1)

    fun withOppgaveApi(
        oppgaveMediator: OppgaveMediator = mockk<OppgaveMediator>(relaxed = true),
        oppgaveDTOMapper: OppgaveDTOMapper = mockk<OppgaveDTOMapper>(relaxed = true),
        personMediator: PersonMediator = mockk(relaxed = true),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application {
                installerApis(
                    oppgaveMediator,
                    oppgaveDTOMapper,
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    personMediator = personMediator,
                )
            }
            test()
        }
    }

    fun withOppgaveApi(
        oppgaveMediator: OppgaveMediator = mockk<OppgaveMediator>(relaxed = true),
        pdlKlient: PDLKlient = mockk(relaxed = true),
        relevanteJournalpostIdOppslag: RelevanteJournalpostIdOppslag = mockk(relaxed = true),
        saksbehandlerOppslag: SaksbehandlerOppslag = mockk(relaxed = true),
        oppgaveRepository: OppgaveRepository = mockk<OppgaveRepository>(relaxed = true),
        personMediator: PersonMediator = mockk(relaxed = true),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application {
                installerApis(
                    oppgaveMediator,
                    OppgaveDTOMapper(
                        Oppslag(
                            pdlKlient,
                            relevanteJournalpostIdOppslag,
                            saksbehandlerOppslag,
                            skjermingKlient = mockk(relaxed = true),
                        ),
                        OppgaveHistorikkDTOMapper(oppgaveRepository, saksbehandlerOppslag),
                        mockk<SakMediator>(relaxed = true),
                    ),
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    personMediator = personMediator,
                )
            }
            test()
        }
    }

    fun HttpRequestBuilder.autentisert(token: String = gyldigSaksbehandlerToken()) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    fun gyldigSaksbehandlerToken(
        adGrupper: List<String> = emptyList(),
        navIdent: String = SAKSBEHANDLER_IDENT,
    ): String {
        return mockAzure.lagTokenMedClaims(
            mapOf(
                "groups" to defaultSaksbehandlerADGruppe + adGrupper,
                "NAVident" to navIdent,
            ),
        )
    }

    fun gyldigMaskinToken(): String = mockAzure.lagTokenMedClaims(mapOf("idtyp" to "app"))

    fun lagTestOppgaveMedTilstandOgBehandling(
        tilstand: Oppgave.Tilstand.Type,
        tildeltBehandlerIdent: String? = null,
        behandling: Behandling,
        utsattTil: LocalDate? = null,
        opprettet: LocalDateTime = LocalDateTime.now(),
        oppgaveId: UUID = UUIDv7.ny(),
        person: Person =
            Person(
                id = TEST_UUID,
                ident = TEST_IDENT,
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            ),
    ): Oppgave {
        return Oppgave.rehydrer(
            oppgaveId = oppgaveId,
            behandlerIdent = tildeltBehandlerIdent,
            opprettet = opprettet,
            emneknagger = emptySet(),
            tilstand =
                when (tilstand) {
                    OPPRETTET -> Opprettet
                    KLAR_TIL_BEHANDLING -> KlarTilBehandling
                    UNDER_BEHANDLING -> UnderBehandling
                    FERDIG_BEHANDLET -> FerdigBehandlet
                    PAA_VENT -> PåVent
                    KLAR_TIL_KONTROLL -> KlarTilKontroll
                    UNDER_KONTROLL -> UnderKontroll()
                    AVVENTER_LÅS_AV_BEHANDLING -> AvventerLåsAvBehandling
                    AVVENTER_OPPLÅSING_AV_BEHANDLING -> AvventerOpplåsingAvBehandling
                    BEHANDLES_I_ARENA -> BehandlesIArena
                },
            utsattTil = utsattTil,
            behandlingId = behandling.behandlingId,
            behandlingType = behandling.type,
            person = person,
            meldingOmVedtak =
                Oppgave.MeldingOmVedtak(
                    kilde = DP_SAK,
                    kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                ),
            tilstandslogg =
                Tilstandslogg.rehydrer(
                    listOf(
                        Tilstandsendring(
                            tilstand = tilstand,
                            hendelse =
                                ForslagTilVedtakHendelse(
                                    ident = TEST_IDENT,
                                    behandletHendelseId = SOKNAD_ID.toString(),
                                    behandletHendelseType = "Søknad",
                                    behandlingId = behandling.behandlingId,
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
                ),
        )
    }

    fun lagTestOppgaveMedTilstand(
        tilstand: Oppgave.Tilstand.Type,
        saksbehandlerIdent: String? = null,
        skjermesSomEgneAnsatte: Boolean = false,
        utsattTil: LocalDate? = null,
        oprettet: LocalDateTime = LocalDateTime.now(),
        behandlingId: UUID = UUIDv7.ny(),
        oppgaveId: UUID = UUIDv7.ny(),
        person: Person =
            Person(
                id = TEST_UUID,
                ident = TEST_IDENT,
                skjermesSomEgneAnsatte = skjermesSomEgneAnsatte,
                adressebeskyttelseGradering = UGRADERT,
            ),
    ): Oppgave {
        val behandling =
            Behandling(
                behandlingId = behandlingId,
                opprettet = LocalDateTime.now(),
                hendelse = TomHendelse,
            )
        return lagTestOppgaveMedTilstandOgBehandling(
            tilstand = tilstand,
            tildeltBehandlerIdent = saksbehandlerIdent,
            behandling = behandling,
            utsattTil = utsattTil,
            opprettet = oprettet,
            oppgaveId = oppgaveId,
            person = person,
        )
    }

    val testPerson =
        PDLPersonIntern(
            ident = TEST_IDENT,
            fornavn = "PETTER",
            etternavn = "SMART",
            mellomnavn = null,
            fødselsdato = fødselsdato,
            alder = 0,
            statsborgerskap = "NOR",
            kjønn = PDLPerson.Kjonn.UKJENT,
            adresseBeskyttelseGradering = UGRADERT,
            sikkerhetstiltak =
                listOf(
                    SikkerhetstiltakIntern(
                        type = "Tiltakstype",
                        beskrivelse = "To ansatte i samtale",
                        gyldigFom = LocalDate.now(),
                        gyldigTom = LocalDate.now().plusDays(1),
                    ),
                ),
        )
}
