package no.nav.dagpenger.saksbehandling.maskinell

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendEncodedPathSegments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Configuration
import java.util.UUID

class BehandlingKlient(
    private val behandlingUrl: String = Configuration.behandlingUrl,
    private val tokenProvider: (String) -> String,
    engine: HttpClientEngine = CIO.create {},
) {
    private val client = createHttpClient(engine)

    suspend fun hentBehandling(
        behandlingId: UUID,
        subjectToken: String,
    ): JsonNode =
        withContext(Dispatchers.IO) {
            val url = URLBuilder(behandlingUrl).appendEncodedPathSegments("behandling", behandlingId.toString()).build()
            try {
                val response: HttpResponse =
                    client.get(url) {
                        header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke(subjectToken)}")
                    }
                if (response.status.value == 200) {
                    logger.info("Kall til dp-behandling gikk OK")
                    return@withContext jacksonObjectMapper().readTree(response.bodyAsText())
                } else {
                    logger.warn("Kall til dp-behandling feilet med status ${response.status}")
                    throw IllegalStateException()
                }
            } catch (e: Exception) {
                logger.warn("Kall til dp-behandling feilet", e)
                throw e
            }
        }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
