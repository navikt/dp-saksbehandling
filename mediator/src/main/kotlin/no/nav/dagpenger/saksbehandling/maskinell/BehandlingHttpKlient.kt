package no.nav.dagpenger.saksbehandling.maskinell

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendEncodedPathSegments
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import no.nav.dagpenger.saksbehandling.createHttpClient
import java.util.UUID

class BehandlingHttpKlient(
    private val behandlingUrl: String,
    private val behandlingScope: String,
    private val tokenProvider: (token: String, audience: String) -> String,
    engine: HttpClientEngine = CIO.create {},
) : BehandlingKlient {
    private val client = createHttpClient(engine)

    override suspend fun hentBehandling(
        behandlingId: UUID,
        saksbehandlerToken: String,
    ): Pair<BehandlingDTO, Map<String, Any>> =
        withContext(Dispatchers.IO) {
            val url = URLBuilder(behandlingUrl).appendEncodedPathSegments("behandling", behandlingId.toString()).build()
            try {
                val response: HttpResponse =
                    client.get(url) {
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${tokenProvider.invoke(saksbehandlerToken, behandlingScope)}",
                        )
                        accept(ContentType.Application.Json)
                    }

                sikkerLogger.info { "Response fra dp-behandling ved GET behandlingId $behandlingId: $response" }
                val rawBehandlingResponse = response.bodyAsText()
                val behandlingDto = objectMapper.readValue<BehandlingDTO>(rawBehandlingResponse)
                val behandlingAsMap = objectMapper.readValue<Map<String, Any>>(rawBehandlingResponse)
                return@withContext Pair(behandlingDto, behandlingAsMap)
            } catch (e: Exception) {
                logger.warn("GET kall til dp-behandling feilet for behandlingId $behandlingId", e)
                throw e
            }
        }

    override suspend fun bekreftBehandling(
        behandlingId: UUID,
        saksbehandlerToken: String,
    ) {
        withContext(Dispatchers.IO) {
            val url =
                URLBuilder(behandlingUrl).appendEncodedPathSegments(
                    "behandling",
                    behandlingId.toString(),
                    "oppplysning",
                    "bekreftelse",
                ).build()
            try {
                val response: HttpResponse =
                    client.post(url) {
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${tokenProvider.invoke(saksbehandlerToken, behandlingScope)}",
                        )
                        accept(ContentType.Application.Json)
                    }

                sikkerLogger.info { "Response fra dp-behandling ved POST bekreftelse av behandlingId $behandlingId: $response" }
            } catch (e: Exception) {
                logger.warn("POST kall til dp-behandling feilet for bekreftelse av behandlingId $behandlingId", e)
                throw e
            }
        }
    }

    override suspend fun godkjennBehandling(behandlingId: UUID, ident: String, saksbehandlerToken: String) {
        withContext(Dispatchers.IO) {
            val url = "$behandlingUrl/behandling/$behandlingId/godkjenn"
            try {
                val response: HttpResponse =
                    client.post(urlString = url) {
                        header(
                            HttpHeaders.Authorization,
                            "Bearer ${tokenProvider.invoke(saksbehandlerToken, behandlingScope)}",
                        )
                        setBody("""{"ident": "$ident"}""")
                        contentType(ContentType.Application.Json)
                    }
            } catch (e: Exception) {
                logger.warn("POST kall til dp-behandling feilet for godkjenning av behandlingId $behandlingId", e)
                throw e
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerLogger = KotlinLogging.logger("tjenestekall")
    }
}
