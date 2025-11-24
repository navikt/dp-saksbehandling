package no.nav.dagpenger.saksbehandling.innsending

import PersonMediator
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.innsending.InnsendingRepository
import no.nav.dagpenger.saksbehandling.db.innsending.PostgresInnsendingRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class InnsendingMediatorTest {
    private val testPerson = DBTestHelper.testPerson
    private val sakId = UUIDv7.ny()
    private val søknadId = UUIDv7.ny()
    private val behandlingIdSøknad = UUIDv7.ny()
    private val journalpostId = "journalpostId123"
    private val søknadIdSomSkalVarsles = UUIDv7.ny()
    private val søknadIdSomIkkeSkalVarsles = UUIDv7.ny()
    private val registrertTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    private val personMedSak =
        Person(
            id = UUIDv7.ny(),
            ident = "44444422222",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        )
    private val personUtenSak =
        Person(
            id = UUIDv7.ny(),
            ident = "11111122222",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        )
    private val skjemaKode = "NAVe"
    private val sakMediatorMock: SakMediator =
        mockk<SakMediator>(relaxed = true).also {
            coEvery { it.finnSisteSakId(personMedSak.ident) } returns sakId
            coEvery { it.finnSisteSakId(personUtenSak.ident) } returns null
        }
    private val oppgaveMediatorMock =
        mockk<OppgaveMediator>().also {
            coEvery { it.skalEttersendingTilSøknadVarsles(søknadIdSomSkalVarsles, any()) } returns true
            coEvery { it.skalEttersendingTilSøknadVarsles(søknadIdSomIkkeSkalVarsles, any()) } returns false
            coEvery { it.lagOppgaveForInnsendingBehandling(any(), any(), any()) } just Runs
        }

    private val personMediatorMock: PersonMediator =
        mockk<PersonMediator>().also {
            every { it.finnEllerOpprettPerson(personMedSak.ident) } returns personMedSak
            every { it.finnEllerOpprettPerson(personUtenSak.ident) } returns personUtenSak
            every { it.finnEllerOpprettPerson(testPerson.ident) } returns testPerson
        }

    @Test
    fun `Skal lage innsending behandling og oppgave dersom vi eier saken`() {
        val sak =
            Sak(
                sakId = sakId,
                søknadId = søknadId,
                opprettet = DBTestHelper.opprettetNå,
                behandlinger =
                    mutableSetOf(
                        Behandling(
                            behandlingId = behandlingIdSøknad,
                            opprettet = DBTestHelper.opprettetNå,
                            hendelse =
                                SøknadsbehandlingOpprettetHendelse(
                                    søknadId = søknadId,
                                    behandlingId = behandlingIdSøknad,
                                    ident = testPerson.ident,
                                    opprettet = DBTestHelper.opprettetNå,
                                ),
                            utløstAv = UtløstAvType.SØKNAD,
                        ),
                    ),
            )

        val saksbehandler =
            Saksbehandler(
                navIdent = "saksbehandler1",
                emptySet(),
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
                        tilstand = Oppgave.FerdigBehandlet,
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
                    innsendingBehandler =
                        mockk<InnsendingBehandler>().also {
                            coEvery {
                                it.utførAksjon(any(), any())
                            } returns
                                InnsendingFerdigstiltHendelse(
                                    innsendingId = UUIDv7.ny(),
                                    aksjonType = Aksjon.Type.AVSLUTT,
                                    opprettetBehandlingId = null,
                                    utførtAv = saksbehandler,
                                )
                        },
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
                    skjemaKode = skjemaKode,
                    kategori = Kategori.KLAGE,
                ),
            )
            val sakHistorikk = sakMediator.hentSakHistorikk(ident = testPerson.ident)
            sakHistorikk.finnSak { it.sakId == sak.sakId }?.let { sak ->
                sak.behandlinger().size shouldBe 2
                sak.behandlinger().first().utløstAv shouldBe UtløstAvType.INNSENDING
            } ?: fail("Sak med id ${sak.sakId} ikke funnet")

            oppgaveMediator.finnOppgaverFor(ident = testPerson.ident).size shouldBe 2
            val innsendingOppgave =
                oppgaveMediator.finnOppgaverFor(ident = testPerson.ident).single {
                    it.behandling.utløstAv == UtløstAvType.INNSENDING
                }
            innsendingOppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
            val innsending =
                innsendingRepository.finnInnsendingerForPerson(ident = testPerson.ident).let { innsendinger ->
                    innsendinger.size shouldBe 1
                    innsendinger.first()
                }

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = innsendingOppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            InnsendingMediator(
                sakMediator = sakMediator,
                oppgaveMediator = oppgaveMediator,
                personMediator = personMediatorMock,
                innsendingRepository = innsendingRepository,
                innsendingBehandler =
                    mockk<InnsendingBehandler>().also {
                        every { it.utførAksjon(any(), any()) } returns
                            InnsendingFerdigstiltHendelse(
                                innsendingId = innsending.innsendingId,
                                aksjonType = Aksjon.Type.AVSLUTT,
                                opprettetBehandlingId = null,
                                utførtAv = saksbehandler,
                            )
                    },
            ).let { mediator ->
                mediator.ferdigstill(
                    hendelse =
                        FerdigstillInnsendingHendelse(
                            innsendingId = innsendingOppgave.behandling.behandlingId,
                            aksjon = Aksjon.Avslutt(sak.sakId),
                            vurdering = "Vurderingstekst",
                            utførtAv = saksbehandler,
                        ),
                )
            }

            oppgaveMediator.hentOppgave(innsendingOppgave.oppgaveId, saksbehandler).let { oppgave ->
                oppgave.tilstand() shouldBe Oppgave.FerdigBehandlet
            }

            innsendingMediator.hentInnsending(innsending.innsendingId, saksbehandler).let { innsending ->
                innsending.innsendingResultat().let { resultat ->
                    resultat shouldBe Innsending.InnsendingResultat.Ingen
                }
                innsending.valgtSakId() shouldBe sak.sakId
            }
        }
    }

    @Test
    fun `Skal lage innsending behandling og oppgave dersom vi eier saken for ettersending som skal varsles`() {
        val sak =
            Sak(
                sakId = sakId,
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
                        tilstand = Oppgave.KlarTilKontroll,
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

            val innsendingMottattHendelse =
                InnsendingMottattHendelse(
                    ident = testPerson.ident,
                    journalpostId = journalpostId,
                    registrertTidspunkt = registrertTidspunkt,
                    søknadId = søknadId,
                    skjemaKode = skjemaKode,
                    kategori = Kategori.ETTERSENDING,
                )

            innsendingMediator.taImotInnsending(
                innsendingMottattHendelse,
            )
            val sakHistorikk = sakMediator.hentSakHistorikk(ident = testPerson.ident)
            sakHistorikk.finnSak { it.sakId == sak.sakId }?.let { sak ->
                sak.behandlinger().size shouldBe 2
                sak.behandlinger().first().utløstAv shouldBe UtløstAvType.INNSENDING
            } ?: fail("Sak med id ${sak.sakId} ikke funnet")

            oppgaveMediator.finnOppgaverFor(ident = testPerson.ident).size shouldBe 2
            val innsendingOppgave =
                oppgaveMediator.finnOppgaverFor(ident = testPerson.ident).single {
                    it.behandling.utløstAv == UtløstAvType.INNSENDING
                }
            innsendingOppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
            innsendingRepository.finnInnsendingerForPerson(ident = testPerson.ident).size shouldBe 1
        }
    }

    @Test
    fun `Skal ikke håndtere innsending hvis vi ikke eier saken`() {
        val innsendingRepository = mockk<InnsendingRepository>(relaxed = false)
        val innsendingBehandler = mockk<InnsendingBehandler>()
        val mediator =
            InnsendingMediator(
                sakMediator = sakMediatorMock,
                oppgaveMediator = oppgaveMediatorMock,
                personMediator = personMediatorMock,
                innsendingRepository = innsendingRepository,
                innsendingBehandler = innsendingBehandler,
            )

        val innsendingMottattHendelse =
            InnsendingMottattHendelse(
                ident = personUtenSak.ident,
                journalpostId = journalpostId,
                registrertTidspunkt = registrertTidspunkt,
                søknadId = null,
                skjemaKode = skjemaKode,
                kategori = Kategori.KLAGE,
            )

        mediator.taImotInnsending(
            innsendingMottattHendelse,
        ) shouldBe HåndterInnsendingResultat.UhåndtertInnsending

        verify(exactly = 0) {
            innsendingRepository.lagre(any())
        }
        verify(exactly = 0) {
            innsendingBehandler.utførAksjon(any(), any())
        }
    }

    @Test
    fun `Skal lage innsending hvis vi ikke eier saken men skal varsle om ettersending`() {
        val slot = slot<Innsending>()
        val innsendingRepositoryMock: InnsendingRepository =
            mockk<InnsendingRepository>().also {
                every { it.lagre(capture(slot)) } just Runs
            }
        val mediator =
            InnsendingMediator(
                sakMediator = sakMediatorMock,
                oppgaveMediator = oppgaveMediatorMock,
                personMediator = personMediatorMock,
                innsendingRepository = innsendingRepositoryMock,
                innsendingBehandler = mockk(),
            )

        val innsendingMottattHendelse =
            InnsendingMottattHendelse(
                ident = personUtenSak.ident,
                journalpostId = journalpostId,
                registrertTidspunkt = registrertTidspunkt,
                søknadId = søknadIdSomSkalVarsles,
                skjemaKode = skjemaKode,
                kategori = Kategori.ETTERSENDING,
            )

        mediator.taImotInnsending(
            innsendingMottattHendelse,
        ) shouldBe HåndterInnsendingResultat.UhåndtertInnsending

        slot.captured.let {
            it.person shouldBe personUtenSak
            it.journalpostId shouldBe journalpostId
            it.mottatt shouldBe registrertTidspunkt
            it.skjemaKode shouldBe skjemaKode
            it.kategori shouldBe Kategori.ETTERSENDING
        }
    }

    @Test
    fun `Avbryt innsending hvis behandling opprettes for søknad`() {
        val søknadIdGjenopptak = UUIDv7.ny()
        val sak =
            Sak(
                sakId = UUIDv7.ny(),
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
                        tilstand = Oppgave.FerdigBehandlet,
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
                                id = sak.sakId.toString(),
                                kontekst = "Dagpenger",
                            ),
                        automatiskBehandlet = false,
                    ),
            )
            val innsendingMottattHendelse =
                InnsendingMottattHendelse(
                    ident = testPerson.ident,
                    journalpostId = journalpostId,
                    registrertTidspunkt = registrertTidspunkt,
                    søknadId = søknadIdGjenopptak,
                    skjemaKode = skjemaKode,
                    kategori = Kategori.GJENOPPTAK,
                )

            innsendingMediator.taImotInnsending(
                innsendingMottattHendelse,
            )
            val sakHistorikk = sakMediator.hentSakHistorikk(ident = testPerson.ident)
            sakHistorikk.finnSak { it.sakId == sak.sakId }?.let { sak ->
                sak.behandlinger().size shouldBe 2
                sak.behandlinger().first().utløstAv shouldBe UtløstAvType.INNSENDING
            } ?: fail("Sak med id ${sak.sakId} ikke funnet")

            val oppgaveFørAvbryt =
                oppgaveMediator.finnAlleOppgaverFor(ident = testPerson.ident).single {
                    it.behandling.utløstAv == UtløstAvType.INNSENDING
                }
            oppgaveFørAvbryt.tilstand() shouldBe Oppgave.Opprettet
            innsendingRepository.finnInnsendingerForPerson(ident = testPerson.ident).size shouldBe 1

            innsendingMediator.automatiskFerdigstill(
                hendelse =
                    BehandlingOpprettetForSøknadHendelse(
                        ident = testPerson.ident,
                        søknadId = søknadIdGjenopptak,
                        behandlingId = behandlingIdSøknad,
                    ),
            )
            val oppgaveEtterAvbryt =
                oppgaveMediator.finnAlleOppgaverFor(ident = testPerson.ident).single {
                    it.behandling.utløstAv == UtløstAvType.INNSENDING
                }
            oppgaveEtterAvbryt.tilstand() shouldBe Oppgave.Avbrutt
            val innsendingEtterAvbryt = innsendingRepository.finnInnsendingerForPerson(ident = testPerson.ident).single()
            innsendingEtterAvbryt.innsendingResultat() shouldBe Innsending.InnsendingResultat.RettTilDagpenger(behandlingIdSøknad)
            innsendingEtterAvbryt.tilstand() shouldBe "FERDIGSTILT"
        }
    }

    @Test
    @Disabled("Skal fikses senere")
    fun `Ikke avbryt innsending hvis behandling opprettes for søknad og innsendingen er under behandling`() {
        val slot = slot<Innsending>()
        val person = TestHelper.testPerson
        val søknadId = UUIDv7.ny()
        val innsending =
            TestHelper.lagInnsending(
                person = person,
                kategori = Kategori.NY_SØKNAD,
            )

        val innsendingRepository =
            mockk<InnsendingRepository>().also {
                every { it.lagre(capture(slot)) } just Runs
            }

        val behandlingOpprettetForSøknadHendelse =
            BehandlingOpprettetForSøknadHendelse(
                ident = innsending.person.ident,
                søknadId = søknadId,
                behandlingId = UUIDv7.ny(),
            )

        InnsendingMediator(
            sakMediator = mockk(),
            oppgaveMediator = mockk(),
            personMediator = mockk(),
            innsendingRepository = innsendingRepository,
            innsendingBehandler = mockk(),
        ).automatiskFerdigstill(hendelse = behandlingOpprettetForSøknadHendelse)

        verify(exactly = 0) { innsendingRepository.lagre(any()) }
    }

    @Test
    fun `Ikke avbryt innsending hvis behandling opprettes for søknad og innsendingen er ferdigbehandlet`() {
        val slot = slot<Innsending>()
        val person = TestHelper.testPerson
        val søknadId = UUIDv7.ny()
        val innsending =
            TestHelper.lagInnsending(
                person = person,
                kategori = Kategori.NY_SØKNAD,
            )

        val innsendingRepository =
            mockk<InnsendingRepository>().also {
                every { it.hent(innsending.innsendingId) } returns innsending
                every { it.lagre(capture(slot)) } just Runs
                every { it.finnInnsendingerForPerson(person.ident) } returns listOf(innsending)
            }

        val behandlingOpprettetForSøknadHendelse =
            BehandlingOpprettetForSøknadHendelse(
                ident = innsending.person.ident,
                søknadId = søknadId,
                behandlingId = UUIDv7.ny(),
            )

        InnsendingMediator(
            sakMediator = mockk(),
            oppgaveMediator = mockk(),
            personMediator = mockk(),
            innsendingRepository = innsendingRepository,
            innsendingBehandler = mockk(),
        ).automatiskFerdigstill(hendelse = behandlingOpprettetForSøknadHendelse)

        verify(exactly = 0) { innsendingRepository.lagre(any()) }
    }

    @Test
    fun `Ikke avbryt innsending hvis behandling opprettes for søknad og innsendingen mangler søknadId`() {
        val slot = slot<Innsending>()
        val person = TestHelper.testPerson
        val søknadId = UUIDv7.ny()
        val innsending =
            TestHelper.lagInnsending(
                person = person,
                kategori = Kategori.NY_SØKNAD,
            ).also {
            }

        val innsendingRepository =
            mockk<InnsendingRepository>().also {
                every { it.hent(innsending.innsendingId) } returns innsending
                every { it.lagre(capture(slot)) } just Runs
                every { it.finnInnsendingerForPerson(person.ident) } returns listOf(innsending)
            }

        val behandlingOpprettetForSøknadHendelse =
            BehandlingOpprettetForSøknadHendelse(
                ident = innsending.person.ident,
                søknadId = søknadId,
                behandlingId = UUIDv7.ny(),
            )

        InnsendingMediator(
            sakMediator = mockk(),
            oppgaveMediator = mockk(),
            personMediator = mockk(),
            innsendingRepository = innsendingRepository,
            innsendingBehandler = mockk(),
        ).automatiskFerdigstill(hendelse = behandlingOpprettetForSøknadHendelse)

        verify(exactly = 0) { innsendingRepository.lagre(any()) }
    }

    private fun OppgaveMediator.finnAlleOppgaverFor(ident: String): List<Oppgave> {
        return this.søk(
            Søkefilter(
                periode = Periode.UBEGRENSET_PERIODE,
                tilstander = Oppgave.Tilstand.Type.values,
            ),
        ).oppgaver
    }
}
