package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.db.Postgres.withCleanDb
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.runMigration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PostgresMigrationTest {
    @Test
    fun `Migration scripts are applied successfully`() {
        withCleanDb {
            val migrations = runMigration()
            Assertions.assertEquals(9, migrations)
        }
    }
}
