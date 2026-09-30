package no.nav.dagpenger.saksbehandling.klage

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.klage.PostgresKlageRepository
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KlageAlarmJobRepositoryTest {
    @Test
    fun `henter bare behandlinger i angitt tilstand som er eldre enn grensen`() {
        val førOpprettelse = LocalDateTime.now().minusMinutes(1)
        val etterOpprettelse = LocalDateTime.now().plusMinutes(1)
        val tilstand = KlageBehandling.KlageTilstand.Type.BEHANDLES_AV_KLAGEINSTANS

        withMigratedDb { dataSource ->
            val databaseSession = DatabaseSession(dataSource)
            val klageRepository = PostgresKlageRepository(databaseSession)
            val behandling1 = lagKlagebehandling(tilstand = tilstand)
            val behandling2 = lagKlagebehandling(tilstand = tilstand)
            val behandling3 = lagKlagebehandling(tilstand = tilstand)
            val annenTilstandBehandling =
                lagKlagebehandling(tilstand = KlageBehandling.KlageTilstand.Type.FERDIGSTILT)

            listOf(behandling1, behandling2, behandling3, annenTilstandBehandling).forEach(klageRepository::lagre)

            klageRepository.hentBehandlingerIkkeFerdigstilt(tilstand, førOpprettelse).size shouldBe 0
            val behandlinger = klageRepository.hentBehandlingerIkkeFerdigstilt(tilstand, etterOpprettelse)

            behandlinger.size shouldBe 3
            behandlinger.map { it.behandlingId }.toSet() shouldBe
                setOf(behandling1.behandlingId, behandling2.behandlingId, behandling3.behandlingId)
            behandlinger.forEach {
                it.tilstand shouldBe tilstand.name
                it.sistEndret.isBefore(etterOpprettelse) shouldBe true
            }
        }
    }
}
