package no.nav.dagpenger.saksbehandling.henvendelse

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
import no.nav.dagpenger.saksbehandling.db.henvendelse.HenvendelseRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillHenvendelseHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.Avbrutt
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.UnderBehandling
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class HenvendelseMediatorTest {
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
    fun `Skal lage henvendelse dersom vi eier saken`() {
        val slot = slot<Henvendelse>()
        val henvendelseRepositoryMock: HenvendelseRepository =
            mockk<HenvendelseRepository>().also {
                every { it.lagre(capture(slot)) } just Runs
            }
        val mediator =
            HenvendelseMediator(
                sakMediator = sakMediatorMock,
                oppgaveMediator = oppgaveMediatorMock,
                personMediator = personMediatorMock,
                henvendelseRepository = henvendelseRepositoryMock,
                henvendelseBehandler = mockk(),
            )

        val henvendelseMottattHendelse =
            HenvendelseMottattHendelse(
                ident = personMedSak.ident,
                journalpostId = journalpostId,
                registrertTidspunkt = registrertTidspunkt,
                søknadId = null,
                skjemaKode = skjemaKode,
                kategori = Kategori.KLAGE,
            )

        mediator.taImotHenvendelse(
            henvendelseMottattHendelse,
        ) shouldBe HåndterHenvendelseResultat.HåndtertHenvendelse(sakId)

        slot.captured.let {
            it.person shouldBe personMedSak
            it.journalpostId shouldBe journalpostId
            it.mottatt shouldBe registrertTidspunkt
            it.skjemaKode shouldBe skjemaKode
            it.kategori shouldBe Kategori.KLAGE
            it.tilstand().type shouldBe KLAR_TIL_BEHANDLING
            it.tilstandslogg.single().hendelse shouldBe henvendelseMottattHendelse
        }
    }

    @Test
    fun `Skal ikke håndtere henvendelse hvis vi ikke eier saken`() {
        val henvendelseRepository = mockk<HenvendelseRepository>(relaxed = false)
        val henvendelseBehandler = mockk<HenvendelseBehandler>()
        val mediator =
            HenvendelseMediator(
                sakMediator = sakMediatorMock,
                oppgaveMediator = oppgaveMediatorMock,
                personMediator = personMediatorMock,
                henvendelseRepository = henvendelseRepository,
                henvendelseBehandler = henvendelseBehandler,
            )

        val henvendelseMottattHendelse =
            HenvendelseMottattHendelse(
                ident = personUtenSak.ident,
                journalpostId = journalpostId,
                registrertTidspunkt = registrertTidspunkt,
                søknadId = null,
                skjemaKode = skjemaKode,
                kategori = Kategori.KLAGE,
            )

        mediator.taImotHenvendelse(
            henvendelseMottattHendelse,
        ) shouldBe HåndterHenvendelseResultat.UhåndtertHenvendelse

        verify(exactly = 0) {
            henvendelseRepository.lagre(any())
        }
        verify(exactly = 0) {
            henvendelseBehandler.utførAksjon(any(), any())
        }
    }

    @Test
    fun `Skal lage henvendelse hvis vi ikke eier saken men skal varsle om ettersending`() {
        val slot = slot<Henvendelse>()
        val henvendelseRepositoryMock: HenvendelseRepository =
            mockk<HenvendelseRepository>().also {
                every { it.lagre(capture(slot)) } just Runs
            }
        val mediator =
            HenvendelseMediator(
                sakMediator = sakMediatorMock,
                oppgaveMediator = oppgaveMediatorMock,
                personMediator = personMediatorMock,
                henvendelseRepository = henvendelseRepositoryMock,
                henvendelseBehandler = mockk(),
            )

        val henvendelseMottattHendelse =
            HenvendelseMottattHendelse(
                ident = personUtenSak.ident,
                journalpostId = journalpostId,
                registrertTidspunkt = registrertTidspunkt,
                søknadId = søknadIdSomSkalVarsles,
                skjemaKode = skjemaKode,
                kategori = Kategori.ETTERSENDING,
            )

        mediator.taImotHenvendelse(
            henvendelseMottattHendelse,
        ) shouldBe HåndterHenvendelseResultat.UhåndtertHenvendelse

        slot.captured.let {
            it.person shouldBe personUtenSak
            it.journalpostId shouldBe journalpostId
            it.mottatt shouldBe registrertTidspunkt
            it.skjemaKode shouldBe skjemaKode
            it.kategori shouldBe Kategori.ETTERSENDING
            it.tilstand().type shouldBe KLAR_TIL_BEHANDLING
            it.tilstandslogg.single().hendelse shouldBe henvendelseMottattHendelse
        }
    }

    @Test
    fun `Tildeling av en henvendelse`() {
        val slot = slot<Henvendelse>()
        val saksbehandler = Saksbehandler(navIdent = "saksbehandler1", emptySet())
        val henvendelse =
            TestHelper.lagHenvendelse(
                behandlerIdent = null,
                tilstand = KlarTilBehandling,
            )

        val henvendelseRepository =
            mockk<HenvendelseRepository>().also {
                every { it.hent(henvendelse.henvendelseId) } returns henvendelse
                every { it.lagre(capture(slot)) } just Runs
            }
        HenvendelseMediator(
            sakMediator = mockk(),
            oppgaveMediator = mockk(),
            personMediator = mockk(),
            henvendelseRepository = henvendelseRepository,
            henvendelseBehandler = mockk(),
        ).tildel(
            TildelHendelse(
                henvendelseId = henvendelse.henvendelseId,
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
    fun `Ferdigstilling av en henvendelse`() {
        val slot = slot<Henvendelse>()
        val saksbehandler = Saksbehandler(navIdent = "saksbehandler1", emptySet())
        val henvendelse =
            TestHelper.lagHenvendelse(
                tilstand = UnderBehandling,
                behandlerIdent = saksbehandler.navIdent,
            )

        val henvendelseRepository =
            mockk<HenvendelseRepository>().also {
                every { it.hent(henvendelse.henvendelseId) } returns henvendelse
                every { it.lagre(capture(slot)) } just Runs
            }

        val henvendelseFerdigstiltHendelse =
            HenvendelseFerdigstiltHendelse(
                henvendelseId = henvendelse.henvendelseId,
                aksjon = Aksjon.OpprettKlage::class.java.simpleName,
                behandlingId = UUIDv7.ny(),
                utførtAv = saksbehandler,
            )

        val ferdigstillHenvendelseHendelse =
            FerdigstillHenvendelseHendelse(
                henvendelseId = henvendelse.henvendelseId,
                aksjon = Aksjon.OpprettKlage(sakId),
                utførtAv = saksbehandler,
            )

        val henvendelseBehandler =
            mockk<HenvendelseBehandler>().also {
                every {
                    it.utførAksjon(
                        hendelse = ferdigstillHenvendelseHendelse,
                        henvendelse = henvendelse,
                    )
                } returns henvendelseFerdigstiltHendelse
            }

        HenvendelseMediator(
            sakMediator = mockk(),
            oppgaveMediator = mockk(),
            personMediator = mockk(),
            henvendelseRepository = henvendelseRepository,
            henvendelseBehandler = henvendelseBehandler,
        ).ferdigstill(
            ferdigstillHenvendelseHendelse,
        )

        slot.captured.let {
            it.tilstand() shouldBe Henvendelse.Tilstand.Ferdigbehandlet
            it.tilstandslogg.first().hendelse.let { hendelse ->
                hendelse shouldBe henvendelseFerdigstiltHendelse
            }
        }
    }

    @Test
    fun `Avbryt henvendelse hvis behandling opprettes for søknad`() {
        val slot = slot<Henvendelse>()
        val person = TestHelper.testPerson
        val søknadId = UUIDv7.ny()
        val henvendelse =
            TestHelper.lagHenvendelse(
                person = person,
                tilstand = KlarTilBehandling,
                kategori = Kategori.NY_SØKNAD,
            ).also { henvendelse ->
                henvendelse.tilstandslogg.leggTil(
                    nyTilstand = KlarTilBehandling.type,
                    hendelse =
                        HenvendelseMottattHendelse(
                            ident = person.ident,
                            journalpostId = henvendelse.journalpostId,
                            registrertTidspunkt = henvendelse.mottatt,
                            søknadId = søknadId,
                            skjemaKode = "NAV 04-01.03",
                            kategori = Kategori.NY_SØKNAD,
                        ),
                )
            }

        val henvendelseRepository =
            mockk<HenvendelseRepository>().also {
                every { it.hent(henvendelse.henvendelseId) } returns henvendelse
                every { it.lagre(capture(slot)) } just Runs
                every { it.finnHenvendelserForPerson(person.ident) } returns listOf(henvendelse)
            }

        val behandlingOpprettetForSøknadHendelse =
            BehandlingOpprettetForSøknadHendelse(
                ident = henvendelse.person.ident,
                søknadId = søknadId,
                behandlingId = UUIDv7.ny(),
            )

        HenvendelseMediator(
            sakMediator = mockk(),
            oppgaveMediator = mockk(),
            personMediator = mockk(),
            henvendelseRepository = henvendelseRepository,
            henvendelseBehandler = mockk(),
        ).avbrytHenvendelse(hendelse = behandlingOpprettetForSøknadHendelse)

        slot.captured.let {
            it.tilstand() shouldBe Avbrutt
            it.tilstandslogg.first().hendelse.let { hendelse ->
                hendelse shouldBe behandlingOpprettetForSøknadHendelse
            }
        }
    }

    @Test
    fun `Ikke avbryt henvendelse hvis behandling opprettes for søknad og henvendelsen er under arbeid`() {
        val slot = slot<Henvendelse>()
        val person = TestHelper.testPerson
        val søknadId = UUIDv7.ny()
        val henvendelse =
            TestHelper.lagHenvendelse(
                person = person,
                tilstand = UnderBehandling,
                kategori = Kategori.NY_SØKNAD,
                behandlerIdent = saksbehandler.navIdent,
            ).also { henvendelse ->
                henvendelse.tilstandslogg.leggTil(
                    nyTilstand = KLAR_TIL_BEHANDLING,
                    hendelse =
                        HenvendelseMottattHendelse(
                            ident = person.ident,
                            journalpostId = henvendelse.journalpostId,
                            registrertTidspunkt = henvendelse.mottatt,
                            søknadId = søknadId,
                            skjemaKode = "NAV 04-01.03",
                            kategori = Kategori.NY_SØKNAD,
                        ),
                )
                henvendelse.tilstandslogg.leggTil(
                    nyTilstand = UNDER_BEHANDLING,
                    hendelse =
                        TildelHendelse(
                            henvendelseId = henvendelse.henvendelseId,
                            utførtAv = saksbehandler,
                            ansvarligIdent = saksbehandler.navIdent,
                        ),
                )
            }

        val henvendelseRepository =
            mockk<HenvendelseRepository>().also {
                every { it.hent(henvendelse.henvendelseId) } returns henvendelse
                every { it.lagre(capture(slot)) } just Runs
                every { it.finnHenvendelserForPerson(person.ident) } returns listOf(henvendelse)
            }

        val behandlingOpprettetForSøknadHendelse =
            BehandlingOpprettetForSøknadHendelse(
                ident = henvendelse.person.ident,
                søknadId = søknadId,
                behandlingId = UUIDv7.ny(),
            )

        HenvendelseMediator(
            sakMediator = mockk(),
            oppgaveMediator = mockk(),
            personMediator = mockk(),
            henvendelseRepository = henvendelseRepository,
            henvendelseBehandler = mockk(),
        ).avbrytHenvendelse(hendelse = behandlingOpprettetForSøknadHendelse)

        verify(exactly = 0) { henvendelseRepository.lagre(any()) }
    }

    @Test
    fun `Ikke avbryt henvendelse hvis behandling opprettes for søknad og henvendelsen mangler søknadId`() {
        val slot = slot<Henvendelse>()
        val person = TestHelper.testPerson
        val søknadId = UUIDv7.ny()
        val henvendelse =
            TestHelper.lagHenvendelse(
                person = person,
                tilstand = KlarTilBehandling,
                kategori = Kategori.NY_SØKNAD,
                behandlerIdent = saksbehandler.navIdent,
            ).also { henvendelse ->
                henvendelse.tilstandslogg.leggTil(
                    nyTilstand = KLAR_TIL_BEHANDLING,
                    hendelse =
                        HenvendelseMottattHendelse(
                            ident = person.ident,
                            journalpostId = henvendelse.journalpostId,
                            registrertTidspunkt = henvendelse.mottatt,
                            søknadId = null,
                            skjemaKode = "NAV 04-01.03",
                            kategori = Kategori.NY_SØKNAD,
                        ),
                )
            }

        val henvendelseRepository =
            mockk<HenvendelseRepository>().also {
                every { it.hent(henvendelse.henvendelseId) } returns henvendelse
                every { it.lagre(capture(slot)) } just Runs
                every { it.finnHenvendelserForPerson(person.ident) } returns listOf(henvendelse)
            }

        val behandlingOpprettetForSøknadHendelse =
            BehandlingOpprettetForSøknadHendelse(
                ident = henvendelse.person.ident,
                søknadId = søknadId,
                behandlingId = UUIDv7.ny(),
            )

        HenvendelseMediator(
            sakMediator = mockk(),
            oppgaveMediator = mockk(),
            personMediator = mockk(),
            henvendelseRepository = henvendelseRepository,
            henvendelseBehandler = mockk(),
        ).avbrytHenvendelse(hendelse = behandlingOpprettetForSøknadHendelse)

        verify(exactly = 0) { henvendelseRepository.lagre(any()) }
    }
}
