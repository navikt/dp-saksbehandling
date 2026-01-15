package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.TestHelper.lagBehandling
import no.nav.dagpenger.saksbehandling.TestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.test.Test

class StatistikkV2Test {
    @Test
    fun `test hentAntallVedtakGjortV2`() {
        val behandling1 = lagBehandling(behandlingId = UUIDv7.ny())
        val behandling2 = lagBehandling(behandlingId = UUIDv7.ny())
        val behandling3 = lagBehandling(behandlingId = UUIDv7.ny())
        DBTestHelper.withBehandlinger(
            behandlinger = listOf(behandling1, behandling2, behandling3),
        ) { ds: DataSource ->
            // Insert test data
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling1,
                ),
            )
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling2,
                ),
            )
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling3,
                ),
            )

            val statistikkTjeneste = PostgresStatistikkV2Tjeneste(ds)
            val statistikkFilter =
                StatistikkFilter(
                    periode = Periode(fom = LocalDate.of(2025, 1, 1).minusDays(1), tom = LocalDate.now().plusDays(1)),
                )
            val result = statistikkTjeneste.hentAntallVedtakGjort(statistikkFilter)

            result shouldBe 3
        }
    }

    @Test
    fun `test hentAntallVedtakGjortV2 utenfor periode`() {
        val behandling1 = lagBehandling(behandlingId = UUIDv7.ny())
        val behandling2 = lagBehandling(behandlingId = UUIDv7.ny())
        val behandling3 = lagBehandling(behandlingId = UUIDv7.ny())
        DBTestHelper.withBehandlinger(
            behandlinger = listOf(behandling1, behandling2, behandling3),
        ) { ds: DataSource ->
            // Insert test data
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling1,
                ),
            )
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling2,
                ),
            )
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling3,
                ),
            )

            val statistikkTjeneste = PostgresStatistikkV2Tjeneste(ds)
            val statistikkFilter =
                StatistikkFilter(
                    periode = Periode(fom = LocalDate.of(1000, 1, 1), tom = LocalDate.of(1000, 1, 1)),
                )
            val result = statistikkTjeneste.hentAntallVedtakGjort(statistikkFilter)

            result shouldBe 0
        }
    }
}
