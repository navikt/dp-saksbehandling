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
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetricsPlugin
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.pdl.PDLPerson.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.pdl.PDLPerson.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.pdl.PDLPerson.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.pdl.PDLPerson.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.pdl.createPersonOppslag
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger { }
private val sikkerLogg = KotlinLogging.logger("tjenestekall")

internal class PDLHttpKlient(
    url: String,
    private val tokenSupplier: () -> String,
    httpClient: HttpClient = defaultHttpClient(),
) : PDLKlient {
    private val hentPersonClient =
        createPersonOppslag(
            url = "$url/graphql",
            httpClient = httpClient,
        )

    override suspend fun erAdressebeskyttet(ident: String): Result<Boolean> {
        return hentPerson(ident)
            .map {
                when (it.adresseBeskyttelse) {
                    FORTROLIG -> true
                    STRENGT_FORTROLIG -> true
                    STRENGT_FORTROLIG_UTLAND -> true
                    UGRADERT -> false
                }
            }
    }

    override suspend fun person(ident: String): Result<PDLPersonIntern> {
        return hentPerson(ident)
            .map { pdlPerson ->
                PDLPersonIntern(
                    ident = pdlPerson.fodselnummer,
                    fornavn = pdlPerson.fornavn,
                    etternavn = pdlPerson.etternavn,
                    mellomnavn = pdlPerson.mellomnavn,
                    alder = pdlPerson.alder.toInt(),
                    statsborgerskap = pdlPerson.statsborgerskap,
                    kjønn = pdlPerson.kjonn,
                    fødselsdato = pdlPerson.fodselsdato,
                )
            }
    }

    private suspend fun hentPerson(ident: String): Result<PDLPerson> {
        return kotlin.runCatching {
            val invoke = tokenSupplier.invoke()
            measureTimedValue {
                hentPersonClient.hentPerson(
                    ident,
                    mapOf(
                        HttpHeaders.Authorization to "Bearer $invoke",
                        // https://behandlingskatalog.intern.nav.no/process/purpose/DAGPENGER/486f1672-52ed-46fb-8d64-bda906ec1bc9
                        "behandlingsnummer" to "B286",
                        "TEMA" to "DAG",
                    ),
                )
            }.let { timedValue ->
                logger.info { "Henting av personopplysninger tok ${timedValue.duration.inWholeMilliseconds} ms" }
                timedValue.value
            }
        }.onFailure { e -> sikkerLogg.error(e) { "Feil ved henting av personopplysninger for ident $ident" } }
    }
}

internal fun defaultHttpClient(engine: HttpClientEngine = CIO.create {}) =
    HttpClient(engine) {
        expectSuccess = true
        install(PrometheusMetricsPlugin) {
        }

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
