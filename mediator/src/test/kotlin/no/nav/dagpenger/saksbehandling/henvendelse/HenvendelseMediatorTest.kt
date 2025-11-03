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
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.henvendelse.HenvendelseRepository
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.Type.KLAR_TIL_BEHANDLING
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
        val mediator =
            HenvendelseMediator(
                sakMediator = sakMediatorMock,
                oppgaveMediator = oppgaveMediatorMock,
                personMediator = personMediatorMock,
                henvendelseRepository = henvendelseRepository,
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
}
