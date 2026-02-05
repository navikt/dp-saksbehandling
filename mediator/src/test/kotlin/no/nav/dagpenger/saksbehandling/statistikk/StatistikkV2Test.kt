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
    fun `test hent alt fra statistikkv2`() {
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
                    emneknagger = setOf("hallo"),
                ),
            )
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling2,
                    emneknagger = setOf("hallo"),
                ),
            )
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling3,
                    emneknagger = setOf("hallo2"),
                ),
            )

            val statistikkTjeneste = PostgresStatistikkV2Tjeneste(ds)
            val statistikkFilter =
                StatistikkFilter(
                    periode = Periode(fom = LocalDate.of(2025, 1, 1).minusDays(1), tom = LocalDate.now().plusDays(1)),
                )
            val oppgavetyper = statistikkTjeneste.hentOppgavetyper(statistikkFilter)

            oppgavetyper.size shouldBe 1
            oppgavetyper[0].total shouldBe 3
            oppgavetyper[0].navn shouldBe behandling1.utløstAv.name
            oppgavetyper[0].eldsteOppgave.toLocalDate() shouldBe behandling1.opprettet.toLocalDate()

            val oppgavetypeserier = statistikkTjeneste.hentOppgavetypeSerier(statistikkFilter)
            oppgavetypeserier.size shouldBe 1
            oppgavetypeserier[0].navn shouldBe behandling1.utløstAv.name
            oppgavetypeserier[0].verdier.size shouldBe 1
            oppgavetypeserier[0].verdier[0] shouldBe 3

            val rettighetstyper = statistikkTjeneste.hentRettighetstyper(statistikkFilter)
            rettighetstyper.size shouldBe 1
            rettighetstyper[0].total shouldBe 3
            rettighetstyper[0].navn shouldBe Oppgave.FerdigBehandlet.type.name
            rettighetstyper[0].eldsteOppgave.toLocalDate() shouldBe behandling1.opprettet.toLocalDate()

            val rettighetstypeserier = statistikkTjeneste.hentRettighetstypeSerier(statistikkFilter)
            rettighetstypeserier.size shouldBe 2
            rettighetstypeserier[0].navn shouldBe "hallo"
            rettighetstypeserier[0].verdier.size shouldBe 1
            rettighetstypeserier[0].verdier[0] shouldBe 2
            rettighetstypeserier[1].navn shouldBe "hallo2"
            rettighetstypeserier[1].verdier.size shouldBe 1
            rettighetstypeserier[1].verdier[0] shouldBe 1
        }
    }
}
