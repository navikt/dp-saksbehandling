package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.GodkjennBehandlingFeiletException
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.helper.vedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.IkkeRelevantAvklaringHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlarTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ToTrinnskontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.ForslagTilVedtakMottak
import no.nav.dagpenger.saksbehandling.mottak.VedtakFattetMottak
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

class OppgaveMediatorTest {
    private val testIdent = "12345612345"
    private val saksbehandler = "saksbehandlerIdent"
    private val beslutter = "beslutterIdent"
    private val sak = Sak("12342", "Arena")
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
                        adresseBeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                    ),
                )
        }
    private val behandlingKlientMock =
        mockk<BehandlingKlient>().also {
            every { it.godkjennBehandling(any(), any(), any()) } returns Result.success(Unit)
        }
    private val skjermingKlientMock =
        mockk<SkjermingKlient>(relaxed = true).also {
            coEvery { it.erSkjermetPerson(any()) } returns Result.success(false)
        }
    private val emneknagger = setOf("EØSArbeid", "SykepengerSiste36Måneder")

    @Test
    fun `Skal kunne sette oppgave til klar-til-kontroll`() {
        withMigratedDb { dataSource ->
            val oppgave = dataSource.lagTestoppgave(UNDER_BEHANDLING)
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(dataSource),
                    skjermingKlientMock,
                    pdlKlientMock,
                    behandlingKlientMock,
                    mockk(),
                )
            oppgaveMediator.gjørKlarTilKontroll(
                KlarTilKontrollHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = saksbehandler,
                ),
            )
            val oppgaveTilKontroll = oppgaveMediator.hentOppgave(oppgave.oppgaveId)
            oppgaveTilKontroll.tilstand().type shouldBe KLAR_TIL_KONTROLL
            oppgaveTilKontroll.behandlerIdent shouldBe null
            oppgaveTilKontroll.sisteSaksbehandler() shouldBe saksbehandler
        }
    }

    @Test
    fun `Skal kunne ta en oppgave under kontroll`() {
        withMigratedDb { dataSource ->
            val oppgave = dataSource.lagTestoppgave(KLAR_TIL_KONTROLL)
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(dataSource),
                    skjermingKlientMock,
                    pdlKlientMock,
                    behandlingKlientMock,
                    mockk(),
                )
            oppgaveMediator.tildelTotrinnskontroll(
                ToTrinnskontrollHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = beslutter,
                    utførtAv = beslutter,
                ),
            )
            val oppgaveUnderKontroll = oppgaveMediator.hentOppgave(oppgave.oppgaveId)
            oppgaveUnderKontroll.tilstand().type shouldBe UNDER_KONTROLL
            oppgaveUnderKontroll.behandlerIdent shouldBe beslutter
            oppgaveUnderKontroll.sisteSaksbehandler() shouldBe saksbehandler
            oppgaveUnderKontroll.sisteBeslutter() shouldBe beslutter
        }
    }

    @Test
    fun `Skal ignorere ForslagTilVedtakHendelse hvis oppgave ikke finnes for den behandlingen`() {
        withMigratedDb { datasource ->
            val repo = PostgresOppgaveRepository(datasource)
            val oppgaveMediator =
                OppgaveMediator(repo, skjermingKlientMock, pdlKlientMock, behandlingKlientMock, mockk())
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
    fun `Skal kunne slette avklaringer når de blir irrelevante`() {
        withMigratedDb { datasource ->
            val testEmneknagger = setOf("a", "b", "c")

            val oppgave = datasource.lagTestoppgave(tilstand = OPPRETTET)

            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(datasource),
                    skjermingKlientMock,
                    pdlKlientMock,
                    behandlingKlientMock,
                    mockk(),
                )

            oppgaveMediator.settOppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = UUIDv7.ny(),
                    behandlingId = oppgave.behandlingId,
                    emneknagger = testEmneknagger,
                ),
            )

            oppgaveMediator.hentOppgave(oppgave.oppgaveId).emneknagger shouldContainAll testEmneknagger

            oppgaveMediator.fjernEmneknagg(
                IkkeRelevantAvklaringHendelse(
                    ident = testIdent,
                    behandlingId = oppgave.behandlingId,
                    ikkeRelevantEmneknagg = "a",
                ),
            )

            oppgaveMediator.hentOppgave(oppgave.oppgaveId).emneknagger shouldContainAll testEmneknagger.minus("a")
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir vedtatt`() {
        withMigratedDb { datasource ->
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(datasource),
                    skjermingKlientMock,
                    pdlKlientMock,
                    behandlingKlientMock,
                    mockk(),
                )
            val utsendingMediator = mockk<UtsendingMediator>(relaxed = true)

            BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)
            VedtakFattetMottak(testRapid, oppgaveMediator, utsendingMediator)

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
            oppgave.behandlingId shouldBe behandlingId
            oppgave.emneknagger shouldContainAll emneknagger

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler,
                    utførtAv = saksbehandler,
                ),
            )

            val tildeltOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId)
            tildeltOppgave.tilstand().type shouldBe UNDER_BEHANDLING
            tildeltOppgave.behandlerIdent shouldBe saksbehandler

            testRapid.sendTestMessage(
                vedtakFattetHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    sakId = sak.id.toInt(),
                ),
            )

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId)
            ferdigbehandletOppgave.tilstand().type shouldBe FERDIG_BEHANDLET
        }
    }

    @Test
    fun `Livssyklus for oppgave ferdigstilles med melding om vedtak fra saksbehandler`() {
        val behandlingId = UUIDv7.ny()
        val saksbehandlerToken = "token"
        val behandlingClientMock =
            mockk<BehandlingKlient>().also {
                every {
                    it.godkjennBehandling(
                        behandlingId = behandlingId,
                        ident = testIdent,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } returns Result.success(Unit)
            }
        withMigratedDb { datasource ->
            val utsendingMediator = UtsendingMediator(PostgresUtsendingRepository(datasource))
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(datasource),
                    skjermingKlient = skjermingKlientMock,
                    pdlKlient = pdlKlientMock,
                    behandlingKlient = behandlingClientMock,
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
            oppgave.behandlingId shouldBe behandlingId
            oppgave.emneknagger shouldContainAll emneknagger

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler,
                    utførtAv = saksbehandler,
                ),
            )

            val tildeltOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId)
            tildeltOppgave.tilstand().type shouldBe UNDER_BEHANDLING
            tildeltOppgave.behandlerIdent shouldBe saksbehandler

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
                behandlingClientMock.godkjennBehandling(behandlingId, testIdent, saksbehandlerToken)
            }

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId)
            ferdigbehandletOppgave.tilstand().type shouldBe FERDIG_BEHANDLET

            val utsending = utsendingMediator.hent(ferdigbehandletOppgave.oppgaveId)
            utsending.brev() shouldBe meldingOmVedtak
            utsending.oppgaveId shouldBe ferdigbehandletOppgave.oppgaveId
            utsending.ident shouldBe ferdigbehandletOppgave.ident
        }
    }

    @Test
    fun `Skal slette utsending hvis godkjenning av behandling feiler`() {
        val behandlingId = UUIDv7.ny()
        val saksbehandlerToken = "token"
        val behandlingClientMock =
            mockk<BehandlingKlient>().also {
                every {
                    it.godkjennBehandling(
                        behandlingId = behandlingId,
                        ident = testIdent,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } returns Result.failure(RuntimeException("Feil ved godkjenning av behandling"))
            }

        withMigratedDb { datasource ->
            val utsendingMediator = UtsendingMediator(PostgresUtsendingRepository(datasource))
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(datasource),
                    skjermingKlient = skjermingKlientMock,
                    pdlKlient = pdlKlientMock,
                    behandlingKlient = behandlingClientMock,
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
            oppgave.behandlingId shouldBe behandlingId
            oppgave.emneknagger shouldContainAll emneknagger

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler,
                    utførtAv = saksbehandler,
                ),
            )

            val tildeltOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId)
            tildeltOppgave.tilstand().type shouldBe UNDER_BEHANDLING
            tildeltOppgave.behandlerIdent shouldBe saksbehandler

            val meldingOmVedtak = "<H1>Hei</H1><p>Her er et brev</p>"

            shouldThrow<GodkjennBehandlingFeiletException> {
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
                behandlingClientMock.godkjennBehandling(behandlingId, testIdent, saksbehandlerToken)
            }

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId)
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
                    it.godkjennBehandling(
                        behandlingId = behandlingId,
                        ident = testIdent,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } returns Result.success(Unit)
            }
        withMigratedDb { datasource ->
            val utsendingMediator = UtsendingMediator(PostgresUtsendingRepository(datasource))
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(datasource),
                    skjermingKlient = skjermingKlientMock,
                    pdlKlient = pdlKlientMock,
                    behandlingKlient = behandlingClientMock,
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
                    ansvarligIdent = saksbehandler,
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
                behandlingClientMock.godkjennBehandling(behandlingId, testIdent, saksbehandlerToken)
            }

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId)
            ferdigbehandletOppgave.tilstand().type shouldBe FERDIG_BEHANDLET

            utsendingMediator.finnUtsendingFor(ferdigbehandletOppgave.oppgaveId) shouldBe null
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir avbrutt`() {
        withMigratedDb { datasource ->
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(datasource),
                    skjermingKlient = skjermingKlientMock,
                    pdlKlient = pdlKlientMock,
                    behandlingKlient = behandlingKlientMock,
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
            val oppgaver = oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)
            oppgaver.size shouldBe 1
            oppgaver.single().behandlingId shouldBe behandlingId

            oppgaveMediator.avbrytOppgave(
                BehandlingAvbruttHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = testIdent,
                ),
            )

            assertThrows<DataNotFoundException> {
                oppgaveMediator.hentOppgave(oppgaver.single().oppgaveId)
            }
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir satt på vent`() {
        withMigratedDb { datasource ->
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(datasource),
                    skjermingKlient = skjermingKlientMock,
                    pdlKlient = pdlKlientMock,
                    behandlingKlient = behandlingKlientMock,
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
                    ansvarligIdent = saksbehandler,
                    utførtAv = saksbehandler,
                ),
            )

            val utSattTil = LocalDate.now().plusDays(17)
            oppgaveMediator.utsettOppgave(
                UtsettOppgaveHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    navIdent = saksbehandler,
                    utsattTil = utSattTil,
                    beholdOppgave = false,
                    utførtAv = saksbehandler,
                ),
            )

            oppgaveMediator.hentAlleOppgaverMedTilstand(PAA_VENT).size shouldBe 1
        }
    }

    private fun DataSource.lagTestoppgave(tilstand: Oppgave.Tilstand.Type): Oppgave {
        val oppgaveMediator =
            OppgaveMediator(
                repository = PostgresOppgaveRepository(this),
                skjermingKlientMock,
                pdlKlientMock,
                behandlingKlientMock,
                mockk(),
            )
        val utsendingMediator = mockk<UtsendingMediator>(relaxed = true)

        BehandlingOpprettetMottak(testRapid, oppgaveMediator, pdlKlientMock, skjermingKlientMock)
        VedtakFattetMottak(testRapid, oppgaveMediator, utsendingMediator)

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
            return oppgaveMediator.hentOppgave(oppgave.oppgaveId)
        }

        oppgaveMediator.tildelOppgave(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = saksbehandler,
                utførtAv = saksbehandler,
            ),
        )

        if (tilstand == UNDER_BEHANDLING) {
            return oppgaveMediator.hentOppgave(oppgave.oppgaveId)
        }

        oppgaveMediator.gjørKlarTilKontroll(
            KlarTilKontrollHendelse(
                oppgaveId = oppgave.oppgaveId,
                utførtAv = saksbehandler,
            ),
        )

        if (tilstand == KLAR_TIL_KONTROLL) {
            return oppgaveMediator.hentOppgave(oppgave.oppgaveId)
        }

        oppgaveMediator.tildelTotrinnskontroll(
            ToTrinnskontrollHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutter,
                utførtAv = beslutter,
            ),
        )

        return oppgaveMediator.hentOppgave(oppgave.oppgaveId)
    }
}
