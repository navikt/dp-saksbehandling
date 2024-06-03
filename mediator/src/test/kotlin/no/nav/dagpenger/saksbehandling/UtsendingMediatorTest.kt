package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.mottak.UtsendingMottak
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class UtsendingMediatorTest {
    private val rapid = TestRapid()

    @Test
    fun `livsyklus test av en utsending`() {
        withMigratedDb { datasource ->
            val repository = PostgresUtsendingRepository(datasource)
            val utsendingMediator = UtsendingMediator(repository)
            UtsendingMottak(
                rapidsConnection = rapid,
                mediator = utsendingMediator
            )

            val oppgaveId = UUIDv7.ny()

            rapid.sendTestMessage(
                """
                {
                    "@event_name": "start_utsending",
                    "oppgaveId": "$oppgaveId"
                }
                """
            )

            repository.hent(oppgaveId)


        }
    }
}