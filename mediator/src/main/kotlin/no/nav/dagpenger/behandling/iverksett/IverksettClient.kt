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
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.appendEncodedPathSegments
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.behandling.Configuration
import no.nav.dagpenger.behandling.Configuration.azureAdClient
import no.nav.dagpenger.kontrakter.iverksett.IverksettDto

private const val API_PATH = "api"
private const val IVERKSETTING_PATH: String = "iverksetting"

internal class IverksettClient(
    private val baseUrl: String = Configuration.dpIverksettUrl,
    private val iverksettScope: String = Configuration.dpIverksettScope,
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

            logger.info { "Prøver å sende behandling med id ${iverksettDto.behandlingId} til iverksetting" }
            val respons =
                httpClient.post(url) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${tokenProvider.invoke(subjectToken, iverksettScope)}",
                    )
                    setBody(iverksettDto)
                }

            if (respons.status == HttpStatusCode.Accepted) {
                logger.info("API kall mot iverksetting var suksessfull for behandling med id ${iverksettDto.behandlingId}")
            } else {
                throw RuntimeException("API kall mot iverksetting feilet med status: ${respons.status}")
            }
        }
    }
}

private val tilOboToken = { token: String, scope: String ->
    azureAdClient.onBehalfOf(token, scope).accessToken
}
