package no.nav.dagpenger.saksbehandling.pdl

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.jackson
import mu.KotlinLogging
import no.nav.dagpenger.pdl.PDLPerson.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.pdl.PDLPerson.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.pdl.PDLPerson.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.pdl.PDLPerson.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.pdl.createPersonOppslag

private val sikkerLogg = KotlinLogging.logger("tjenestekall")

internal fun defaultHttpClient(engine: HttpClientEngine = CIO.create {}) =
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

internal class PdlPerson(
    url: String,
    private val tokenSupplier: () -> String,
    httpClient: HttpClient = defaultHttpClient(),
) {
    private val hentPersonClient =
        createPersonOppslag(
            url = "$url/graphql",
            httpClient = httpClient,
        )

    internal suspend fun erAdressebeskyttet(ident: String): Result<Boolean> {
        try {
            val invoke = tokenSupplier.invoke()
            val adresseBeskyttelse = hentPersonClient.hentPerson(
                ident,
                mapOf(
                    HttpHeaders.Authorization to "Bearer $invoke",
//                    HttpHeaders.XRequestId to MDC.get("behovId"),
//                    "Nav-Call-Id" to MDC.get("behovId"),
                    // https://behandlingskatalog.intern.nav.no/process/purpose/DAGPENGER/486f1672-52ed-46fb-8d64-bda906ec1bc9
                    "behandlingsnummer" to "B286",
                    "TEMA" to "DAG",
                ),
            ).adresseBeskyttelse
            return when (adresseBeskyttelse) {
                FORTROLIG -> Result.success(true)
                STRENGT_FORTROLIG -> Result.success(true)
                STRENGT_FORTROLIG_UTLAND -> Result.success(true)
                UGRADERT -> Result.success(false)
            }
        } catch (e: Exception) {
            sikkerLogg.error(e) { "Feil i adressebeskyttelse-oppslag for person med id $ident" }
            return Result.failure(e)
        }
    }
}
