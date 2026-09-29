package no.nav.dagpenger.saksbehandling.klage

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.klage.PostgresKlageRepository
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import javax.sql.DataSource

class KlageAlarmJobRepositoryTest {
    @Test
    fun `henter bare behandlinger i angitt tilstand som er eldre enn grensen`() {
        val grense = LocalDateTime.now().minusMonths(4).truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        val eldreBehandlingId = UUIDv7.ny()
        val grenseBehandlingId = UUIDv7.ny()
        val nyereBehandlingId = UUIDv7.ny()
        val annenTilstandBehandlingId = UUIDv7.ny()
        val tilstand = KlageBehandling.KlageTilstand.Type.BEHANDLES_AV_KLAGEINSTANS

        withMigratedDb { dataSource ->
            dataSource.opprettKlage(eldreBehandlingId, tilstand.name, grense.minusSeconds(1))
            dataSource.opprettKlage(grenseBehandlingId, tilstand.name, grense)
            dataSource.opprettKlage(nyereBehandlingId, tilstand.name, grense.plusSeconds(1))
            dataSource.opprettKlage(
                annenTilstandBehandlingId,
                KlageBehandling.KlageTilstand.Type.FERDIGSTILT.name,
                grense.minusDays(1),
            )

            val behandlinger =
                PostgresKlageRepository(DatabaseSession(dataSource))
                    .hentBehandlingerIkkeFerdigstilt(tilstand, grense)

            behandlinger.size shouldBe 1
            behandlinger.single().behandlingId shouldBe eldreBehandlingId
            behandlinger.single().tilstand shouldBe tilstand.name
            behandlinger.single().sistEndret shouldBe grense.minusSeconds(1)
        }
    }

    private fun DataSource.opprettKlage(
        id: java.util.UUID,
        tilstand: String,
        endretTidspunkt: LocalDateTime,
    ) {
        sessionOf(this).use { session ->
            session.run(
                queryOf(
                    """
                    INSERT INTO klage_v1
                        (id, tilstand, registrert_tidspunkt, endret_tidspunkt, journalpost_id, behandlende_enhet, opplysninger)
                    VALUES
                        (:id, :tilstand, :tidspunkt, :tidspunkt, '123', '4408', :opplysninger)
                    """.trimIndent(),
                    mapOf(
                        "id" to id,
                        "tilstand" to tilstand,
                        "tidspunkt" to endretTidspunkt,
                        "opplysninger" to
                            PGobject().also {
                                it.type = "JSONB"
                                it.value = "{}"
                            },
                    ),
                ).asUpdate,
            )
        }
    }
}
