package no.nav.dagpenger.saksbehandling.db

import com.zaxxer.hikari.HikariDataSource
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
            PostgresDataSourceBuilder.runMigration()
            block(PostgresDataSourceBuilder.dataSource)
        }
    }

    fun withMigratedDb(): HikariDataSource {
        setup()
        PostgresDataSourceBuilder.runMigration()
        return PostgresDataSourceBuilder.dataSource
    }

    fun setup() {
        System.setProperty(PostgresDataSourceBuilder.DB_URL_KEY, instance.jdbcUrl)
        System.setProperty(PostgresDataSourceBuilder.DB_USERNAME_KEY, instance.databaseName)
        System.setProperty(PostgresDataSourceBuilder.DB_PASSWORD_KEY, instance.password)
        System.setProperty(PostgresDataSourceBuilder.DB_USERNAME_KEY, instance.username)
    }

    fun tearDown() {
        System.clearProperty(PostgresDataSourceBuilder.DB_PASSWORD_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_USERNAME_KEY)
        System.setProperty(PostgresDataSourceBuilder.DB_URL_KEY, instance.jdbcUrl)
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
