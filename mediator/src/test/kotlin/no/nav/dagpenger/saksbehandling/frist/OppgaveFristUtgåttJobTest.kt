package no.nav.dagpenger.saksbehandling.frist

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.PaaVent
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresRepository
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveFristUtgåttJobTest {
    @Test
    fun `Sett utgåtte oppgaver til KLAR_FOR_BEHANDLING`() =
        withMigratedDb { ds ->
            val saksbehandlerIdent = "Z123456"
            val repo = PostgresRepository(ds)

            val idag = LocalDate.now()
            val igår = idag.minusDays(1)

            val oppgave =
                lagOppgave(
                    tilstand = PaaVent,
                    utsattTil = igår,
                    saksbehandlerIdent = null,
                )
            val oppgave2 =
                lagOppgave(
                    tilstand = PaaVent,
                    utsattTil = igår,
                    saksbehandlerIdent = saksbehandlerIdent,
                )

            val oppgave3 =
                lagOppgave(
                    tilstand = PaaVent,
                    utsattTil = idag,
                    saksbehandlerIdent = saksbehandlerIdent,
                )

            repo.lagre(oppgave)
            repo.lagre(oppgave2)
            repo.lagre(oppgave3)

            settOppgaverMedUtgåttFristTilKlarTilBehandling(
                dataSource = ds,
                frist = idag,
            )

            repo.hentOppgave(oppgave.oppgaveId).let { oppgave ->
                oppgave.tilstand() shouldBe KlarTilBehandling
                oppgave.emneknagger shouldContain "Tidligere utsatt"
                oppgave.saksbehandlerIdent shouldBe null
            }

            repo.hentOppgave(oppgave2.oppgaveId).let { oppgave ->
                oppgave.tilstand() shouldBe KlarTilBehandling
                oppgave.emneknagger shouldContain "Tidligere utsatt"
                oppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
            }

            repo.hentOppgave(oppgave3.oppgaveId).let { oppgave ->
                oppgave.tilstand() shouldBe PaaVent
                oppgave.emneknagger shouldNotContain "Tidligere utsatt"
                oppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
            }
        }
}
