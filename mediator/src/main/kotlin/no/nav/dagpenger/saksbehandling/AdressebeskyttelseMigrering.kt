package no.nav.dagpenger.saksbehandling

import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder
import no.nav.dagpenger.saksbehandling.pdl.PDLHttpKlient

internal val pdlKlient = PDLHttpKlient(
    url = Configuration.pdlUrl,
    tokenSupplier = Configuration.pdlTokenProvider,
)

private val logger = KotlinLogging.logger { }

suspend fun adressebeskyttelseMigrering() {
    logger.info { "Starter migrering av adressebeskyttelse" }

    val alleIdenter = sessionOf(PostgresDataSourceBuilder.dataSource).use { session ->
        session.run(
            queryOf("SELECT DISTINCT ident FROM person_v1").map {
                it.string("ident")
            }.asList
        )
    }.also {
        logger.info { "Hentet ${it.size} unike identer fra databasen" }
    }

    alleIdenter.forEach { ident ->
        val adresseBeskyttelse = pdlKlient.person(ident).getOrThrow().adresseBeskyttelseGradering
        sessionOf(PostgresDataSourceBuilder.dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                    """
                        UPDATE person_v1
                        SET    adressebeskyttelse_gradering = :adresseBeskyttelseGradering
                        WHERE  ident = :fnr
                        """.trimIndent(),
                    paramMap =
                    mapOf(
                        "fnr" to ident,
                        "adresseBeskyttelseGradering" to adresseBeskyttelse.name,
                    ),
                ).asUpdate,
            )
        }
    }
    logger.info { "Migrering av adressebeskyttelse ferdig" }
}
