package no.nav.dagpenger.saksbehandling

import PersonMediator
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
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
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.behandling.BehandlingException
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKreverIkkeTotrinnskontrollException
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SlettNotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.mottak.ForslagTilVedtakMottak
import no.nav.dagpenger.saksbehandling.mottak.VedtakFattetMottak
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.sak.SakMediator
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
import java.util.UUID
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

    private val behandlingIDKreverIkkeTotrinnskontroll = UUIDv7.ny()

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
    fun `Kan ikke opprette oppgave dersom sak eller behandling ikke finnes`() {
        val personUtenSakHistorikk = "12345678901"
        val personUtenBehandling = "12345678902"

        val sakMediatorMock =
            mockk<SakMediator>().also {
                every { it.finnSakHistorikkk(personUtenSakHistorikk) } returns null
                every { it.finnSakHistorikkk(personUtenBehandling) } returns
                    SakHistorikk(
                        person =
                            Person(
                                id = UUIDv7.ny(),
                                ident = personUtenBehandling,
                                skjermesSomEgneAnsatte = false,
                                adressebeskyttelseGradering = UGRADERT,
                            ),
                        saker = mutableSetOf(),
                    )
            }

        val oppgaveMediator =
            OppgaveMediator(
                oppgaveRepository =
                    mockk<OppgaveRepository>().also {
                        every { it.finnOppgaveFor(behandlingId = any()) } returns null
                    },
                behandlingKlient = mockk(),
                utsendingMediator = mockk(),
                oppslag = mockk(),
                meldingOmVedtakKlient = mockk(),
                sakMediator = sakMediatorMock,
            ).also {
                it.setRapidsConnection(testRapid)
            }

        shouldThrow<IllegalStateException> {
            oppgaveMediator.opprettOppgaveForBehandling(
                BehandlingOpprettetHendelse(
                    behandlingId = UUIDv7.ny(),
                    sakId = UUIDv7.ny(),
                    ident = personUtenSakHistorikk,
                    opprettet = LocalDateTime.now(),
                    type = BehandlingType.KLAGE,
                ),
            )
        }
        shouldThrow<IllegalStateException> {
            oppgaveMediator.opprettOppgaveForBehandling(
                BehandlingOpprettetHendelse(
                    behandlingId = UUIDv7.ny(),
                    sakId = UUIDv7.ny(),
                    ident = personUtenBehandling,
                    opprettet = LocalDateTime.now(),
                    type = BehandlingType.KLAGE,
                ),
            )
        }

        oppgaveMediator.opprettEllerOppdaterOppgave(
            ForslagTilVedtakHendelse(
                ident = personUtenBehandling,
                behandlingId = UUIDv7.ny(),
                søknadId = UUIDv7.ny(),
            ),
        ) shouldBe null

        oppgaveMediator.opprettEllerOppdaterOppgave(
            ForslagTilVedtakHendelse(
                ident = personUtenSakHistorikk,
                behandlingId = UUIDv7.ny(),
                søknadId = UUIDv7.ny(),
            ),
        ) shouldBe null

        testRapid.inspektør.let {
            it.size shouldBe 4
            for (i in 0 until it.size) {
                it.message(i).let { message ->
                    message["@event_name"].asText() shouldBe "saksbehandling_alert"
                    message["alertType"].asText() shouldBe "BEHANDLING_IKKE_FUNNET"
                }
            }
        }
    }

    @Test
    fun `Skal kunne sette oppgave til kontroll`() {
        settOppOppgaveMediator { datasource, oppgaveMediator ->
            val oppgave = datasource.lagTestoppgave(UNDER_BEHANDLING)
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
        settOppOppgaveMediator { datasource, oppgaveMediator ->
            val oppgave = datasource.lagTestoppgave(KLAR_TIL_KONTROLL)
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
        settOppOppgaveMediator { datasource, oppgaveMediator ->
            val oppgave = datasource.lagTestoppgave(UNDER_KONTROLL)
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
        settOppOppgaveMediator { _, oppgaveMediator ->
            ForslagTilVedtakMottak(rapidsConnection = testRapid, oppgaveMediator = oppgaveMediator)

            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = "ad",
                    søknadId = UUIDv7.ny(),
                    behandlingId = UUIDv7.ny(),
                )
            oppgaveMediator.opprettEllerOppdaterOppgave(forslagTilVedtakHendelse)
            oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING).size shouldBe 0
        }
    }

    @Test
    fun `Skal kunne motta flere forslag til vedtak hendelser og oppdatere emneknaggene med de siste mottatte`() {
        settOppOppgaveMediator { datasource, oppgaveMediator ->
            val testEmneknagger1 = setOf("a", "b", "c")
            val testEmneknagger2 = setOf("x", "y")
            val oppgave = datasource.lagTestoppgave(tilstand = KLAR_TIL_BEHANDLING)

            oppgaveMediator.opprettEllerOppdaterOppgave(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = UUIDv7.ny(),
                    behandlingId = oppgave.behandlingId,
                    emneknagger = testEmneknagger1,
                ),
            )

            oppgaveMediator.opprettEllerOppdaterOppgave(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = UUIDv7.ny(),
                    behandlingId = oppgave.behandlingId,
                    emneknagger = testEmneknagger2,
                ),
            )

            val oppdatertOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)

            oppdatertOppgave.emneknagger shouldBe testEmneknagger2
            oppdatertOppgave.tilstand() shouldBe KlarTilBehandling
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
        val person =
            Person(
                id = UUIDv7.ny(),
                ident = hendelse.ident,
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            )
        val behandling =
            Behandling(
                behandlingId = hendelse.behandlingId,
                opprettet = LocalDateTime.now(),
                hendelse = hendelse,
            )
        val oppgave =
            lagOppgave(
                tilstand = tilstand,
                behandlingId = behandling.behandlingId,
                behandlingType = behandling.type,
                person = person,
            )

        settOppOppgaveMediator(hendelse = hendelse) { datasource, oppgaveMediator ->
            val oppgaveRepository = PostgresOppgaveRepository(datasource)

            oppgaveRepository.lagre(oppgave)

            oppgaveMediator.skalEttersendingTilSøknadVarsles(
                hendelse.søknadId,
                hendelse.ident,
            ) shouldBe skalLageGosysOppgave
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir vedtatt`() {
        val behandlingId = UUIDv7.ny()
        val meldingOmVedtakKlientMock =
            mockk<MeldingOmVedtakKlient>().also {
                coEvery {
                    it.lagOgHentMeldingOmVedtak(
                        person = any(),
                        saksbehandler = any(),
                        beslutter = any(),
                        behandlingId = behandlingId,
                        saksbehandlerToken = any(),
                    )
                } returns Result.success("<html>brev</html>")
            }

        settOppOppgaveMediator(movKlient = meldingOmVedtakKlientMock) { datasource, oppgaveMediator ->
            val oppgave =
                datasource.lagTestoppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    behandlingId = behandlingId,
                )

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
        val behandlingId = UUIDv7.ny()
        val meldingOmVedtakKlientMock =
            mockk<MeldingOmVedtakKlient>().also {
                coEvery {
                    it.lagOgHentMeldingOmVedtak(
                        person = any(),
                        saksbehandler = any(),
                        beslutter = any(),
                        behandlingId = behandlingId,
                        saksbehandlerToken = any(),
                    )
                } returns Result.failure(MeldingOmVedtakKlient.KanIkkeLageMeldingOmVedtak("Feil ved henting/lagring av melding om vedtak"))
            }

        settOppOppgaveMediator(movKlient = meldingOmVedtakKlientMock) { datasource, oppgaveMediator ->

            val oppgave =
                datasource.lagTestoppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    behandlingId = behandlingId,
                )

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
        val søknadId = UUIDv7.ny()
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
        settOppOppgaveMediator(
            behandlingKlient = behandlingClientMock,
        ) { datasource, oppgaveMediator ->
            val utsendingMediator = UtsendingMediator(PostgresUtsendingRepository(datasource))

            oppgaveMediator.opprettEllerOppdaterOppgave(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                ),
            )

            val oppgave =
                datasource.lagTestoppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                )

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
            utsending.ident shouldBe ferdigbehandletOppgave.personIdent()
        }
    }

    @Test
    fun `Skal slette utsending hvis godkjenning av behandling feiler`() {
        val behandlingId = UUIDv7.ny()
        val saksbehandlerToken = "token"
        val søknadId = UUIDv7.ny()
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

        settOppOppgaveMediator(behandlingKlient = behandlingClientMock) { datasource, oppgaveMediator ->
            oppgaveMediator.opprettEllerOppdaterOppgave(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                ),
            )

            val oppgave =
                datasource.lagTestoppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                )

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

            UtsendingMediator(PostgresUtsendingRepository(datasource)).also { utsendingMediator ->
                utsendingMediator.finnUtsendingFor(ferdigbehandletOppgave.oppgaveId) shouldBe null
            }
        }
    }

    @Test
    fun `Livssyklus for oppgave ferdigstilles med brev-håndtering i Arena`() {
        val behandlingId = UUIDv7.ny()
        val søknadId = UUIDv7.ny()
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
        settOppOppgaveMediator(behandlingKlient = behandlingClientMock) { datasource, oppgaveMediator ->
            oppgaveMediator.opprettEllerOppdaterOppgave(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                ),
            )

            val oppgave =
                datasource.lagTestoppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                )
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

            UtsendingMediator(PostgresUtsendingRepository(datasource))
                .finnUtsendingFor(ferdigbehandletOppgave.oppgaveId) shouldBe null
        }
    }

    @Test
    fun `Skal kaste feil dersom godkjenn behandling feiler`() {
        val behandlingId = UUIDv7.ny()
        val saksbehandlerToken = "token"
        val søknadId = UUIDv7.ny()
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
        settOppOppgaveMediator(behandlingKlient = behandlingClientMock) { datasource, oppgaveMediator ->

            val oppgave =
                datasource.lagTestoppgave(
                    tilstand = KLAR_TIL_BEHANDLING,
                    behandlingId = behandlingId,
                    søknadId = søknadId,
                )

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
        settOppOppgaveMediator { datasource, oppgaveMediator ->

            val oppgave = datasource.lagTestoppgave()
            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            oppgaveMediator.avbrytOppgave(
                BehandlingAvbruttHendelse(
                    behandlingId = oppgave.behandlingId,
                    søknadId = oppgave.soknadId()!!,
                    ident = testIdent,
                ),
            )

            oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør).tilstand().type shouldBe BEHANDLES_I_ARENA
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir satt på vent`() {
        settOppOppgaveMediator { datasource, oppgaveMediator ->

            val oppgave = datasource.lagTestoppgave(tilstand = KLAR_TIL_BEHANDLING)

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
        val behandlingKlientMock =
            behandlingKlientMock.also {
                coEvery {
                    it.kreverTotrinnskontroll(any(), any())
                } throws BehandlingKreverIkkeTotrinnskontrollException("Behandling krever ikke totrinnskontroll")
            }
        settOppOppgaveMediator(behandlingKlient = behandlingKlientMock) { datasource, oppgaveMediator ->

            val oppgave = datasource.lagTestoppgave(tilstand = KLAR_TIL_BEHANDLING)

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
        val behandlingKlientMock =
            mockk<BehandlingKlient>().also {
                coEvery {
                    it.kreverTotrinnskontroll(any(), any())
                } returns Result.success(true)
                every { it.godkjenn(behandlingId = any(), any(), any()) } returns Result.success(Unit)
                every { it.beslutt(behandlingId = any(), any(), any()) } returns Result.success(Unit)
                every { it.sendTilbake(behandlingId = any(), any(), any()) } returns Result.success(Unit)
            }

        settOppOppgaveMediator(behandlingKlient = behandlingKlientMock) { datasource, oppgaveMediator ->

            val oppgave = datasource.lagTestoppgave(tilstand = KLAR_TIL_BEHANDLING)

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

    private fun DataSource.lagTestoppgave(
        tilstand: Type = KLAR_TIL_BEHANDLING,
        behandlingId: UUID = UUIDv7.ny(),
        søknadId: UUID = UUIDv7.ny(),
    ): Oppgave {
        val personRepository = PostgresPersonRepository(this)
        val sakMediator =
            SakMediator(
                personMediator =
                    PersonMediator(
                        personRepository = personRepository,
                        oppslag = oppslagMock,
                    ),
                sakRepository = PostgresRepository(dataSource),
            ).also {
                it.setRapidsConnection(testRapid)
            }

        val oppgaveMediator =
            OppgaveMediator(
                oppgaveRepository = PostgresOppgaveRepository(this),
                behandlingKlient = behandlingKlientMock,
                oppslag = oppslagMock,
                utsendingMediator = mockk(),
                meldingOmVedtakKlient = mockk(),
                sakMediator = sakMediator,
            ).also {
                it.setRapidsConnection(testRapid)
            }

        sakMediator.opprettSak(
            søknadsbehandlingOpprettetHendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    ident = testIdent,
                    opprettet = LocalDateTime.now(),
                    utførtAv = Applikasjon(navn = ""),
                ),
        )

        VedtakFattetMottak(testRapid, oppgaveMediator)

        val oppgave =
            oppgaveMediator.opprettEllerOppdaterOppgave(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                ),
            ) ?: throw IllegalStateException("Kunne ikke opprette oppgave")

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

    private fun settOppOppgaveMediator(
        hendelse: Hendelse = TomHendelse,
        behandlingKlient: BehandlingKlient = behandlingKlientMock,
        movKlient: MeldingOmVedtakKlient = mockk(),
        test: (datasource: DataSource, oppgaveMediator: OppgaveMediator) -> Unit,
    ) {
        withMigratedDb { datasource ->
            val sakMediator =
                SakMediator(
                    personMediator =
                        PersonMediator(
                            personRepository = PostgresPersonRepository(dataSource),
                            oppslag = oppslagMock,
                        ),
                    sakRepository = PostgresRepository(dataSource),
                ).also {
                    it.setRapidsConnection(testRapid)
                }

            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = PostgresOppgaveRepository(datasource),
                    behandlingKlient = behandlingKlient,
                    oppslag = oppslagMock,
                    utsendingMediator = UtsendingMediator(PostgresUtsendingRepository(datasource)),
                    meldingOmVedtakKlient = movKlient,
                    sakMediator = sakMediator,
                ).also {
                    it.setRapidsConnection(testRapid)
                }

            if (hendelse is SøknadsbehandlingOpprettetHendelse) {
                val sak =
                    sakMediator.opprettSak(
                        søknadsbehandlingOpprettetHendelse = hendelse,
                    )
                sakMediator.knyttTilSak(
                    behandlingOpprettetHendelse =
                        BehandlingOpprettetHendelse(
                            behandlingId = hendelse.behandlingId,
                            ident = hendelse.ident,
                            sakId = sak.sakId,
                            opprettet = hendelse.opprettet,
                            type = BehandlingType.RETT_TIL_DAGPENGER,
                        ),
                )
            }

            VedtakFattetMottak(testRapid, oppgaveMediator)
            test(dataSource, oppgaveMediator)
        }
    }
}
