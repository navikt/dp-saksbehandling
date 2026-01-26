package no.nav.dagpenger.saksbehandling.frist

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.PåVent
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TestHelper.lagBehandling
import no.nav.dagpenger.saksbehandling.TestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.UtløstAvType.KLAGE
import no.nav.dagpenger.saksbehandling.UtløstAvType.SØKNAD
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveFristUtgåttJobTest {
    @Test
    fun `Sett utgåtte oppgaver klare igjen`() {
        val behandling1 = lagBehandling(utløstAvType = SØKNAD)
        val behandling2 = lagBehandling(utløstAvType = SØKNAD)
        val behandling3 = lagBehandling(utløstAvType = KLAGE)
        val behandling4 = lagBehandling(utløstAvType = KLAGE)
        DBTestHelper.withBehandlinger(
            person = TestHelper.testPerson,
            behandlinger = listOf(behandling1, behandling2, behandling3, behandling4),
        ) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = repo,
                    behandlingKlient = mockk(),
                    utsendingMediator = mockk(),
                    sakMediator = mockk(),
                )
            val saksbehandlerIdent1 = "ident 1"
            val saksbehandlerIdent2 = "ident 2"
            val iDag = LocalDate.now()
            val iMorgen = iDag.plusDays(1)

            val oppgave1 =
                lagOppgave(
                    tilstand = PåVent,
                    utsattTil = iDag,
                    behandling = behandling1,
                    saksbehandlerIdent = null,
                )
            val oppgave2 =
                lagOppgave(
                    tilstand = PåVent,
                    utsattTil = iDag,
                    behandling = behandling2,
                    saksbehandlerIdent = saksbehandlerIdent1,
                    emneknagger = setOf(Emneknagg.TIDLIGERE_UTSATT.visningsnavn),
                )
            val oppgave3 =
                lagOppgave(
                    tilstand = PåVent,
                    utsattTil = iDag,
                    behandling = behandling3,
                    saksbehandlerIdent = saksbehandlerIdent2,
                )
            val oppgave4 =
                lagOppgave(
                    tilstand = PåVent,
                    utsattTil = iMorgen,
                    behandling = behandling4,
                    saksbehandlerIdent = saksbehandlerIdent1,
                )

            repo.lagre(oppgave1)
            repo.lagre(oppgave2)
            repo.lagre(oppgave3)
            repo.lagre(oppgave4)

            runBlocking {
                OppgaveFristUtgåttJob(oppgaveMediator).executeJob()
            }

            repo.hentOppgave(oppgave1.oppgaveId).let { oppgave ->
                oppgave.tilstand() shouldBe KlarTilBehandling
                oppgave.emneknagger shouldContain Emneknagg.TIDLIGERE_UTSATT.visningsnavn
                oppgave.behandlerIdent shouldBe null
                oppgave.tilstandslogg.first().tilstand shouldBe KLAR_TIL_BEHANDLING
                oppgave.utsattTil() shouldBe null
            }

            repo.hentOppgave(oppgave2.oppgaveId).let { oppgave ->
                oppgave.tilstand() shouldBe UnderBehandling
                oppgave.emneknagger shouldContain Emneknagg.TIDLIGERE_UTSATT.visningsnavn
                oppgave.behandlerIdent shouldBe saksbehandlerIdent1
                oppgave.tilstandslogg.first().tilstand shouldBe UNDER_BEHANDLING
                oppgave.utsattTil() shouldBe null
            }

            repo.hentOppgave(oppgave3.oppgaveId).let { oppgave ->
                oppgave.tilstand() shouldBe UnderBehandling
                oppgave.emneknagger shouldContain Emneknagg.TIDLIGERE_UTSATT.visningsnavn
                oppgave.behandlerIdent shouldBe saksbehandlerIdent2
                oppgave.tilstandslogg.first().tilstand shouldBe UNDER_BEHANDLING
                oppgave.utsattTil() shouldBe null
            }

            repo.hentOppgave(oppgave4.oppgaveId).let { oppgave ->
                oppgave.tilstand() shouldBe PåVent
                oppgave.emneknagger shouldNotContain Emneknagg.TIDLIGERE_UTSATT.visningsnavn
                oppgave.behandlerIdent shouldBe saksbehandlerIdent1
                oppgave.utsattTil() shouldNotBe null
            }
        }
    }
}
