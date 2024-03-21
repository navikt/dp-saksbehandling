package no.nav.dagpenger.saksbehandling.db

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.db.Postgres.withCleanDb
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.runMigration
import org.junit.jupiter.api.Test

class PostgresMigrationTest {
    @Test
    fun `Migration scripts are applied successfully`() {
        withCleanDb {
            val migrations = runMigration()
            migrations shouldBe 3
        }
    }
}
