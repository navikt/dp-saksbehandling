package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresRepository
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import no.nav.dagpenger.saksbehandling.mottak.UtsendingMottak
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerArkiverbarVersjonAvBrev
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.VenterPåVedtak
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource

class UtsendingMediatorTest {
    private val rapid = TestRapid()

    @Test
    fun `Vedtaksbrev starter utsendingen`() {
        withMigratedDb { datasource ->
            val oppgaveId = lagreOppgave(datasource)
            val utsendingRepository = PostgresUtsendingRepository(datasource)
            val utsendingMediator = UtsendingMediator(utsendingRepository)

            val brev = "vedtaksbrev.html"
            val vedtaksbrevHendelse = VedtaksbrevHendelse(oppgaveId, brev)
            utsendingMediator.mottaBrev(vedtaksbrevHendelse)

            val utsending = utsendingRepository.hent(oppgaveId)
            utsending.oppgaveId shouldBe oppgaveId
            utsending.tilstand().type shouldBe VenterPåVedtak
            utsending.brev() shouldBe brev
        }
    }

    @Test
    fun `livsyklus av en utsending`() {
        withMigratedDb { datasource ->
            val oppgaveId = lagreOppgave(datasource)
            val repository = PostgresUtsendingRepository(datasource)
            val utsendingMediator = UtsendingMediator(repository)
            UtsendingMottak(
                rapidsConnection = rapid,
                utsendingMediator = utsendingMediator,
            )
            utsendingMediator.mottaBrev(VedtaksbrevHendelse(oppgaveId, "vedtaksbrev.html"))

            val utsending = repository.hent(oppgaveId)
            utsending.oppgaveId shouldBe oppgaveId
            utsending.tilstand().type shouldBe VenterPåVedtak
            utsending.brev() shouldBe "vedtaksbrev.html"

            rapid.sendTestMessage(
                """
                {
                    "@event_name": "vedtak_fattet",
                    "søknadId": "${UUID.randomUUID()}",
                    "behandlingId": "${UUID.randomUUID()}",
                    "ident": "12345678901"
                }
                """,
            )
            utsending.tilstand().type shouldBe AvventerArkiverbarVersjonAvBrev
        }
    }

    private fun lagreOppgave(dataSource: DataSource): UUID {
        val oppgave = lagOppgave()
        val repository = PostgresRepository(dataSource)
        repository.lagre(oppgave)
        return oppgave.oppgaveId
    }
}
