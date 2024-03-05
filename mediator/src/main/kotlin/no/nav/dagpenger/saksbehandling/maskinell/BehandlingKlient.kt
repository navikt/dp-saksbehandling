package no.nav.dagpenger.saksbehandling.maskinell

import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendEncodedPathSegments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import java.util.UUID

class BehandlingKlient(
    private val behandlingUrl: String,
    private val behandlingScope: String,
    private val tokenProvider: (token: String, audience: String) -> String,
    engine: HttpClientEngine = CIO.create {},
) {
    private val client = createHttpClient(engine)

    suspend fun hentBehandling(
        behandlingId: UUID,
        saksbehandlerToken: String,
    ): BehandlingDTO =
        withContext(Dispatchers.IO) {
            val url = URLBuilder(behandlingUrl).appendEncodedPathSegments("behandling", behandlingId.toString()).build()
            try {
                val response: HttpResponse =
                    client.get(url) {
                        header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke(saksbehandlerToken, behandlingScope)}")
                        accept(ContentType.Application.Json)
                    }

                sikkerLogger.info { "Response fra dp-behandling ved GET behandlingId $behandlingId: $response" }
                return@withContext response.body<BehandlingDTO>()
            } catch (e: Exception) {
                logger.warn("GET kall til dp-behandling feilet for behandlingId $behandlingId", e)
                throw e
            }
        }

    suspend fun bekreftBehandling(
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
                        header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke(saksbehandlerToken, behandlingScope)}")
                        accept(ContentType.Application.Json)
                    }

                sikkerLogger.info { "Response fra dp-behandling ved POST bekreftelse av behandlingId $behandlingId: $response" }
            } catch (e: Exception) {
                logger.warn("POST kall til dp-behandling feilet for bekreftelse av behandlingId $behandlingId", e)
                throw e
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerLogger = KotlinLogging.logger("tjenestekall")
    }
}
