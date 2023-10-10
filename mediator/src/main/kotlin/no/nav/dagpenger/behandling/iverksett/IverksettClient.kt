package no.nav.dagpenger.behandling.iverksett

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.appendEncodedPathSegments
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.behandling.Configuration
import no.nav.dagpenger.behandling.Configuration.tokenXClient
import no.nav.dagpenger.kontrakter.iverksett.IverksettDto

private const val API_PATH = "api"
private const val IVERKSETTING_PATH: String = "iverksetting"

internal class IverksettClient(
    private val baseUrl: String = Configuration.dpIverksettUrl,
    private val iversettAudience: String = Configuration.dpIverksettAudience,
    private val tokenProvider: (token: String, audience: String) -> String = tilOboToken,
    engine: HttpClientEngine = CIO.create {},
) {
    private companion object {
        val logger = KotlinLogging.logger {}
    }

    private val httpClient =
        HttpClient(engine) {
            expectSuccess = true

            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                }
            }
        }

    internal fun iverksett(
        subjectToken: String,
        iverksettDto: IverksettDto,
    ) {
        runBlocking {
            val url = URLBuilder(baseUrl).appendEncodedPathSegments(API_PATH, IVERKSETTING_PATH).build()

            try {
                httpClient.post(url) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke(subjectToken, iversettAudience)}")
                    setBody(iverksettDto)
                }
            } catch (e: ClientRequestException) {
                when (e.response.status) {
                    HttpStatusCode.Accepted -> {
                        logger.info(
                            """API kall mot iverksetting var suksessfull. Statuskode: ${e.response.status} 
                            |Beskrivelse: ${e.response.status.description}
                            """.trimMargin(),
                        )
                    }

                    HttpStatusCode.BadRequest, HttpStatusCode.Forbidden, HttpStatusCode.Conflict -> {
                        // TODO: Hva vil vi at skal skje når iverksetting feiler
                        logger.warn(feilmelding(e.response))
                    }

                    else -> logger.warn("En uventet feil skjedde ved forsøk på kall mot iverksetting", e)
                }
            }
        }
    }

    private fun feilmelding(respons: HttpResponse) =
        "API kall mot iverksetting feilet. Statuskode: ${respons.status} Beskrivelse: ${respons.status.description}"
}

private val tilOboToken = { token: String, audience: String ->
    tokenXClient.tokenExchange(token, audience).accessToken
}
