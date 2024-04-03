package no.nav.dagpenger.saksbehandling.maskinell

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
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

    override suspend fun hentBehandling(behandlingId: UUID, saksbehandlerToken: String): Map<String, Any> {
        val urlString = "$behandlingUrl/behandling/$behandlingId"
        return kotlin.runCatching {
            client.get(urlString = urlString) {
                header(
                    HttpHeaders.Authorization,
                    "Bearer ${tokenProvider.invoke(saksbehandlerToken, behandlingScope)}",
                )
                accept(ContentType.Application.Json)
            }
        }.map {
            objectMapper.readValue<Map<String, Any>>(it.bodyAsText())
        }.getOrElse {
            logger.warn("GET kall til $urlString feilet. BehandlingId $behandlingId", it)
            throw it
        }
    }

    override suspend fun godkjennBehandling(behandlingId: UUID, ident: String, saksbehandlerToken: String): Int {
        return oppdaterBehandling(behandlingId, ident, saksbehandlerToken, "godkjenn")
    }

    override suspend fun avbrytBehandling(behandlingId: UUID, ident: String, saksbehandlerToken: String): Int {
        return oppdaterBehandling(behandlingId, ident, saksbehandlerToken, "avbryt")
    }

    private suspend fun oppdaterBehandling(
        behandlingId: UUID,
        ident: String,
        saksbehandlerToken: String,
        endepunkt: String,
    ): Int {
        return withContext(Dispatchers.IO) {
            val url = "$behandlingUrl/behandling/$behandlingId/$endepunkt"
            try {
                client.post(urlString = url) {
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${tokenProvider.invoke(saksbehandlerToken, behandlingScope)}",
                    )
                    setBody("""{"ident": "$ident"}""")
                    contentType(ContentType.Application.Json)
                }.status.value
            } catch (e: Exception) {
                logger.warn("POST kall til $url feilet. BehandlingId $behandlingId", e)
                throw e
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerLogger = KotlinLogging.logger("tjenestekall")
    }
}
