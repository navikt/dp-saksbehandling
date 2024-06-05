package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.lagBehandling
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
            val oppgaveId = lagreOppgaveOgBehandling(datasource).first
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
            val (oppgaveId, behandlingId) = lagreOppgaveOgBehandling(datasource)

            val repository = PostgresUtsendingRepository(datasource)
            val utsendingMediator = UtsendingMediator(repository)
            UtsendingMottak(
                rapidsConnection = rapid,
                utsendingMediator = utsendingMediator,
            )
            utsendingMediator.mottaBrev(VedtaksbrevHendelse(oppgaveId, "vedtaksbrev.html"))

            var utsending = repository.hent(oppgaveId)
            utsending.oppgaveId shouldBe oppgaveId

            utsending.tilstand().type shouldBe VenterPåVedtak
            utsending.brev() shouldBe "vedtaksbrev.html"

            rapid.sendTestMessage(
                """
                {
                    "@event_name": "vedtak_fattet",
                    "søknadId": "${UUID.randomUUID()}",
                    "behandlingId": "$behandlingId",
                    "ident": "12345678901"
                }
                """,
            )

            utsending = repository.hent(oppgaveId)
            utsending.tilstand().type shouldBe AvventerArkiverbarVersjonAvBrev
        }
    }

    private fun lagreOppgaveOgBehandling(dataSource: DataSource): Pair<UUID, UUID> {
        val behandling = lagBehandling()
        val oppgave = lagOppgave(behandling = behandling)
        val repository = PostgresOppgaveRepository(dataSource)
        repository.lagre(oppgave)
        return Pair(oppgave.oppgaveId, behandling.behandlingId)
    }
}
