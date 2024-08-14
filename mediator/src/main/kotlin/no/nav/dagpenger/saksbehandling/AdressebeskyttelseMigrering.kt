package no.nav.dagpenger.saksbehandling

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.pdl.PDLHttpKlient

private fun httpKlient(engine: HttpClientEngine = CIO.create {}) =
    HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            jackson {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                setSerializationInclusion(JsonInclude.Include.NON_NULL)
                registerModules(JavaTimeModule())
            }
        }

        install(HttpRequestRetry) {
            retryOnException(maxRetries = 5)
            this.constantDelay(
                millis = 100,
                randomizationMs = 0,
            )
        }
    }

private val pdlKlient =
    PDLHttpKlient(
        url = Configuration.pdlUrl,
        tokenSupplier = Configuration.pdlTokenProvider,
        httpClient = httpKlient(),
    )

private val logger = KotlinLogging.logger { }

suspend fun adressebeskyttelseMigrering() {
    logger.info { "Starter migrering av adressebeskyttelse" }

    val alleIdenter =
        sessionOf(PostgresDataSourceBuilder.dataSource).use { session ->
            session.run(
                queryOf("SELECT DISTINCT ident FROM person_v1").map {
                    it.string("ident")
                }.asList,
            )
        }.also {
            logger.info { "Hentet ${it.size} unike identer fra databasen" }
        }

    val repo = PostgresOppgaveRepository(dataSource = PostgresDataSourceBuilder.dataSource)

    alleIdenter.forEach { ident ->
        val adresseBeskyttelse = pdlKlient.person(ident).getOrThrow().adresseBeskyttelseGradering
        repo.oppdaterAdressebeskyttetStatus(ident, adresseBeskyttelse)
    }
    logger.info { "Migrering av adressebeskyttelse ferdig" }
}
