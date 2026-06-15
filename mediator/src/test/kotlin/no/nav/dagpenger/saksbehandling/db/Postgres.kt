package no.nav.dagpenger.saksbehandling.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.dagpenger.saksbehandling.Configuration
import org.flywaydb.core.internal.configuration.ConfigUtils
import org.testcontainers.postgresql.PostgreSQLContainer
import javax.sql.DataSource

internal object Postgres {
    val instance by lazy {
        PostgreSQLContainer("postgres:18.1").apply {
            start()
        }
    }

    fun withMigratedDb(block: (ds: DataSource) -> Unit) {
        withCleanDb {
            PostgresDataSourceBuilder.runMigration(configuration = Configuration)
            block(PostgresDataSourceBuilder.dataSource)
        }
    }

    fun withMigratedDb(): HikariDataSource {
        setup()
        PostgresDataSourceBuilder.runMigration(configuration = Configuration)
        return PostgresDataSourceBuilder.dataSource
    }

    fun setup() {
        System.setProperty(PostgresDataSourceBuilder.DB_HOST_KEY, instance.host)
        System.setProperty(
            PostgresDataSourceBuilder.DB_PORT_KEY,
            instance.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT).toString(),
        )
        System.setProperty(PostgresDataSourceBuilder.DB_DATABASE_KEY, instance.databaseName)
        System.setProperty(PostgresDataSourceBuilder.DB_PASSWORD_KEY, instance.password)
        System.setProperty(PostgresDataSourceBuilder.DB_USERNAME_KEY, instance.username)
    }

    fun tearDown() {
        System.clearProperty(PostgresDataSourceBuilder.DB_PASSWORD_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_USERNAME_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_HOST_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_PORT_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_DATABASE_KEY)
        System.clearProperty(ConfigUtils.CLEAN_DISABLED)
    }

    fun withCleanDb(block: () -> Unit) {
        setup()
        PostgresDataSourceBuilder
            .clean()
            .run {
                block()
            }.also {
                tearDown()
            }
    }
}
