package no.nav.dagpenger.saksbehandling.frist

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.BehandlingType.KLAGE
import no.nav.dagpenger.saksbehandling.BehandlingType.SØKNAD
import no.nav.dagpenger.saksbehandling.Emneknagg.PåVent.TIDLIGERE_UTSATT
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.PåVent
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.lagBehandling
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.lagPerson
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveFristUtgåttJobTest {
    @Test
    fun `Sett utgåtte oppgaver klare igjen`() {
        val behandling1 = lagBehandling(type = SØKNAD)
        val behandling2 = lagBehandling(type = SØKNAD)
        val behandling3 = lagBehandling(type = KLAGE)
        val behandling4 = lagBehandling(type = KLAGE)
        DBTestHelper.withBehandlinger(
            person = lagPerson(),
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
                    behandlingId = behandling1.behandlingId,
                    saksbehandlerIdent = null,
                )
            val oppgave2 =
                lagOppgave(
                    tilstand = PåVent,
                    utsattTil = iDag,
                    behandlingId = behandling2.behandlingId,
                    saksbehandlerIdent = saksbehandlerIdent1,
                    emneknagger = setOf(TIDLIGERE_UTSATT.visningsnavn),
                )
            val oppgave3 =
                lagOppgave(
                    tilstand = PåVent,
                    utsattTil = iDag,
                    behandlingId = behandling3.behandlingId,
                    saksbehandlerIdent = saksbehandlerIdent2,
                )
            val oppgave4 =
                lagOppgave(
                    tilstand = PåVent,
                    utsattTil = iMorgen,
                    behandlingId = behandling4.behandlingId,
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
                oppgave.emneknagger shouldContain TIDLIGERE_UTSATT.visningsnavn
                oppgave.behandlerIdent shouldBe null
                oppgave.tilstandslogg.first().tilstand shouldBe KLAR_TIL_BEHANDLING
                oppgave.utsattTil() shouldBe null
            }

            repo.hentOppgave(oppgave2.oppgaveId).let { oppgave ->
                oppgave.tilstand() shouldBe UnderBehandling
                oppgave.emneknagger shouldContain TIDLIGERE_UTSATT.visningsnavn
                oppgave.behandlerIdent shouldBe saksbehandlerIdent1
                oppgave.tilstandslogg.first().tilstand shouldBe UNDER_BEHANDLING
                oppgave.utsattTil() shouldBe null
            }

            repo.hentOppgave(oppgave3.oppgaveId).let { oppgave ->
                oppgave.tilstand() shouldBe UnderBehandling
                oppgave.emneknagger shouldContain TIDLIGERE_UTSATT.visningsnavn
                oppgave.behandlerIdent shouldBe saksbehandlerIdent2
                oppgave.tilstandslogg.first().tilstand shouldBe UNDER_BEHANDLING
                oppgave.utsattTil() shouldBe null
            }

            repo.hentOppgave(oppgave4.oppgaveId).let { oppgave ->
                oppgave.tilstand() shouldBe PåVent
                oppgave.emneknagger shouldNotContain TIDLIGERE_UTSATT.visningsnavn
                oppgave.behandlerIdent shouldBe saksbehandlerIdent1
                oppgave.utsattTil() shouldNotBe null
            }
        }
    }
}
