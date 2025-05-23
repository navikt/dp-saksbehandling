package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Emneknagg.PåVent.AVVENT_MELDEKORT
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerLåsAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerOpplåsingAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.BehandlesIArena
import no.nav.dagpenger.saksbehandling.Oppgave.FerdigBehandlet
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilKontroll
import no.nav.dagpenger.saksbehandling.Oppgave.Opprettet
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.BEHANDLES_I_ARENA
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.EGNE_ANSATTE
import no.nav.dagpenger.saksbehandling.TilgangType.FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.behandling.BehandlingException
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKreverIkkeTotrinnskontrollException
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SlettNotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.ForslagTilVedtakMottak
import no.nav.dagpenger.saksbehandling.mottak.VedtakFattetMottak
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream
import javax.sql.DataSource

class
OppgaveMediatorTest {
    private val testIdent = "12345612345"
    private val saksbehandler = Saksbehandler("saksbehandlerIdent", setOf())
    private val beslutter = Saksbehandler("beslutterIdent", setOf(), setOf(BESLUTTER))
    private val behandlerDTO =
        BehandlerDTO(
            ident = "saksbehandlerIdent",
            fornavn = "Saks",
            etternavn = "Behandler",
            enhet =
                BehandlerDTOEnhetDTO(
                    navn = "Enhet",
                    enhetNr = "1234",
                    postadresse = "Postadresse",
                ),
        )

    private val testInspektør =
        Saksbehandler(
            "beslutterIdent",
            setOf(),
            setOf(
                SAKSBEHANDLER,
                BESLUTTER,
                EGNE_ANSATTE,
                FORTROLIG_ADRESSE,
                STRENGT_FORTROLIG_ADRESSE,
                STRENGT_FORTROLIG_ADRESSE_UTLAND,
            ),
        )
    private val testRapid = TestRapid()
    private val pdlKlientMock =
        mockk<PDLKlient>(relaxed = true).also {
            coEvery { it.person(any()) } returns
                Result.success(
                    PDLPersonIntern(
                        ident = "tacimates",
                        fornavn = "commune",
                        etternavn = "convallis",
                        mellomnavn = null,
                        fødselsdato = LocalDate.of(1984, 3, 1),
                        alder = 20,
                        statsborgerskap = null,
                        kjønn = PDLPerson.Kjonn.KVINNE,
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
                    ),
                )
        }

    val behandlingIDKreverIkkeTotrinnskontroll = UUIDv7.ny()

    private val behandlingKlientMock =
        mockk<BehandlingKlient>().also {
            every { it.godkjenn(any(), any(), any()) } returns Result.success(Unit)
            coEvery { it.kreverTotrinnskontroll(behandlingIDKreverIkkeTotrinnskontroll, any()) } returns
                Result.success(
                    false,
                )
            coEvery {
                it.kreverTotrinnskontroll(
                    not(behandlingIDKreverIkkeTotrinnskontroll),
                    any(),
                )
            } returns Result.success(true)
            every { it.sendTilbake(any(), any(), any()) } returns Result.success(Unit)
            every { it.beslutt(any(), any(), any()) } returns Result.success(Unit)
        }
    private val skjermingKlientMock =
        mockk<SkjermingKlient>(relaxed = true).also {
            coEvery { it.erSkjermetPerson(any()) } returns Result.success(false)
        }

    private val utsendingMediatorMock =
        mockk<UtsendingMediator>(relaxed = true).also {
            coEvery { it.opprettUtsending(any(), any(), any()) } returns UUIDv7.ny()
        }

    private val saksbehandlerOppslagMock =
        mockk<SaksbehandlerOppslag>().also {
            coEvery { it.hentSaksbehandler(saksbehandler.navIdent) } returns behandlerDTO
        }

    private val oppslagMock =
        Oppslag(
            pdlKlient = pdlKlientMock,
            relevanteJournalpostIdOppslag = mockk(),
            saksbehandlerOppslag = saksbehandlerOppslagMock,
            skjermingKlient = skjermingKlientMock,
        )

    private val emneknagger = setOf("Emneknagg 1", "Emneknagg 2")

    @Test
    fun `Skal kunne sette oppgave til kontroll`() {
        withMigratedDb { dataSource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val oppgave = dataSource.lagTestoppgave(UNDER_BEHANDLING)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingKlientMock,
                    utsendingMediator = utsendingMediatorMock,
                    meldingOmVedtakKlient = mockk(),
                ).also {
                    it.setRapidsConnection(testRapid)
                }
            oppgaveMediator.sendTilKontroll(
                SendTilKontrollHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = saksbehandler,
                ),
                saksbehandlerToken = "testToken",
            )
            val oppgaveTilKontroll = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            oppgaveTilKontroll.tilstand().type shouldBe KLAR_TIL_KONTROLL
            oppgaveTilKontroll.behandlerIdent shouldBe null
            oppgaveTilKontroll.sisteSaksbehandler() shouldBe saksbehandler.navIdent
        }
    }

    @Test
    fun `Skal kunne ta en oppgave under kontroll`() {
        withMigratedDb { dataSource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val oppgave = dataSource.lagTestoppgave(KLAR_TIL_KONTROLL)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingKlientMock,
                    utsendingMediator = mockk(),
                    meldingOmVedtakKlient = mockk(),
                ).also {
                    it.setRapidsConnection(testRapid)
                }
            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = beslutter.navIdent,
                    utførtAv = beslutter,
                ),
            )
            val oppgaveUnderKontroll = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            oppgaveUnderKontroll.tilstand().type shouldBe UNDER_KONTROLL
            oppgaveUnderKontroll.behandlerIdent shouldBe beslutter.navIdent
            oppgaveUnderKontroll.sisteSaksbehandler() shouldBe saksbehandler.navIdent
            oppgaveUnderKontroll.sisteBeslutter() shouldBe beslutter.navIdent
        }
    }

    @Test
    fun `Skal kunne lagre og slette et notat på en oppgave`() {
        withMigratedDb { dataSource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val oppgave = dataSource.lagTestoppgave(UNDER_KONTROLL)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingKlientMock,
                    utsendingMediator = mockk(),
                    meldingOmVedtakKlient = mockk(),
                )

            oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør).tilstand().notat() shouldBe null

            oppgaveMediator.lagreNotat(
                notatHendelse =
                    NotatHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        tekst = "Dette er et notat",
                        utførtAv = testInspektør,
                    ),
            )

            oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør).tilstand().notat().let {
                require(it != null) { "Notatet er null" }
                it.hentTekst() shouldBe "Dette er et notat"
            }

            oppgaveMediator.slettNotat(
                slettNotatHendelse =
                    SlettNotatHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = testInspektør,
                    ),
            )
            oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør).tilstand().notat() shouldBe null
        }
    }

    @Test
    fun `Skal ignorere ForslagTilVedtakHendelse hvis oppgave ikke finnes for den behandlingen`() {
        withMigratedDb { datasource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    behandlingKlient = behandlingKlientMock,
                    utsendingMediator = mockk(),
                    oppslag = oppslagMock,
                    meldingOmVedtakKlient = mockk(),
                ).also {
                    it.setRapidsConnection(testRapid)
                }
            ForslagTilVedtakMottak(rapidsConnection = testRapid, oppgaveMediator = oppgaveMediator)

            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = "ad",
                    søknadId = UUIDv7.ny(),
                    behandlingId = UUIDv7.ny(),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
            oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING).size shouldBe 0
        }
    }

    @Test
    fun `Skal kunne motta flere forslag til vedtak hendelser og oppdatere emneknaggene med de siste mottatte`() {
        withMigratedDb { datasource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val testEmneknagger1 = setOf("a", "b", "c")
            val testEmneknagger2 = setOf("x", "y")
            val oppgave = datasource.lagTestoppgave(tilstand = OPPRETTET)

            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingKlientMock,
                    meldingOmVedtakKlient = mockk(),
                    utsendingMediator = mockk(),
                )

            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = UUIDv7.ny(),
                    behandlingId = oppgave.behandling.behandlingId,
                    emneknagger = testEmneknagger1,
                ),
            )

            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = UUIDv7.ny(),
                    behandlingId = oppgave.behandling.behandlingId,
                    emneknagger = testEmneknagger2,
                ),
            )

            val oppdatertOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)

            oppdatertOppgave.emneknagger shouldBe testEmneknagger2
            oppdatertOppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
        }
    }

    companion object {
        @JvmStatic
        private fun skalEttersendingTilSøknadVarsles(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(Opprettet, false),
                Arguments.of(KlarTilBehandling, false),
                Arguments.of(BehandlesIArena, false),
                Arguments.of(AvventerOpplåsingAvBehandling, false),
                Arguments.of(AvventerLåsAvBehandling, false),
                Arguments.of(Oppgave.PåVent, true),
                Arguments.of(UnderBehandling, true),
                Arguments.of(KlarTilKontroll, true),
                Arguments.of(Oppgave.UnderKontroll(), true),
                Arguments.of(FerdigBehandlet, true),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("skalEttersendingTilSøknadVarsles")
    fun `Finnes det en oppgave til behandling`(
        tilstand: Oppgave.Tilstand,
        skalLageGosysOppgave: Boolean,
    ) {
        val hendelse =
            SøknadsbehandlingOpprettetHendelse(
                søknadId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = LocalDateTime.now(),
            )
        val behandling =
            Behandling(
                behandlingId = hendelse.behandlingId,
                person =
                    Person(
                        id = UUIDv7.ny(),
                        ident = hendelse.ident,
                        skjermesSomEgneAnsatte = false,
                        adressebeskyttelseGradering = UGRADERT,
                    ),
                opprettet = LocalDateTime.now(),
                hendelse = hendelse,
            )
        val oppgave = lagOppgave(tilstand = tilstand, behandling = behandling)

        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            val oppgaveRepository = PostgresOppgaveRepository(ds)

            oppgaveRepository.lagre(oppgave)

            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingKlientMock,
                    meldingOmVedtakKlient = mockk(),
                    utsendingMediator = mockk(),
                )
            oppgaveMediator.skalEttersendingTilSøknadVarsles(
                hendelse.søknadId,
                hendelse.ident,
            ) shouldBe skalLageGosysOppgave
        }
    }

    @Test
    fun `Skal kunne hente en person basert op ident og fnr`() {
        val personId = UUIDv7.ny()
        val person =
            Person(
                id = personId,
                ident = OppgaveApiTestHelper.testPerson.ident,
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
            )
        val oppgave = lagOppgave(person = person)

        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            val oppgaveRepository = PostgresOppgaveRepository(ds)

            oppgaveRepository.lagre(oppgave)

            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingKlientMock,
                    meldingOmVedtakKlient = mockk(),
                    utsendingMediator = mockk(),
                )
            oppgaveMediator.hentPerson(person.ident).shouldBe(person)
            oppgaveMediator.hentPerson(personId).shouldBe(person)
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir vedtatt`() {
        val meldingOmVedtakKlientMock =
            mockk<MeldingOmVedtakKlient>().also {
                coEvery {
                    it.lagOgHentMeldingOmVedtak(
                        person = any(),
                        saksbehandler = any(),
                        beslutter = any(),
                        behandlingId = any(),
                        saksbehandlerToken = any(),
                    )
                } returns Result.success("<html>brev</html>")
            }

        withMigratedDb { datasource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingKlientMock,
                    meldingOmVedtakKlient = meldingOmVedtakKlientMock,
                    utsendingMediator = utsendingMediatorMock,
                )

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)

            val søknadId = UUIDv7.ny()
            val behandlingId = UUIDv7.ny()
            val søknadsbehandlingOpprettetHendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    ident = testIdent,
                    opprettet = LocalDateTime.now(),
                )

            oppgaveMediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)
            oppgaveMediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)
            oppgaveMediator.hentAlleOppgaverMedTilstand(OPPRETTET).let { oppgaver ->
                oppgaver.size shouldBe 1
                oppgaver.single().behandling.hendelse shouldBe søknadsbehandlingOpprettetHendelse
            }

            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                ),
            )

            val oppgaverKlarTilBehandling = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)

            oppgaverKlarTilBehandling.size shouldBe 1
            val oppgave = oppgaverKlarTilBehandling.single()
            oppgave.behandling.behandlingId shouldBe behandlingId
            oppgave.emneknagger shouldContainAll emneknagger

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            val tildeltOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            tildeltOppgave.tilstand().type shouldBe UNDER_BEHANDLING
            tildeltOppgave.behandlerIdent shouldBe saksbehandler.navIdent

            runBlocking {
                oppgaveMediator.ferdigstillOppgave(
                    oppgaveId = oppgave.oppgaveId,
                    saksBehandler = saksbehandler,
                    saksbehandlerToken = "token",
                )
            }

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            ferdigbehandletOppgave.tilstand().type shouldBe FERDIG_BEHANDLET
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som feiler ved laging av html`() {
        val meldingOmVedtakKlientMock =
            mockk<MeldingOmVedtakKlient>().also {
                coEvery {
                    it.lagOgHentMeldingOmVedtak(
                        person = any(),
                        saksbehandler = any(),
                        beslutter = any(),
                        behandlingId = any(),
                        saksbehandlerToken = any(),
                    )
                } returns Result.failure(MeldingOmVedtakKlient.KanIkkeLageMeldingOmVedtak("Feil ved henting/lagring av melding om vedtak"))
            }

        withMigratedDb { datasource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingKlientMock,
                    meldingOmVedtakKlient = meldingOmVedtakKlientMock,
                    utsendingMediator = utsendingMediatorMock,
                )

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)

            val søknadId = UUIDv7.ny()
            val behandlingId = UUIDv7.ny()
            val søknadsbehandlingOpprettetHendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    ident = testIdent,
                    opprettet = LocalDateTime.now(),
                )

            oppgaveMediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)
            oppgaveMediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)
            oppgaveMediator.hentAlleOppgaverMedTilstand(OPPRETTET).let { oppgaver ->
                oppgaver.size shouldBe 1
                oppgaver.single().behandling.hendelse shouldBe søknadsbehandlingOpprettetHendelse
            }

            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                ),
            )

            val oppgaverKlarTilBehandling = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)

            oppgaverKlarTilBehandling.size shouldBe 1
            val oppgave = oppgaverKlarTilBehandling.single()
            oppgave.behandling.behandlingId shouldBe behandlingId
            oppgave.emneknagger shouldContainAll emneknagger

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            val tildeltOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            tildeltOppgave.tilstand().type shouldBe UNDER_BEHANDLING
            tildeltOppgave.behandlerIdent shouldBe saksbehandler.navIdent

            runBlocking {
                shouldThrow<MeldingOmVedtakKlient.KanIkkeLageMeldingOmVedtak> {
                    oppgaveMediator.ferdigstillOppgave(
                        oppgaveId = oppgave.oppgaveId,
                        saksBehandler = saksbehandler,
                        saksbehandlerToken = "token",
                    )
                }
            }

            val oppgaveUnderBehandling = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            oppgaveUnderBehandling.tilstand().type shouldBe UNDER_BEHANDLING
        }
    }

    @Test
    fun `Livssyklus for oppgave ferdigstilles med melding om vedtak fra saksbehandler`() {
        val behandlingId = UUIDv7.ny()
        val saksbehandlerToken = "token"
        val behandlingClientMock =
            mockk<BehandlingKlient>().also {
                every {
                    it.godkjenn(
                        behandlingId = behandlingId,
                        ident = testIdent,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } returns Result.success(Unit)
            }
        withMigratedDb { datasource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val utsendingMediator = UtsendingMediator(PostgresUtsendingRepository(datasource))
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingClientMock,
                    meldingOmVedtakKlient = mockk(),
                    utsendingMediator = utsendingMediator,
                )

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)

            val søknadId = UUIDv7.ny()
            val søknadsbehandlingOpprettetHendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    ident = testIdent,
                    opprettet = LocalDateTime.now(),
                )

            oppgaveMediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)
            oppgaveMediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)
            oppgaveMediator.hentAlleOppgaverMedTilstand(OPPRETTET).let { oppgaver ->
                oppgaver.size shouldBe 1
                oppgaver.single().behandling.hendelse shouldBe søknadsbehandlingOpprettetHendelse
            }

            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                ),
            )

            val oppgaverKlarTilBehandling = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)

            oppgaverKlarTilBehandling.size shouldBe 1
            val oppgave = oppgaverKlarTilBehandling.single()
            oppgave.behandling.behandlingId shouldBe behandlingId
            oppgave.emneknagger shouldContainAll emneknagger

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            val tildeltOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            tildeltOppgave.tilstand().type shouldBe UNDER_BEHANDLING
            tildeltOppgave.behandlerIdent shouldBe saksbehandler.navIdent

            val meldingOmVedtak = "<H1>Hei</H1><p>Her er et brev</p>"
            oppgaveMediator.ferdigstillOppgave(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = meldingOmVedtak,
                    utførtAv = saksbehandler,
                ),
                "token",
            )

            verify(exactly = 1) {
                behandlingClientMock.godkjenn(behandlingId, testIdent, saksbehandlerToken)
            }

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            ferdigbehandletOppgave.tilstand().type shouldBe FERDIG_BEHANDLET

            val utsending = utsendingMediator.hent(ferdigbehandletOppgave.oppgaveId)
            utsending.brev() shouldBe meldingOmVedtak
            utsending.oppgaveId shouldBe ferdigbehandletOppgave.oppgaveId
            utsending.ident shouldBe ferdigbehandletOppgave.behandling.person.ident
        }
    }

    @Test
    fun `Skal slette utsending hvis godkjenning av behandling feiler`() {
        val behandlingId = UUIDv7.ny()
        val saksbehandlerToken = "token"
        val behandlingClientMock =
            mockk<BehandlingKlient>().also {
                every {
                    it.godkjenn(
                        behandlingId = behandlingId,
                        ident = testIdent,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } returns Result.failure(BehandlingException("Feil ved godkjenning av behandling", 403))
            }

        withMigratedDb { datasource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val utsendingMediator = UtsendingMediator(PostgresUtsendingRepository(datasource))
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingClientMock,
                    meldingOmVedtakKlient = mockk(),
                    utsendingMediator = utsendingMediator,
                )

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)

            val søknadId = UUIDv7.ny()
            val søknadsbehandlingOpprettetHendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    ident = testIdent,
                    opprettet = LocalDateTime.now(),
                )

            oppgaveMediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)
            oppgaveMediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)
            oppgaveMediator.hentAlleOppgaverMedTilstand(OPPRETTET).let { oppgaver ->
                oppgaver.size shouldBe 1
                oppgaver.single().behandling.hendelse shouldBe søknadsbehandlingOpprettetHendelse
            }

            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                ),
            )

            val oppgaverKlarTilBehandling = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)

            oppgaverKlarTilBehandling.size shouldBe 1
            val oppgave = oppgaverKlarTilBehandling.single()
            oppgave.behandling.behandlingId shouldBe behandlingId
            oppgave.emneknagger shouldContainAll emneknagger

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            val tildeltOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            tildeltOppgave.tilstand().type shouldBe UNDER_BEHANDLING
            tildeltOppgave.behandlerIdent shouldBe saksbehandler.navIdent

            val meldingOmVedtak = "<H1>Hei</H1><p>Her er et brev</p>"

            shouldThrow<BehandlingException> {
                oppgaveMediator.ferdigstillOppgave(
                    GodkjentBehandlingHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        meldingOmVedtak = meldingOmVedtak,
                        utførtAv = saksbehandler,
                    ),
                    "token",
                )
            }

            verify(exactly = 1) {
                behandlingClientMock.godkjenn(behandlingId, testIdent, saksbehandlerToken)
            }

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            ferdigbehandletOppgave.tilstand().type shouldBe UNDER_BEHANDLING

            utsendingMediator.finnUtsendingFor(ferdigbehandletOppgave.oppgaveId) shouldBe null
        }
    }

    @Test
    fun `Livssyklus for oppgave ferdigstilles med brev-håndtering i Arena`() {
        val behandlingId = UUIDv7.ny()
        val saksbehandlerToken = "token"
        val behandlingClientMock =
            mockk<BehandlingKlient>().also {
                every {
                    it.godkjenn(
                        behandlingId = behandlingId,
                        ident = testIdent,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } returns Result.success(Unit)
            }
        withMigratedDb { datasource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val utsendingMediator = UtsendingMediator(PostgresUtsendingRepository(datasource))
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingClientMock,
                    meldingOmVedtakKlient = mockk(),
                    utsendingMediator = utsendingMediator,
                )

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)

            val søknadId = UUIDv7.ny()
            val søknadsbehandlingOpprettetHendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    ident = testIdent,
                    opprettet = LocalDateTime.now(),
                )

            oppgaveMediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)
            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                ),
            )

            val oppgave = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING).single()
            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            oppgaveMediator.ferdigstillOppgave(
                GodkjennBehandlingMedBrevIArena(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = saksbehandler,
                ),
                "token",
            )

            verify(exactly = 1) {
                behandlingClientMock.godkjenn(behandlingId, testIdent, saksbehandlerToken)
            }

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            ferdigbehandletOppgave.tilstand().type shouldBe FERDIG_BEHANDLET

            utsendingMediator.finnUtsendingFor(ferdigbehandletOppgave.oppgaveId) shouldBe null
        }
    }

    @Test
    fun `Skal kaste feil dersom godkjenn behandling feiler`() {
        val behandlingId = UUIDv7.ny()
        val saksbehandlerToken = "token"
        val behandlingClientMock =
            mockk<BehandlingKlient>().also {
                every {
                    it.godkjenn(
                        behandlingId = behandlingId,
                        ident = testIdent,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } returns Result.failure(BehandlingException("Feil ved godkjenning av behandling", 403))
            }
        withMigratedDb { datasource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val utsendingMediator = UtsendingMediator(PostgresUtsendingRepository(datasource))
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingClientMock,
                    meldingOmVedtakKlient = mockk(),
                    utsendingMediator = utsendingMediator,
                )

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)

            val søknadId = UUIDv7.ny()
            val søknadsbehandlingOpprettetHendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    ident = testIdent,
                    opprettet = LocalDateTime.now(),
                )

            oppgaveMediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)
            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                ),
            )

            val oppgave = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING).single()
            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            shouldThrow<BehandlingException> {
                oppgaveMediator.ferdigstillOppgave(
                    GodkjennBehandlingMedBrevIArena(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                    ),
                    "token",
                )
            }

            verify(exactly = 1) {
                behandlingClientMock.godkjenn(behandlingId, testIdent, saksbehandlerToken)
            }

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            ferdigbehandletOppgave.tilstand().type shouldBe UNDER_BEHANDLING
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir avbrutt`() {
        withMigratedDb { datasource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingKlientMock,
                    meldingOmVedtakKlient = mockk(),
                    utsendingMediator = mockk(),
                )

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)

            val søknadId = UUIDv7.ny()
            val behandlingId = UUIDv7.ny()

            oppgaveMediator.opprettOppgaveForBehandling(
                søknadsbehandlingOpprettetHendelse =
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = søknadId,
                        behandlingId = behandlingId,
                        ident = testIdent,
                        opprettet = LocalDateTime.now(),
                    ),
            )
            oppgaveMediator.hentAlleOppgaverMedTilstand(OPPRETTET).size shouldBe 1

            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                ),
            )

            val oppgave = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING).single()
            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            oppgaveMediator.avbrytOppgave(
                BehandlingAvbruttHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = testIdent,
                ),
            )

            val avbruttOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            avbruttOppgave.tilstand().type shouldBe BEHANDLES_I_ARENA
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir opprettet og så avbrutt`() {
        withMigratedDb { datasource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingKlientMock,
                    meldingOmVedtakKlient = mockk(),
                    utsendingMediator = mockk(),
                )

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)

            val søknadId = UUIDv7.ny()
            val behandlingId = UUIDv7.ny()

            oppgaveMediator.opprettOppgaveForBehandling(
                søknadsbehandlingOpprettetHendelse =
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = søknadId,
                        behandlingId = behandlingId,
                        ident = testIdent,
                        opprettet = LocalDateTime.now(),
                    ),
            )
            oppgaveMediator.avbrytOppgave(
                BehandlingAvbruttHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = testIdent,
                ),
            )

            oppgaveMediator.hentOppgaveIdFor(behandlingId) shouldBe null
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir satt på vent`() {
        withMigratedDb { datasource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingKlientMock,
                    meldingOmVedtakKlient = mockk(),
                    utsendingMediator = mockk(),
                )

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)

            val søknadId = UUIDv7.ny()
            val behandlingId = UUIDv7.ny()

            oppgaveMediator.opprettOppgaveForBehandling(
                søknadsbehandlingOpprettetHendelse =
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = søknadId,
                        behandlingId = behandlingId,
                        ident = testIdent,
                        opprettet = LocalDateTime.now(),
                    ),
            )
            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                ),
            )

            val oppgave = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING).single()

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            val utSattTil = LocalDate.now().plusDays(17)
            oppgaveMediator.utsettOppgave(
                UtsettOppgaveHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    navIdent = saksbehandler.navIdent,
                    utsattTil = utSattTil,
                    beholdOppgave = false,
                    utførtAv = saksbehandler,
                    årsak = AVVENT_MELDEKORT,
                ),
            )

            oppgaveMediator.hentAlleOppgaverMedTilstand(PAA_VENT)
                .single().emneknagger shouldContain AVVENT_MELDEKORT.visningsnavn
        }
    }

    @Test
    fun `Kast feil når send til kontroll kalles uten at det kreves totrinnskontroll`() {
        withMigratedDb { datasource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient =
                        behandlingKlientMock.also {
                            coEvery {
                                it.godkjenn(
                                    any(),
                                    any(),
                                    any(),
                                )
                            } throws BehandlingException("Behandling krever ikke totrinnskontroll", 403)
                        },
                    utsendingMediator = mockk(),
                    meldingOmVedtakKlient = mockk(),
                ).also {
                    it.setRapidsConnection(testRapid)
                }

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)

            val søknadId = UUIDv7.ny()
            val behandlingId = behandlingIDKreverIkkeTotrinnskontroll

            oppgaveMediator.opprettOppgaveForBehandling(
                søknadsbehandlingOpprettetHendelse =
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = søknadId,
                        behandlingId = behandlingId,
                        ident = testIdent,
                        opprettet = LocalDateTime.now(),
                    ),
            )
            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                ),
            )

            val oppgave = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING).single()

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            shouldThrow<BehandlingKreverIkkeTotrinnskontrollException> {
                oppgaveMediator.sendTilKontroll(
                    sendTilKontrollHendelse =
                        SendTilKontrollHendelse(
                            oppgaveId = oppgave.oppgaveId,
                            utførtAv = saksbehandler,
                        ),
                    saksbehandlerToken = "testtoken",
                )
            }
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som krever totrinnskontroll`() {
        val søknadId = UUIDv7.ny()
        val behandlingId = UUIDv7.ny()
        val behandlingKlientMock =
            mockk<BehandlingKlient>().also {
                coEvery {
                    it.kreverTotrinnskontroll(behandlingId, any())
                } returns Result.success(true)
                every { it.godkjenn(behandlingId = behandlingId, any(), any()) } returns Result.success(Unit)
                every { it.beslutt(behandlingId = behandlingId, any(), any()) } returns Result.success(Unit)
                every { it.sendTilbake(behandlingId = behandlingId, any(), any()) } returns Result.success(Unit)
            }

        withMigratedDb { datasource ->
            val personRepository = PostgresPersonRepository(dataSource)
            val oppgaveRepository = PostgresOppgaveRepository(dataSource)
            val oppgaveMediator =
                OppgaveMediator(
                    personRepository = personRepository,
                    oppgaveRepository = oppgaveRepository,
                    oppslag = oppslagMock,
                    behandlingKlient = behandlingKlientMock,
                    utsendingMediator = mockk(relaxed = true),
                    meldingOmVedtakKlient = mockk(),
                ).also {
                    it.setRapidsConnection(testRapid)
                }

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)

            oppgaveMediator.opprettOppgaveForBehandling(
                søknadsbehandlingOpprettetHendelse =
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = søknadId,
                        behandlingId = behandlingId,
                        ident = testIdent,
                        opprettet = LocalDateTime.now(),
                    ),
            )

            oppgaveMediator.hentAlleOppgaverMedTilstand(OPPRETTET).single().tilstandslogg.single().let {
                it.tilstand shouldBe OPPRETTET
                it.hendelse.shouldBeInstanceOf<SøknadsbehandlingOpprettetHendelse>()
            }

            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                ),
            )

            val oppgave = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING).single()

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            oppgaveMediator.sendTilKontroll(
                sendTilKontrollHendelse =
                    SendTilKontrollHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                    ),
                saksbehandlerToken = "testtoken",
            )

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = beslutter.navIdent,
                    utførtAv = beslutter,
                ),
            )

            oppgaveMediator.returnerTilSaksbehandling(
                ReturnerTilSaksbehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = beslutter,
                ),
                beslutterToken = "testtoken",
            )

            oppgaveMediator.sendTilKontroll(
                sendTilKontrollHendelse =
                    SendTilKontrollHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                    ),
                saksbehandlerToken = "testtoken",
            )

            oppgaveMediator.ferdigstillOppgave(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "test",
                    utførtAv = beslutter,
                ),
                "testtoken",
            )
        }
    }

    private fun DataSource.lagTestoppgave(tilstand: Type): Oppgave {
        val personRepository = PostgresPersonRepository(this)
        val oppgaveMediator =
            OppgaveMediator(
                personRepository = personRepository,
                oppgaveRepository = PostgresOppgaveRepository(this),
                behandlingKlient = behandlingKlientMock,
                oppslag = oppslagMock,
                utsendingMediator = mockk(),
                meldingOmVedtakKlient = mockk(),
            ).also {
                it.setRapidsConnection(testRapid)
            }

        BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)
        VedtakFattetMottak(testRapid, oppgaveMediator)

        val søknadId = UUIDv7.ny()
        val behandlingId = UUIDv7.ny()
        val søknadsbehandlingOpprettetHendelse =
            SøknadsbehandlingOpprettetHendelse(
                søknadId = søknadId,
                behandlingId = behandlingId,
                ident = testIdent,
                opprettet = LocalDateTime.now(),
            )

        oppgaveMediator.opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse)

        val oppgave = oppgaveMediator.hentAlleOppgaverMedTilstand(OPPRETTET).single()

        if (tilstand == OPPRETTET) {
            return oppgave
        }

        oppgaveMediator.settOppgaveKlarTilBehandling(
            ForslagTilVedtakHendelse(
                ident = testIdent,
                søknadId = søknadId,
                behandlingId = behandlingId,
                emneknagger = emneknagger,
            ),
        )

        if (tilstand == KLAR_TIL_BEHANDLING) {
            return oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
        }

        oppgaveMediator.tildelOppgave(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = saksbehandler.navIdent,
                utførtAv = saksbehandler,
            ),
        )

        if (tilstand == UNDER_BEHANDLING) {
            return oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
        }

        oppgaveMediator.sendTilKontroll(
            SendTilKontrollHendelse(
                oppgaveId = oppgave.oppgaveId,
                utførtAv = saksbehandler,
            ),
            saksbehandlerToken = "testtoken",
        )

        if (tilstand == KLAR_TIL_KONTROLL) {
            return oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
        }

        oppgaveMediator.tildelOppgave(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutter.navIdent,
                utførtAv = beslutter,
            ),
        )

        if (tilstand == UNDER_KONTROLL) {
            return oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
        }

        return oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
    }
}
