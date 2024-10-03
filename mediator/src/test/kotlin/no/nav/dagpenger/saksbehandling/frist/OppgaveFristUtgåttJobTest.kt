package no.nav.dagpenger.saksbehandling.frist

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.PaaVent
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveFristUtgåttJobTest {
    @Test
    fun `Sett utgåtte oppgaver til KLAR_FOR_BEHANDLING`() =
        withMigratedDb { ds ->
            val saksbehandlerIdent = "Z123456"
            val repo = PostgresOppgaveRepository(ds)

            val iDag = LocalDate.now()
            val iMorgen = iDag.plusDays(1)

            val oppgave1 =
                lagOppgave(
                    tilstand = PaaVent,
                    utsattTil = iDag,
                    saksbehandlerIdent = null,
                )
            val oppgave2 =
                lagOppgave(
                    tilstand = PaaVent,
                    utsattTil = iDag,
                    saksbehandlerIdent = saksbehandlerIdent,
                    emneknagger = setOf("Tidligere utsatt"),
                )

            val oppgave3 =
                lagOppgave(
                    tilstand = PaaVent,
                    utsattTil = iMorgen,
                    saksbehandlerIdent = saksbehandlerIdent,
                )

            repo.lagre(oppgave1)
            repo.lagre(oppgave2)
            repo.lagre(oppgave3)

            settOppgaverMedUtgåttFristTilKlarTilBehandling(
                dataSource = ds,
                frist = iDag,
            )

            repo.hentOppgave(oppgave1.oppgaveId).let { oppgave ->
                oppgave.tilstand() shouldBe KlarTilBehandling
                oppgave.emneknagger shouldContain "Tidligere utsatt"
                oppgave.behandlerIdent shouldBe null
            }

            repo.hentOppgave(oppgave2.oppgaveId).let { oppgave ->
                oppgave.tilstand() shouldBe KlarTilBehandling
                oppgave.emneknagger shouldContain "Tidligere utsatt"
                oppgave.behandlerIdent shouldBe saksbehandlerIdent
            }

            repo.hentOppgave(oppgave3.oppgaveId).let { oppgave ->
                oppgave.tilstand() shouldBe PaaVent
                oppgave.emneknagger shouldNotContain "Tidligere utsatt"
                oppgave.behandlerIdent shouldBe saksbehandlerIdent
            }
        }
}
