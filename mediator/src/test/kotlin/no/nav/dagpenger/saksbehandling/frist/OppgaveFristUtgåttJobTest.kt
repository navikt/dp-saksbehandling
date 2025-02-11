package no.nav.dagpenger.saksbehandling.frist

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Emneknagg.PåVent.TIDLIGERE_UTSATT
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.PåVent
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.lagOppgave
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveFristUtgåttJobTest {
    @Test
    fun `Sett utgåtte oppgaver klare igjen`() =
        withMigratedDb { ds ->
            val oppgaveMediator =
                OppgaveMediator(
                    repository = PostgresOppgaveRepository(ds),
                    oppslag = mockk(),
                    behandlingKlient = mockk(),
                    utsendingMediator = mockk(),
                    meldingOmVedtakKlient = mockk(),
                )
            val saksbehandlerIdent1 = "ident 1"
            val saksbehandlerIdent2 = "ident 2"
            val repo = PostgresOppgaveRepository(ds)

            val iDag = LocalDate.now()
            val iMorgen = iDag.plusDays(1)

            val oppgave1 =
                lagOppgave(
                    tilstand = PåVent,
                    utsattTil = iDag,
                    saksbehandlerIdent = null,
                )
            val oppgave2 =
                lagOppgave(
                    tilstand = PåVent,
                    utsattTil = iDag,
                    saksbehandlerIdent = saksbehandlerIdent1,
                    emneknagger = setOf(TIDLIGERE_UTSATT.visningsnavn),
                )
            val oppgave3 =
                lagOppgave(
                    tilstand = PåVent,
                    utsattTil = iDag,
                    saksbehandlerIdent = saksbehandlerIdent2,
                )
            val oppgave4 =
                lagOppgave(
                    tilstand = PåVent,
                    utsattTil = iMorgen,
                    saksbehandlerIdent = saksbehandlerIdent1,
                )

            repo.lagre(oppgave1)
            repo.lagre(oppgave2)
            repo.lagre(oppgave3)
            repo.lagre(oppgave4)

            håndterOppgaverSomIkkeLengerSkalVærePåVent(oppgaveMediator)

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
