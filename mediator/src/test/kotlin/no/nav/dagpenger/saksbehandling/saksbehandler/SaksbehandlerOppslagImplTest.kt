package no.nav.dagpenger.saksbehandling.saksbehandler

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.HistogramSnapshot
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.SaksbehandlerEnhetDTO
import no.nav.dagpenger.saksbehandling.getSnapShot
import org.junit.jupiter.api.Test

class SaksbehandlerOppslagImplTest {
    val enhet =
        SaksbehandlerEnhetDTO(
            navn = "Test Enhet",
            enhetNr = "1234",
            postadresse = "Postboks 5678, Test, 1234 Oslo",
        )
    private val saksbehandler =
        SaksbehandlerDTO(
            ident = "navIdent",
            fornavn = "sonet",
            etternavn = "electram",
            enhet = enhet,
        )

    private val mockTokenProvider = { "mockToken" }

    private val mockEngine =
        MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1.0/users" ->
                    respond(
                        content =
                            """
                            {
                                "value": [
                                    {
                                        "givenName": "${saksbehandler.fornavn}",
                                        "surname": "${saksbehandler.etternavn}",
                                        "streetAddress": "${enhet.enhetNr}"
                                    }
                                ]
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )

                "/api/v1/enhet/${enhet.enhetNr}" ->
                    respond(
                        content = """{"navn": "${enhet.navn}"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )

                "/api/v1/enhet/1234/kontaktinformasjon" ->
                    respond(
                        content =
                            """{"postadresse": {"postnummer": "1234", "poststed": "Oslo", "type": "B",
                            | "postboksnummer": "5678", "postboksanlegg": "Test"}}
                            """.trimMargin(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )

                else -> respondError(HttpStatusCode.NotFound)
            }
        }

    private val registry = PrometheusRegistry()

    private val saksbehandlerOppslag =
        SaksbehandlerOppslagImpl(
            norgBaseUrl = "http://localhost",
            msGraphBaseUrl = "http://localhost",
            tokenProvider = mockTokenProvider,
            prometheusRegistry = registry,
            engine = mockEngine,
        )

    @Test
    fun `test hentSaksbehandler happy days `(): Unit =
        runBlocking {
            val result: SaksbehandlerDTO = saksbehandlerOppslag.hentSaksbehandler(saksbehandler.ident)
            result shouldBe saksbehandler
            registry.getSnapShot<HistogramSnapshot> {
                it == "dp_saksbehahandling_saksbehandler_oppslag_duration"
            }.dataPoints.size shouldBe 1
        }
}
