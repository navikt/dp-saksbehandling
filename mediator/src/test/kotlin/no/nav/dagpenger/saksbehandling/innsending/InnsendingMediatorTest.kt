package no.nav.dagpenger.saksbehandling.innsending

import PersonMediator
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TestHelper.saksbehandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.innsending.InnsendingRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.Avbrutt
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.Ferdigbehandlet
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.Type.FERDIGBEHANDLET
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.UnderBehandling
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
        }

    private val personMediatorMock: PersonMediator =
        mockk<PersonMediator>().also {
            every { it.finnEllerOpprettPerson(personMedSak.ident) } returns personMedSak
            every { it.finnEllerOpprettPerson(personUtenSak.ident) } returns personUtenSak
        }

    @Test
    fun `Skal lage innsending dersom vi eier saken`() {
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
                ident = personMedSak.ident,
                journalpostId = journalpostId,
                registrertTidspunkt = registrertTidspunkt,
                søknadId = null,
                skjemaKode = skjemaKode,
                kategori = Kategori.KLAGE,
            )

        mediator.taImotInnsending(
            innsendingMottattHendelse,
        ) shouldBe HåndterInnsendingResultat.HåndtertInnsending(sakId)

        slot.captured.let {
            it.person shouldBe personMedSak
            it.journalpostId shouldBe journalpostId
            it.mottatt shouldBe registrertTidspunkt
            it.skjemaKode shouldBe skjemaKode
            it.kategori shouldBe Kategori.KLAGE
            it.tilstand().type shouldBe KLAR_TIL_BEHANDLING
            it.tilstandslogg.single().hendelse shouldBe innsendingMottattHendelse
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
            it.tilstand().type shouldBe KLAR_TIL_BEHANDLING
            it.tilstandslogg.single().hendelse shouldBe innsendingMottattHendelse
        }
    }

    @Test
    fun `Tildeling av en innsending`() {
        val slot = slot<Innsending>()
        val saksbehandler = Saksbehandler(navIdent = "saksbehandler1", emptySet())
        val innsending =
            TestHelper.lagInnsending(
                behandlerIdent = null,
                tilstand = KlarTilBehandling,
            )

        val innsendingRepository =
            mockk<InnsendingRepository>().also {
                every { it.hent(innsending.innsendingId) } returns innsending
                every { it.lagre(capture(slot)) } just Runs
            }
        InnsendingMediator(
            sakMediator = mockk(),
            oppgaveMediator = mockk(),
            personMediator = mockk(),
            innsendingRepository = innsendingRepository,
            innsendingBehandler = mockk(),
        ).tildel(
            TildelHendelse(
                innsendingId = innsending.innsendingId,
                ansvarligIdent = "ansvarlig",
                utførtAv = saksbehandler,
            ),
        )

        slot.captured.let {
            it.tilstand() shouldBe UnderBehandling
            it.behandlerIdent() shouldBe "ansvarlig"
        }
    }

    @Test
    fun `Ferdigstilling av en innsending`() {
        val slot = slot<Innsending>()
        val saksbehandler = Saksbehandler(navIdent = "saksbehandler1", emptySet())
        val innsending =
            TestHelper.lagInnsending(
                tilstand = UnderBehandling,
                behandlerIdent = saksbehandler.navIdent,
            )

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

        slot.captured.let {
            it.tilstand() shouldBe Ferdigbehandlet
            it.tilstandslogg.first().hendelse.let { hendelse ->
                hendelse shouldBe innsendingFerdigstiltHendelse
            }
        }
    }

    @Test
    fun `Avbryt innsending hvis behandling opprettes for søknad`() {
        val slot = slot<Innsending>()
        val person = TestHelper.testPerson
        val søknadId = UUIDv7.ny()
        val innsending =
            TestHelper.lagInnsending(
                person = person,
                tilstand = KlarTilBehandling,
                kategori = Kategori.NY_SØKNAD,
            ).also { innsending ->
                innsending.tilstandslogg.leggTil(
                    nyTilstand = KlarTilBehandling.type,
                    hendelse =
                        InnsendingMottattHendelse(
                            ident = person.ident,
                            journalpostId = innsending.journalpostId,
                            registrertTidspunkt = innsending.mottatt,
                            søknadId = søknadId,
                            skjemaKode = "NAV 04-01.03",
                            kategori = Kategori.NY_SØKNAD,
                        ),
                )
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

        slot.captured.let {
            it.tilstand() shouldBe Avbrutt
            it.tilstandslogg.first().hendelse.let { hendelse ->
                hendelse shouldBe behandlingOpprettetForSøknadHendelse
            }
        }
    }

    @Test
    fun `Ikke avbryt innsending hvis behandling opprettes for søknad og innsendingen er under behandling`() {
        val slot = slot<Innsending>()
        val person = TestHelper.testPerson
        val søknadId = UUIDv7.ny()
        val innsending =
            TestHelper.lagInnsending(
                person = person,
                tilstand = UnderBehandling,
                kategori = Kategori.NY_SØKNAD,
                behandlerIdent = saksbehandler.navIdent,
            ).also { innsending ->
                innsending.tilstandslogg.leggTil(
                    nyTilstand = KLAR_TIL_BEHANDLING,
                    hendelse =
                        InnsendingMottattHendelse(
                            ident = person.ident,
                            journalpostId = innsending.journalpostId,
                            registrertTidspunkt = innsending.mottatt,
                            søknadId = søknadId,
                            skjemaKode = "NAV 04-01.03",
                            kategori = Kategori.NY_SØKNAD,
                        ),
                )
                innsending.tilstandslogg.leggTil(
                    nyTilstand = UNDER_BEHANDLING,
                    hendelse =
                        TildelHendelse(
                            innsendingId = innsending.innsendingId,
                            utførtAv = saksbehandler,
                            ansvarligIdent = saksbehandler.navIdent,
                        ),
                )
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

    @Test
    fun `Ikke avbryt innsending hvis behandling opprettes for søknad og innsendingen er ferdigbehandlet`() {
        val slot = slot<Innsending>()
        val person = TestHelper.testPerson
        val søknadId = UUIDv7.ny()
        val innsending =
            TestHelper.lagInnsending(
                person = person,
                tilstand = Ferdigbehandlet,
                kategori = Kategori.NY_SØKNAD,
                behandlerIdent = saksbehandler.navIdent,
            ).also { innsending ->
                innsending.tilstandslogg.leggTil(
                    nyTilstand = KLAR_TIL_BEHANDLING,
                    hendelse =
                        InnsendingMottattHendelse(
                            ident = person.ident,
                            journalpostId = innsending.journalpostId,
                            registrertTidspunkt = innsending.mottatt,
                            søknadId = søknadId,
                            skjemaKode = "NAV 04-01.03",
                            kategori = Kategori.NY_SØKNAD,
                        ),
                )
                innsending.tilstandslogg.leggTil(
                    nyTilstand = UNDER_BEHANDLING,
                    hendelse =
                        TildelHendelse(
                            innsendingId = innsending.innsendingId,
                            utførtAv = saksbehandler,
                            ansvarligIdent = saksbehandler.navIdent,
                        ),
                )
                innsending.tilstandslogg.leggTil(
                    nyTilstand = FERDIGBEHANDLET,
                    hendelse =
                        FerdigstillInnsendingHendelse(
                            innsendingId = innsending.innsendingId,
                            aksjon = Aksjon.Avslutt,
                            utførtAv = saksbehandler,
                        ),
                )
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

    @Test
    fun `Ikke avbryt innsending hvis behandling opprettes for søknad og innsendingen mangler søknadId`() {
        val slot = slot<Innsending>()
        val person = TestHelper.testPerson
        val søknadId = UUIDv7.ny()
        val innsending =
            TestHelper.lagInnsending(
                person = person,
                tilstand = KlarTilBehandling,
                kategori = Kategori.NY_SØKNAD,
                behandlerIdent = saksbehandler.navIdent,
            ).also { innsending ->
                innsending.tilstandslogg.leggTil(
                    nyTilstand = KLAR_TIL_BEHANDLING,
                    hendelse =
                        InnsendingMottattHendelse(
                            ident = person.ident,
                            journalpostId = innsending.journalpostId,
                            registrertTidspunkt = innsending.mottatt,
                            søknadId = null,
                            skjemaKode = "NAV 04-01.03",
                            kategori = Kategori.NY_SØKNAD,
                        ),
                )
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
