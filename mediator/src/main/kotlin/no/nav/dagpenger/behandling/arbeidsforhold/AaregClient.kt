package no.nav.dagpenger.behandling.arbeidsforhold

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendEncodedPathSegments
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.behandling.Configuration
import no.nav.dagpenger.behandling.api.models.ArbeidsforholdDTO
import no.nav.helse.rapids_rivers.asLocalDate

internal class AaregClient(
    private val baseUrl: String = Configuration.aaregUrl,
    private val aaregScope: String = Configuration.aaregScope,
    private val tokenProvider: (token: String, audience: String) -> String = tilOboToken,
    engine: HttpClientEngine = CIO.create {},
) {
    private val httpClient =
        HttpClient(engine) {
            expectSuccess = true

            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
        }

    fun hentArbeidsforhold(
        fnr: String,
        subjectToken: String,
    ): List<ArbeidsforholdDTO> =
        runBlocking {
            val url = URLBuilder(baseUrl).appendEncodedPathSegments(API_PATH, ARBEIDSFORHOLD_PATH).build()

            try {
                val response: HttpResponse =
                    httpClient.get(url) {
                        header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke(subjectToken, aaregScope)}")
                        header("Nav-Personident", fnr)
                        parameter("arbeidsforholdstatus", "AKTIV, AVSLUTTET")
                        parameter("historikk", "true")
                    }
                if (response.status.value == 200) {
                    logger.info("Kall til AAREG gikk OK")
                    val arbeidsforhold = jacksonObjectMapper().readTree(response.bodyAsText())

                    arbeidsforhold.map {
                        val id = it["id"].asText()
                        val orgnr = it["arbeidssted"]["identer"][0]["ident"].asText()
                        val ansettelsesform = it["ansettelsesdetaljer"][0]["ansettelsesform"]?.get("beskrivelse")?.asText()
                        val ansettelsestype =
                            ansettelsesform ?: it["ansettelsesdetaljer"][0]["type"].asText()
                        val yrke = it["ansettelsesdetaljer"][0]["yrke"].get("beskrivelse")?.asText()
                        val startdato = it["ansettelsesperiode"]["startdato"].asLocalDate()
                        val sluttdato = it["ansettelsesperiode"].get("sluttdato")?.asLocalDate()
                        val sluttaarsak = it["ansettelsesperiode"]["sluttaarsak"]?.get("beskrivelse")?.asText()

                        ArbeidsforholdDTO(id, orgnr, ansettelsestype, startdato, yrke, sluttdato, sluttaarsak)
                    }
                } else {
                    logger.warn("Kall til AAREG feilet med status ${response.status}")
                    emptyList()
                }
            } catch (e: ClientRequestException) {
                logger.warn("Kall til AAREG feilet", e)
                emptyList()
            }
        }

    companion object {
        private const val API_PATH = "api"
        private const val ARBEIDSFORHOLD_PATH = "v2/arbeidstaker/arbeidsforhold"
        private val tilOboToken = { token: String, scope: String ->
            Configuration.azureAdClient.onBehalfOf(token, scope).accessToken
        }
        private val logger = KotlinLogging.logger {}
    }
}
