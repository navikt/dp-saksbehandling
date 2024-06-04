package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresRepository
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import no.nav.dagpenger.saksbehandling.mottak.UtsendingMottak
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource

class UtsendingMediatorTest {
    private val rapid = TestRapid()

    @Test
    fun `livsyklus av en utsending`() {
        withMigratedDb { datasource ->
            val oppgaveId = lagreOppgave(datasource)
            val repository = PostgresUtsendingRepository(datasource)
            val utsendingMediator = UtsendingMediator(repository)
            UtsendingMottak(
                rapidsConnection = rapid,
                mediator = utsendingMediator,
            )

            rapid.sendTestMessage(startUtsendingEvent(oppgaveId))

            val utsending: Utsending = repository.hent(oppgaveId)
            utsending.oppgaveId shouldBe oppgaveId
        }
    }

    private fun startUtsendingEvent(oppgaveId: UUID) =
        """
                    {
                        "@event_name": "start_utsending",
                        "oppgaveId": "$oppgaveId"
                    }
                    """

    private fun lagreOppgave(dataSource: DataSource): UUID {
        val oppgave = lagOppgave()
        val repository = PostgresRepository(dataSource)
        repository.lagre(oppgave)
        return oppgave.oppgaveId
    }
}
