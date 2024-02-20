package no.nav.dagpenger.saksbehandling.maskinell

import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
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

                return@withContext response.body<BehandlingDTO>()
            } catch (e: Exception) {
                logger.warn("Kall til dp-behandling feilet", e)
                throw e
            }
        }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
