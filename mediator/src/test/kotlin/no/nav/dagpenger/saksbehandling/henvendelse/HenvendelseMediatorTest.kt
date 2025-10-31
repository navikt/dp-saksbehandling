package no.nav.dagpenger.saksbehandling.henvendelse

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class HenvendelseMediatorTest {
    private val sakId = UUIDv7.ny()
    private val journalpostId = "journalpostId123"
    private val testIdentMedSak = "33333388888"
    private val testIdentUtenSak = "11111122222"
    private val søknadIdSomSkalVarsles = UUIDv7.ny()
    private val søknadIdSomIkkeSkalVarsles = UUIDv7.ny()
    private val registrertTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    private val sakMediatorMock: SakMediator =
        mockk<SakMediator>(relaxed = true).also {
            coEvery { it.finnSisteSakId(testIdentMedSak) } returns sakId
            coEvery { it.finnSisteSakId(testIdentUtenSak) } returns null
        }
    private val oppgaveMediatorMock =
        mockk<OppgaveMediator>().also {
            coEvery { it.skalEttersendingTilSøknadVarsles(søknadIdSomSkalVarsles, any()) } returns true
            coEvery { it.skalEttersendingTilSøknadVarsles(søknadIdSomIkkeSkalVarsles, any()) } returns false
        }

    @Test
    fun `Skal ta imot og håndtere henvendelse på egne saker`() {
        withMigratedDb { dataSource ->
            val henvendelseMediator = HenvendelseMediator(sakMediatorMock, oppgaveMediatorMock)
            val håndterHenvendelseResultat =
                henvendelseMediator.taImotHenvendelse(
                    HenvendelseMottattHendelse(
                        ident = testIdentMedSak,
                        journalpostId = journalpostId,
                        registrertTidspunkt = registrertTidspunkt,
                        søknadId = null,
                        skjemaKode = "GENERELL_INNSENDING",
                        kategori = Kategori.GENERELL,
                    ),
                )
            håndterHenvendelseResultat shouldBe HåndterHenvendelseResultat.HåndtertHenvendelse(sakId)
        }
    }

    @Test
    fun `Skal ikke håndtere henvendelse på saker utenfor vår løsning`() {
        withMigratedDb { dataSource ->
            val henvendelseMediator = HenvendelseMediator(sakMediatorMock, oppgaveMediatorMock)
            val håndterHenvendelseResultat =
                henvendelseMediator.taImotHenvendelse(
                    HenvendelseMottattHendelse(
                        ident = testIdentUtenSak,
                        journalpostId = journalpostId,
                        registrertTidspunkt = registrertTidspunkt,
                        søknadId = null,
                        skjemaKode = "GENERELL_INNSENDING",
                        kategori = Kategori.GENERELL,
                    ),
                )
            håndterHenvendelseResultat shouldBe HåndterHenvendelseResultat.UhåndtertHenvendelse
        }
    }
}
