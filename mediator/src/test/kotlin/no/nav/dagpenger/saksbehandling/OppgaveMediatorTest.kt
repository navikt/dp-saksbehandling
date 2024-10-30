package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.EGNE_ANSATTE
import no.nav.dagpenger.saksbehandling.TilgangType.FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.GodkjennBehandlingFeiletException
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.helper.vedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingLåstHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.IkkeRelevantAvklaringHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
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
    private val saksbehandler = Saksbehandler("saksbehandlerIdent", setOf())
    private val beslutter = Saksbehandler("beslutterIdent", setOf(), setOf(BESLUTTER))
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
                        adresseBeskyttelseGradering = UGRADERT,
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
    fun `Skal kunne sette oppgave til AVVENTER_LÅS_AV_BEHANDLING`() {
        withMigratedDb { dataSource ->
            val oppgave = dataSource.lagTestoppgave(UNDER_BEHANDLING)
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(dataSource),
                    skjermingKlientMock,
                    pdlKlientMock,
                    behandlingKlientMock,
                    mockk(),
                ).also {
                    it.setRapidsConnection(testRapid)
                }
            oppgaveMediator.sendTilKontroll(
                SendTilKontrollHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = saksbehandler,
                ),
            )
            val oppgaveTilKontroll = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            oppgaveTilKontroll.tilstand().type shouldBe AVVENTER_LÅS_AV_BEHANDLING
            oppgaveTilKontroll.behandlerIdent shouldBe null
            oppgaveTilKontroll.sisteSaksbehandler() shouldBe saksbehandler.navIdent
        }
    }

    @Test
    fun `Skal kunne ta en oppgave under kontroll`() {
        withMigratedDb { dataSource ->
            val oppgave = dataSource.lagTestoppgave(KLAR_TIL_KONTROLL)
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(dataSource),
                    skjermingKlient = skjermingKlientMock,
                    pdlKlient = pdlKlientMock,
                    behandlingKlient = behandlingKlientMock,
                    utsendingMediator = mockk(),
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
    fun `Skal kunne motta flere forslag til vedtak hendelser og oppdatere emneknaggene`() {
        withMigratedDb { datasource ->
            val testEmneknagger1 = setOf("a", "b", "c")
            val testEmneknagger2 = setOf("x", "y")

            val oppgave = datasource.lagTestoppgave(tilstand = OPPRETTET)

            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(datasource),
                    skjermingKlient = skjermingKlientMock,
                    pdlKlient = pdlKlientMock,
                    behandlingKlient = behandlingKlientMock,
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

            oppdatertOppgave.emneknagger shouldContainAll testEmneknagger1 + testEmneknagger2
            oppdatertOppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
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
                    behandlingId = oppgave.behandling.behandlingId,
                    emneknagger = testEmneknagger,
                ),
            )

            oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør).emneknagger shouldContainAll testEmneknagger

            oppgaveMediator.fjernEmneknagg(
                IkkeRelevantAvklaringHendelse(
                    ident = testIdent,
                    behandlingId = oppgave.behandling.behandlingId,
                    ikkeRelevantEmneknagg = "a",
                ),
            )

            oppgaveMediator.hentOppgave(
                oppgave.oppgaveId,
                testInspektør,
            ).emneknagger shouldContainAll testEmneknagger.minus("a")
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

            testRapid.sendTestMessage(
                vedtakFattetHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    sakId = sak.id.toInt(),
                ),
            )

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
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
                behandlingClientMock.godkjennBehandling(behandlingId, testIdent, saksbehandlerToken)
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
                behandlingClientMock.godkjennBehandling(behandlingId, testIdent, saksbehandlerToken)
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

            shouldThrow<GodkjennBehandlingFeiletException> {
                oppgaveMediator.ferdigstillOppgave(
                    GodkjennBehandlingMedBrevIArena(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                    ),
                    "token",
                )
            }

            verify(exactly = 1) {
                behandlingClientMock.godkjennBehandling(behandlingId, testIdent, saksbehandlerToken)
            }

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            ferdigbehandletOppgave.tilstand().type shouldBe UNDER_BEHANDLING
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
            oppgaver.single().behandling.behandlingId shouldBe behandlingId

            oppgaveMediator.avbrytOppgave(
                BehandlingAvbruttHendelse(
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                    ident = testIdent,
                ),
            )

            assertThrows<DataNotFoundException> {
                oppgaveMediator.hentOppgave(oppgaver.single().oppgaveId, testInspektør)
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
                ),
            )

            oppgaveMediator.hentAlleOppgaverMedTilstand(PAA_VENT).size shouldBe 1
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som krever totrinnskontroll`() {
        withMigratedDb { datasource ->
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(datasource),
                    skjermingKlient = skjermingKlientMock,
                    pdlKlient = pdlKlientMock,
                    behandlingKlient = behandlingKlientMock,
                    utsendingMediator = mockk(),
                ).also {
                    it.setRapidsConnection(testRapid)
                }

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

            oppgaveMediator.sendTilKontroll(
                sendTilKontrollHendelse =
                    SendTilKontrollHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                    ),
            )

            testRapid.inspektør.size shouldBe 1
            testRapid.inspektør.message(0).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                   "@event_name": "oppgave_sendt_til_kontroll",
                   "behandlingId": "${oppgave.behandling.behandlingId}",
                   "ident": "${oppgave.behandling.person.ident}"
                }
                """.trimIndent()

            oppgaveMediator.settOppgaveKlarTilKontroll(
                BehandlingLåstHendelse(
                    behandlingId = oppgave.behandling.behandlingId,
                    søknadId = UUIDv7.ny(),
                    ident = oppgave.behandling.person.ident,
                ),
            )

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = beslutter.navIdent,
                    utførtAv = beslutter,
                ),
            )

            oppgaveMediator.sendTilbakeTilUnderBehandling(
                ReturnerTilSaksbehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = beslutter,
                ),
            )

            testRapid.inspektør.size shouldBe 2
            testRapid.inspektør.message(1).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                   "@event_name": "oppgave_returnert_til_saksbehandling",
                   "behandlingId": "${oppgave.behandling.behandlingId}",
                   "ident": "${oppgave.behandling.person.ident}"
                }
                """.trimIndent()
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
            ).also {
                it.setRapidsConnection(testRapid)
            }
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
        )
        if (tilstand == AVVENTER_LÅS_AV_BEHANDLING) {
            return oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
        }

        oppgaveMediator.settOppgaveKlarTilKontroll(
            BehandlingLåstHendelse(
                behandlingId = oppgave.behandling.behandlingId,
                søknadId = UUIDv7.ny(),
                ident = oppgave.behandling.person.ident,
            ),
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

        return oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
    }
}
