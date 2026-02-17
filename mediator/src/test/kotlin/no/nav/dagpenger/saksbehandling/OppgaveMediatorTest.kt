package no.nav.dagpenger.saksbehandling

import PersonMediator
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Emneknagg.AvbrytBehandling
import no.nav.dagpenger.saksbehandling.Emneknagg.PåVent.AVVENT_MELDEKORT
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerLåsAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerOpplåsingAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.FerdigBehandlet
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilKontroll
import no.nav.dagpenger.saksbehandling.Oppgave.KontrollertBrev.IKKE_RELEVANT
import no.nav.dagpenger.saksbehandling.Oppgave.KontrollertBrev.JA
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.GOSYS
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.INGEN
import no.nav.dagpenger.saksbehandling.Oppgave.Opprettet
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.TestHelper.lagBehandling
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
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.saksbehandling.db.innsending.PostgresInnsendingRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.hendelser.AvbrytOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SlettNotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.innsending.Aksjon
import no.nav.dagpenger.saksbehandling.innsending.InnsendingMediator
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.provider.Arguments
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
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

    private val behandlingIdKreverIkkeTotrinnskontroll = UUIDv7.ny()

    private val behandlingKlientMock =
        mockk<BehandlingKlient>().also {
            every { it.godkjenn(any(), any(), any()) } returns Result.success(Unit)
            coEvery { it.kreverTotrinnskontroll(behandlingIdKreverIkkeTotrinnskontroll, any()) } returns
                Result.success(
                    false,
                )
            coEvery {
                it.kreverTotrinnskontroll(
                    not(behandlingIdKreverIkkeTotrinnskontroll),
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
                every { it.finnSakHistorikk(personUtenSakHistorikk) } returns null
                every { it.finnSakHistorikk(personUtenBehandling) } returns
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
                sakMediator = sakMediatorMock,
            ).also {
                it.setRapidsConnection(testRapid)
            }

        shouldThrow<IllegalStateException> {
            oppgaveMediator.opprettOppgaveForKlageBehandling(
                BehandlingOpprettetHendelse(
                    behandlingId = UUIDv7.ny(),
                    sakId = UUIDv7.ny(),
                    ident = personUtenSakHistorikk,
                    opprettet = LocalDateTime.now(),
                    type = UtløstAvType.KLAGE,
                ),
            )
        }
        shouldThrow<IllegalStateException> {
            oppgaveMediator.opprettOppgaveForKlageBehandling(
                BehandlingOpprettetHendelse(
                    behandlingId = UUIDv7.ny(),
                    sakId = UUIDv7.ny(),
                    ident = personUtenBehandling,
                    opprettet = LocalDateTime.now(),
                    type = UtløstAvType.KLAGE,
                ),
            )
        }

        oppgaveMediator.opprettEllerOppdaterOppgave(
            ForslagTilVedtakHendelse(
                ident = personUtenBehandling,
                behandlingId = UUIDv7.ny(),
                behandletHendelseId = UUIDv7.ny().toString(),
                behandletHendelseType = "Søknad",
            ),
        ) shouldBe null

        oppgaveMediator.opprettEllerOppdaterOppgave(
            ForslagTilVedtakHendelse(
                ident = personUtenSakHistorikk,
                behandlingId = UUIDv7.ny(),
                behandletHendelseId = UUIDv7.ny().toString(),
                behandletHendelseType = "Søknad",
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
    fun `Skal ignorere ForslagTilVedtakHendelse hvis behandlingen ikke finnes`() {
        settOppOppgaveMediator { _, oppgaveMediator ->

            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = "ad",
                    behandletHendelseId = UUIDv7.ny().toString(),
                    behandletHendelseType = "Søknad",
                    behandlingId = UUIDv7.ny(),
                )
            oppgaveMediator.opprettEllerOppdaterOppgave(forslagTilVedtakHendelse)
            oppgaveMediator.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING).size shouldBe 0
        }
    }

    @Test
    fun `Skal lagre egen hendelse med tilstand OPPRETTET når oppgave opprettes`() {
        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                utløstAv = UtløstAvType.SØKNAD,
                opprettet = LocalDateTime.now(),
                hendelse = TomHendelse,
            )
        val søknadHendelse =
            SøknadsbehandlingOpprettetHendelse(
                søknadId = UUIDv7.ny(),
                behandlingId = behandling.behandlingId,
                ident = testIdent,
                opprettet = behandling.opprettet,
                behandlingskjedeId = behandling.behandlingId,
            )
        settOppOppgaveMediator(hendelse = søknadHendelse) { _, oppgaveMediator ->
            val forventedeEmneknagger = setOf("knagg1", "knagg2")
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    behandletHendelseId = UUIDv7.ny().toString(),
                    behandletHendelseType = "Søknad",
                    behandlingId = behandling.behandlingId,
                    emneknagger = forventedeEmneknagger,
                )
            oppgaveMediator.opprettEllerOppdaterOppgave(forslagTilVedtakHendelse)
            val oppgaveId =
                oppgaveMediator.hentOppgaveIdFor(behandling.behandlingId).also {
                    it shouldNotBe null
                }
            with(oppgaveMediator.hentOppgave(oppgaveId!!, testInspektør)) {
                tilstand().type shouldBe KLAR_TIL_BEHANDLING
                tilstandslogg.first().tilstand shouldBe KLAR_TIL_BEHANDLING
                tilstandslogg.first().hendelse shouldBe forslagTilVedtakHendelse
                tilstandslogg.last().tilstand shouldBe OPPRETTET
                tilstandslogg.last().hendelse shouldBe TomHendelse
                forventedeEmneknagger shouldBe forventedeEmneknagger
            }
        }
    }

    @Test
    fun `Opprett oppgave for automatisk VedtakFattetHendelse`() {
        val behandlingId = UUIDv7.ny()
        val søknadId = UUIDv7.ny()
        val opprettet = LocalDateTime.now()

        fun lagVedtakHendelse(
            behandlingId: UUID,
            automatiskBehandlet: Boolean,
        ): VedtakFattetHendelse =
            VedtakFattetHendelse(
                behandlingId = behandlingId,
                behandletHendelseId = søknadId.toString(),
                behandletHendelseType = "Søknad",
                ident = testIdent,
                sak = null,
                automatiskBehandlet = automatiskBehandlet,
            )

        settOppOppgaveMediator(
            hendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    ident = testIdent,
                    opprettet = opprettet,
                    behandlingskjedeId = behandlingId,
                ),
        ) { _, oppgaveMediator ->

            oppgaveMediator.hentOppgaveIdFor(behandlingId) shouldBe null

            shouldThrowWithMessage<IllegalArgumentException>(
                "Mottatt manuell VedtakFattetHendelse uten tilhørende oppgave for " +
                    "behandlingId $behandlingId. " +
                    "Oppgave skal alltid eksistere for manuelle vedtak.",
            ) {
                oppgaveMediator.håndter(
                    vedtakFattetHendelse =
                        lagVedtakHendelse(
                            behandlingId = behandlingId,
                            automatiskBehandlet = false,
                        ),
                    emneknagger = emneknagger,
                )
            }

            val automatiskVedtakfattetHendelse =
                lagVedtakHendelse(
                    behandlingId = behandlingId,
                    automatiskBehandlet = true,
                )

            val forventetEmneKnagger = setOf("e1", "e2")
            oppgaveMediator.håndter(
                vedtakFattetHendelse = automatiskVedtakfattetHendelse,
                emneknagger = forventetEmneKnagger,
            )

            val oppgaveId =
                oppgaveMediator.hentOppgaveIdFor(behandlingId).also {
                    it shouldNotBe null
                }
            with(oppgaveMediator.hentOppgave(oppgaveId!!, testInspektør)) {
                tilstand().type shouldBe FERDIG_BEHANDLET
                tilstandslogg.first().tilstand shouldBe FERDIG_BEHANDLET
                tilstandslogg.first().hendelse shouldBe automatiskVedtakfattetHendelse
                tilstandslogg.last().tilstand shouldBe OPPRETTET
                tilstandslogg.last().hendelse shouldBe TomHendelse
                emneknagger shouldBe forventetEmneKnagger
            }
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
                    behandletHendelseId = UUIDv7.ny().toString(),
                    behandletHendelseType = "Søknad",
                    behandlingId = oppgave.behandling.behandlingId,
                    emneknagger = testEmneknagger1,
                ),
            )

            oppgaveMediator.opprettEllerOppdaterOppgave(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    behandletHendelseId = UUIDv7.ny().toString(),
                    behandletHendelseType = "Søknad",
                    behandlingId = oppgave.behandling.behandlingId,
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
        private fun oppgaveTilstandForSøknad(): Stream<Arguments> =
            Stream.of(
                Arguments.of(Opprettet, false),
                Arguments.of(KlarTilBehandling, false),
                Arguments.of(AvventerOpplåsingAvBehandling, false),
                Arguments.of(AvventerLåsAvBehandling, false),
                Arguments.of(Oppgave.PåVent, true),
                Arguments.of(UnderBehandling, true),
                Arguments.of(KlarTilKontroll, true),
                Arguments.of(Oppgave.UnderKontroll(), true),
                Arguments.of(FerdigBehandlet, false),
            )
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir vedtatt med vedtaksbrev uten totrinnskontroll`() {
        val behandlingId = UUIDv7.ny()

        settOppOppgaveMediator { datasource, oppgaveMediator ->
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
                    saksbehandler = saksbehandler,
                    saksbehandlerToken = "token",
                )
            }

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            ferdigbehandletOppgave.kontrollertBrev() shouldBe IKKE_RELEVANT
            ferdigbehandletOppgave.tilstand().type shouldBe FERDIG_BEHANDLET
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir vedtatt med vedtaksbrev i Gosys uten totrinnskontroll`() {
        val behandlingId = UUIDv7.ny()

        settOppOppgaveMediator { datasource, oppgaveMediator ->
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
            tildeltOppgave.meldingOmVedtakKilde() shouldBe DP_SAK
            tildeltOppgave.kontrollertBrev() shouldBe IKKE_RELEVANT

            oppgaveMediator.endreMeldingOmVedtakKilde(
                oppgaveId = tildeltOppgave.oppgaveId,
                meldingOmVedtakKilde = GOSYS,
                saksbehandler = saksbehandler,
            )
            runBlocking {
                oppgaveMediator.ferdigstillOppgave(
                    oppgaveId = oppgave.oppgaveId,
                    saksbehandler = saksbehandler,
                    saksbehandlerToken = "token",
                )
            }

            val ferdigBehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            ferdigBehandletOppgave.tilstand().type shouldBe FERDIG_BEHANDLET
            ferdigBehandletOppgave.meldingOmVedtakKilde() shouldBe GOSYS
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir vedtatt uten vedtaksbrev`() {
        val behandlingId = UUIDv7.ny()

        settOppOppgaveMediator { datasource, oppgaveMediator ->
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
            tildeltOppgave.meldingOmVedtakKilde() shouldBe DP_SAK

            oppgaveMediator.endreMeldingOmVedtakKilde(
                oppgaveId = tildeltOppgave.oppgaveId,
                meldingOmVedtakKilde = INGEN,
                saksbehandler = saksbehandler,
            )
            runBlocking {
                oppgaveMediator.ferdigstillOppgave(
                    oppgaveId = oppgave.oppgaveId,
                    saksbehandler = saksbehandler,
                    saksbehandlerToken = "token",
                )
            }

            val ferdigBehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            ferdigBehandletOppgave.tilstand().type shouldBe FERDIG_BEHANDLET
            ferdigBehandletOppgave.meldingOmVedtakKilde() shouldBe INGEN
        }
    }

    @Test
    fun `Livssyklus for oppgave ferdigstilles med melding om vedtak fra saksbehandler`() {
        val behandlingId = UUIDv7.ny()
        val søknadId = UUIDv7.ny()
        val saksbehandlerToken = "token"
        val sakMediatorMock =
            mockk<SakMediator>().also {
                every { it.hentSakIdForBehandlingId(any()) } returns UUIDv7.ny()
            }

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
            val utsendingMediator =
                UtsendingMediator(
                    utsendingRepository = PostgresUtsendingRepository(datasource),
                    brevProdusent = mockk(),
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
            tildeltOppgave.meldingOmVedtakKilde() shouldBe DP_SAK

            oppgaveMediator.ferdigstillOppgave(
                oppgaveId = oppgave.oppgaveId,
                saksbehandler = saksbehandler,
                saksbehandlerToken = "token",
            )

            verify(exactly = 1) {
                behandlingClientMock.godkjenn(behandlingId, testIdent, saksbehandlerToken)
            }

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            ferdigbehandletOppgave.tilstand().type shouldBe FERDIG_BEHANDLET
            ferdigbehandletOppgave.meldingOmVedtakKilde() shouldBe DP_SAK

            val utsending =
                utsendingMediator.hentUtsendingForBehandlingId(ferdigbehandletOppgave.behandling.behandlingId)
            utsending.behandlingId shouldBe ferdigbehandletOppgave.behandling.behandlingId
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
                    behandletHendelseId = UUIDv7.ny().toString(),
                    behandletHendelseType = "Søknad",
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

            shouldThrow<BehandlingException> {
                oppgaveMediator.ferdigstillOppgave(
                    oppgaveId = oppgave.oppgaveId,
                    saksbehandler = saksbehandler,
                    saksbehandlerToken = "token",
                )
            }

            verify(exactly = 1) {
                behandlingClientMock.godkjenn(behandlingId, testIdent, saksbehandlerToken)
            }

            val ferdigbehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            ferdigbehandletOppgave.tilstand().type shouldBe UNDER_BEHANDLING

            UtsendingMediator(
                utsendingRepository = PostgresUtsendingRepository(datasource),
                brevProdusent = mockk(),
            ).also { utsendingMediator ->
                utsendingMediator.finnUtsendingForBehandlingId(ferdigbehandletOppgave.behandling.behandlingId) shouldBe null
            }
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
                    oppgaveId = oppgave.oppgaveId,
                    saksbehandler = saksbehandler,
                    saksbehandlerToken = "token",
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
    fun `Livssyklus for søknadsbehandling som blir avbrutt fra regelmotor`() {
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
                    behandlingId = oppgave.behandling.behandlingId,
                    behandletHendelseId = oppgave.søknadId()!!.toString(),
                    behandletHendelseType = "Søknad",
                    ident = testIdent,
                ),
            )

            oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør).tilstand().type shouldBe AVBRUTT
        }
    }

    @Test
    fun `Håndtering av ettersending for søknad oppgaver`() {
        val behandlingId = UUIDv7.ny()
        val søknadId = behandlingId
        settOppOppgaveMediator(
            hendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    ident = testIdent,
                    opprettet = LocalDateTime.now(),
                    behandlingskjedeId = behandlingId,
                ),
        ) { _, oppgaveMediator ->

            oppgaveMediator.opprettEllerOppdaterOppgave(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    behandletHendelseId = søknadId.toString(),
                    behandletHendelseType = "Søknad",
                    behandlingId = behandlingId,
                    emneknagger = emneknagger,
                ),
            )

            val søknadOppgave = oppgaveMediator.finnOppgaverFor(testIdent).single()

            val ettersendingSomManglerSøknadId =
                InnsendingMottattHendelse(
                    ident = søknadOppgave.personIdent(),
                    journalpostId = "asdf",
                    registrertTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                    søknadId = null,
                    skjemaKode = "NAV 11-12.05",
                    kategori = Kategori.ETTERSENDING,
                )
            oppgaveMediator.taImotEttersending(ettersendingSomManglerSøknadId)
            oppgaveMediator.hentOppgave(søknadOppgave.oppgaveId, testInspektør) shouldBe søknadOppgave

            val klage =
                InnsendingMottattHendelse(
                    ident = søknadOppgave.personIdent(),
                    journalpostId = "asdf",
                    registrertTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                    søknadId = søknadOppgave.søknadId(),
                    skjemaKode = "NAV 11-12.05",
                    kategori = Kategori.KLAGE,
                )
            oppgaveMediator.taImotEttersending(klage)
            oppgaveMediator.hentOppgave(søknadOppgave.oppgaveId, testInspektør) shouldBe søknadOppgave

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = søknadOppgave.oppgaveId,
                    ansvarligIdent = testInspektør.navIdent,
                    utførtAv = testInspektør,
                ),
            )

            oppgaveMediator.utsettOppgave(
                UtsettOppgaveHendelse(
                    oppgaveId = søknadOppgave.oppgaveId,
                    navIdent = testInspektør.navIdent,
                    utsattTil = LocalDate.now().plusDays(1),
                    beholdOppgave = false,
                    utførtAv = testInspektør,
                ),
            )

            oppgaveMediator.taImotEttersending(
                ettersendingSomManglerSøknadId.copy(
                    søknadId = søknadOppgave.søknadId(),
                    kategori = Kategori.ETTERSENDING,
                ),
            )

            oppgaveMediator.hentOppgave(søknadOppgave.oppgaveId, testInspektør).let { actual ->
                actual shouldNotBe søknadOppgave
                actual.emneknagger shouldContain ("Ettersending(${LocalDate.now()})")
                actual.tilstand().type shouldBe KLAR_TIL_BEHANDLING
            }
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling som blir avbrutt av saksbehandler`() {
        settOppOppgaveMediator { datasource, oppgaveMediator ->

            val oppgave = datasource.lagTestoppgave()
            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            val avbrytOppgaveHendelse =
                AvbrytOppgaveHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    årsak = AvbrytBehandling.AVBRUTT_BEHANDLES_I_ARENA,
                    navIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                )
            oppgaveMediator.avbryt(avbrytOppgaveHendelse)

            val avbruttOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)

            avbruttOppgave.tilstand().type shouldBe AVBRUTT
            avbruttOppgave.tilstandslogg.first().tilstand shouldBe AVBRUTT
            avbruttOppgave.emneknagger.contains(AvbrytBehandling.AVBRUTT_BEHANDLES_I_ARENA.visningsnavn)
            avbruttOppgave.behandlerIdent shouldBe saksbehandler.navIdent
            testRapid.inspektør.size shouldBe 1
            testRapid.inspektør.message(0).let { message ->
                message["@event_name"].asText() shouldBe "avbryt_behandling"
                message["behandlingId"].asText() shouldBe oppgave.behandling.behandlingId.toString()
                message["ident"].asText() shouldBe oppgave.personIdent()
                message["årsak"].asText() shouldBe avbrytOppgaveHendelse.årsak.visningsnavn
            }
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

            oppgaveMediator
                .hentAlleOppgaverMedTilstand(PAA_VENT)
                .single()
                .emneknagger shouldContain AVVENT_MELDEKORT.visningsnavn
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
    fun `Livssyklus for søknadsbehandling med brev i dp-sak som krever totrinnskontroll`() {
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
                oppgaveId = oppgave.oppgaveId,
                saksbehandler = beslutter,
                saksbehandlerToken = "token",
            )
        }
    }

    @Test
    fun `Livssyklus for søknadsbehandling med brev i gosys som krever totrinnskontroll`() {
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

            oppgaveMediator.endreMeldingOmVedtakKilde(
                oppgaveId = oppgave.oppgaveId,
                meldingOmVedtakKilde = GOSYS,
                saksbehandler = saksbehandler,
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

            oppgaveMediator.lagreKontrollertBrev(
                oppgaveId = oppgave.oppgaveId,
                kontrollertBrev = JA,
                saksbehandler = beslutter,
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

            shouldThrow<Oppgave.Tilstand.KreverKontrollAvGosysBrev> {
                oppgaveMediator.ferdigstillOppgave(
                    oppgaveId = oppgave.oppgaveId,
                    saksbehandler = beslutter,
                    saksbehandlerToken = "token",
                )
            }

            oppgaveMediator.lagreKontrollertBrev(
                oppgaveId = oppgave.oppgaveId,
                kontrollertBrev = JA,
                saksbehandler = beslutter,
            )

            oppgaveMediator.ferdigstillOppgave(
                oppgaveId = oppgave.oppgaveId,
                saksbehandler = beslutter,
                saksbehandlerToken = "token",
            )
        }
    }

    @Test
    fun `Livssyklus for behandling av klage som ferdigstilles`() {
        val behandling = lagBehandling(utløstAvType = UtløstAvType.KLAGE)
        val oppgave =
            TestHelper.lagOppgave(
                tilstand = UnderBehandling,
                behandling = behandling,
                saksbehandlerIdent = saksbehandler.navIdent,
            )
        DBTestHelper.withOppgave(oppgave) { ds ->
            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = PostgresOppgaveRepository(ds),
                    behandlingKlient = mockk(),
                    utsendingMediator = mockk(),
                    sakMediator = mockk(),
                )

            oppgaveMediator
                .ferdigstillOppgave(
                    behandlingId = oppgave.behandling.behandlingId,
                    saksbehandler = saksbehandler,
                ).getOrThrow() shouldBe oppgave.oppgaveId

            val ferdigBehandletOppgave = oppgaveMediator.hentOppgave(oppgave.oppgaveId, testInspektør)
            ferdigBehandletOppgave.tilstand() shouldBe FerdigBehandlet
            ferdigBehandletOppgave.tilstandslogg.first().hendelse shouldBe
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = saksbehandler,
                )
        }
    }

    @Test
    fun `Livssyklus for behandling av innsending som ferdigstilles`() {
        val testPerson = DBTestHelper.testPerson
        val behandlingskjedeId = UUIDv7.ny()
        val sakId = behandlingskjedeId
        val søknadId = UUIDv7.ny()
        val behandlingIdSøknad = UUIDv7.ny()
        val journalpostId = "journalpostId123"
        val registrertTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        val personMediatorMock: PersonMediator =
            mockk<PersonMediator>().also {
                every { it.finnEllerOpprettPerson(testPerson.ident) } returns testPerson
            }
        val sak =
            Sak(
                sakId = behandlingskjedeId,
                søknadId = søknadId,
                opprettet = DBTestHelper.opprettetNå,
                behandlinger = mutableSetOf(),
            )
        DBTestHelper.withMigratedDb {
            val behandling =
                Behandling(
                    behandlingId = behandlingIdSøknad,
                    opprettet = DBTestHelper.opprettetNå,
                    hendelse =
                        SøknadsbehandlingOpprettetHendelse(
                            søknadId = søknadId,
                            behandlingId = behandlingIdSøknad,
                            ident = testPerson.ident,
                            opprettet = DBTestHelper.opprettetNå,
                            behandlingskjedeId = behandlingskjedeId,
                        ),
                    utløstAv = UtløstAvType.SØKNAD,
                )
            opprettSakMedBehandlingOgOppgave(
                person = testPerson,
                sak = sak,
                behandling = behandling,
                oppgave =
                    TestHelper.lagOppgave(
                        person = testPerson,
                        behandling = behandling,
                        tilstand = KlarTilKontroll,
                    ),
            )
            val sakMediator =
                SakMediator(
                    personMediator = personMediatorMock,
                    sakRepository = PostgresSakRepository(it),
                )
            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = PostgresOppgaveRepository(it),
                    behandlingKlient = mockk(),
                    utsendingMediator = mockk(),
                    sakMediator = sakMediator,
                )
            val innsendingRepository = PostgresInnsendingRepository(it)
            val innsendingMediator =
                InnsendingMediator(
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                    personMediator = personMediatorMock,
                    innsendingRepository = innsendingRepository,
                    innsendingBehandler = mockk(),
                )
            sakMediator.merkSakenSomDpSak(
                vedtakFattetHendelse =
                    VedtakFattetHendelse(
                        behandlingId = sak.behandlinger().first().behandlingId,
                        behandletHendelseId = sak.søknadId.toString(),
                        behandletHendelseType = "Søknad",
                        ident = testPerson.ident,
                        sak =
                            UtsendingSak(
                                id = sakId.toString(),
                                kontekst = "Dagpenger",
                            ),
                        automatiskBehandlet = false,
                    ),
            )
            innsendingMediator.taImotInnsending(
                InnsendingMottattHendelse(
                    ident = testPerson.ident,
                    journalpostId = journalpostId,
                    registrertTidspunkt = registrertTidspunkt,
                    søknadId = null,
                    skjemaKode = "SkjemaKode123",
                    kategori = Kategori.KLAGE,
                ),
            )
            oppgaveMediator.finnOppgaverFor(ident = testPerson.ident).size shouldBe 2
            val innsendingOppgave =
                oppgaveMediator.finnOppgaverFor(ident = testPerson.ident).single { oppgave ->
                    oppgave.behandling.utløstAv == UtløstAvType.INNSENDING
                }
            innsendingOppgave.tilstand() shouldBe KlarTilBehandling

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = innsendingOppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            oppgaveMediator
                .finnOppgaverFor(ident = testPerson.ident)
                .single { oppgave ->
                    oppgave.behandling.utløstAv == UtløstAvType.INNSENDING
                }.tilstand() shouldBe UnderBehandling

            oppgaveMediator.ferdigstillOppgave(
                InnsendingFerdigstiltHendelse(
                    innsendingId = innsendingOppgave.behandling.behandlingId,
                    aksjonType = Aksjon.Type.OPPRETT_KLAGE,
                    opprettetBehandlingId = UUIDv7.ny(),
                    utførtAv = saksbehandler,
                ),
            )
            oppgaveMediator
                .finnOppgaverFor(ident = testPerson.ident)
                .single { oppgave ->
                    oppgave.behandling.utløstAv == UtløstAvType.INNSENDING
                }.tilstand() shouldBe FerdigBehandlet
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
                sakRepository = PostgresSakRepository(dataSource),
            ).also {
                it.setRapidsConnection(testRapid)
            }

        val oppgaveMediator =
            OppgaveMediator(
                oppgaveRepository = PostgresOppgaveRepository(this),
                behandlingKlient = behandlingKlientMock,
                utsendingMediator = mockk(),
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
                    behandlingskjedeId = UUIDv7.ny(),
                    utførtAv = Applikasjon(navn = ""),
                ),
        )

        val oppgave =
            oppgaveMediator.opprettEllerOppdaterOppgave(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    behandletHendelseId = UUIDv7.ny().toString(),
                    behandletHendelseType = "Søknad",
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
                    sakRepository = PostgresSakRepository(dataSource),
                ).also {
                    it.setRapidsConnection(testRapid)
                }

            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = PostgresOppgaveRepository(datasource),
                    behandlingKlient = behandlingKlient,
                    utsendingMediator =
                        UtsendingMediator(
                            utsendingRepository = PostgresUtsendingRepository(datasource),
                            brevProdusent = mockk(),
                        ),
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
                            type = UtløstAvType.SØKNAD,
                        ),
                )
            }

            test(dataSource, oppgaveMediator)
        }
    }
}
