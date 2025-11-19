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
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class InnsendingMediatorTest {
    private val sakId = UUIDv7.ny()
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
            every { it.finnEllerOpprettPerson(DBTestHelper.testPerson.ident) } returns DBTestHelper.testPerson
        }

    @Test
    fun `Skal lage innsending behandling og oppgave dersom vi eier saken`() {
        val sak =
            Sak(
                sakId = sakId,
                søknadId = søknadIdSomSkalVarsles,
                opprettet = DBTestHelper.opprettetNå,
                behandlinger =
                    mutableSetOf(
                        Behandling(
                            behandlingId = UUIDv7.ny(),
                            opprettet = DBTestHelper.opprettetNå,
                            hendelse = TomHendelse,
                            utløstAv = UtløstAvType.SØKNAD,
                        ),
                    ),
            )
        DBTestHelper.withSak(sak = sak) {
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
                        ident = DBTestHelper.testPerson.ident,
                        sak =
                            UtsendingSak(
                                id = sakId.toString(),
                                kontekst = "Dagpenger",
                            ),
                        automatiskBehandlet = false,
                    ),
            )
            val innsendingMottattHendelse =
                InnsendingMottattHendelse(
                    ident = DBTestHelper.testPerson.ident,
                    journalpostId = journalpostId,
                    registrertTidspunkt = registrertTidspunkt,
                    søknadId = null,
                    skjemaKode = skjemaKode,
                    kategori = Kategori.KLAGE,
                )

            innsendingMediator.taImotInnsending(
                innsendingMottattHendelse,
            )
            val sakHistorikk = sakMediator.hentSakHistorikk(ident = DBTestHelper.testPerson.ident)
            sakHistorikk.finnSak { it.sakId == sak.sakId }?.let { sak ->
                sak.behandlinger().size shouldBe 2
                sak.behandlinger().first().utløstAv shouldBe UtløstAvType.INNSENDING
            } ?: fail("Sak med id ${sak.sakId} ikke funnet")

            oppgaveMediator.finnOppgaverFor(ident = DBTestHelper.testPerson.ident).size shouldBe 1
            val oppgave = oppgaveMediator.finnOppgaverFor(ident = DBTestHelper.testPerson.ident).first()
            oppgave.behandling.utløstAv shouldBe UtløstAvType.INNSENDING
            oppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
            innsendingRepository.finnInnsendingerForPerson(ident = DBTestHelper.testPerson.ident).size shouldBe 1
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
    fun `Ferdigstilling av en innsending`() {
        val slot = slot<Innsending>()
        val saksbehandler = Saksbehandler(navIdent = "saksbehandler1", emptySet())
        val innsending =
            TestHelper.lagInnsending()

        val innsendingRepository =
            mockk<InnsendingRepository>().also {
                every { it.hent(innsending.innsendingId) } returns innsending
                every { it.lagre(capture(slot)) } just Runs
            }

        val innsendingFerdigstiltHendelse =
            InnsendingFerdigstiltHendelse(
                innsendingId = innsending.innsendingId,
                aksjon = Aksjon.OpprettKlage::class.java.simpleName,
                behandlingId = UUIDv7.ny(),
                utførtAv = saksbehandler,
            )

        val ferdigstillInnsendingHendelse =
            FerdigstillInnsendingHendelse(
                innsendingId = innsending.innsendingId,
                aksjon = Aksjon.OpprettKlage(sakId),
                utførtAv = saksbehandler,
            )

        val innsendingBehandler =
            mockk<InnsendingBehandler>().also {
                every {
                    it.utførAksjon(
                        hendelse = ferdigstillInnsendingHendelse,
                        innsending = innsending,
                    )
                } returns innsendingFerdigstiltHendelse
            }

        InnsendingMediator(
            sakMediator = mockk(),
            oppgaveMediator = mockk(),
            personMediator = mockk(),
            innsendingRepository = innsendingRepository,
            innsendingBehandler = innsendingBehandler,
        ).ferdigstill(
            ferdigstillInnsendingHendelse,
        )
    }

    @Test
    fun `Avbryt innsending hvis behandling opprettes for søknad`() {
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
        ).avbrytInnsending(hendelse = behandlingOpprettetForSøknadHendelse)
    }

    @Test
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
        ).avbrytInnsending(hendelse = behandlingOpprettetForSøknadHendelse)

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
        ).avbrytInnsending(hendelse = behandlingOpprettetForSøknadHendelse)

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
        ).avbrytInnsending(hendelse = behandlingOpprettetForSøknadHendelse)

        verify(exactly = 0) { innsendingRepository.lagre(any()) }
    }
}
