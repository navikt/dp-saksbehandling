package no.nav.dagpenger.saksbehandling.klage

import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.lagBehandling
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.lagPerson
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KlageHttpKlientTest {
    @Test
    fun `Oversend klage til klageinstans`() {
        val klageBehandling = lagKlagebehandling()
        val fagsakId = UUIDv7.ny().toString()

        val oppgave =
            lagOppgave(
                behandling =
                    lagBehandling(
                        person = lagPerson(ident = "11111111111"),
                        behandlingId = klageBehandling.behandlingId,
                        type = BehandlingType.KLAGE,
                    ),
                opprettet = LocalDateTime.now(),
            )
        var requestBody: String? = null
        val mockEngine =
            MockEngine { request ->
                request.headers[HttpHeaders.Authorization] shouldBe "Bearer token"
                request.url.host shouldBe "localhost"
                request.url.segments shouldBe listOf("api", "oversendelse", "v4", "sak")
                requestBody = request.body.toByteArray().toString(Charsets.UTF_8)
                respondOk("")
            }

        val kabalKlient =
            KlageHttpKlient(
                kabalApiUrl = "http://localhost:8080",
                tokenProvider = { "token" },
                httpClient =
                    httpClient(
                        prometheusRegistry = PrometheusRegistry(),
                        engine = mockEngine,
                    ),
            )
        val resultat: Result<HttpStatusCode> =
            runBlocking {
                kabalKlient.registrerKlage(
                    klageBehandling = klageBehandling,
                    personIdentId = oppgave.behandling.person.ident,
                    // TODO: Hvor skal vi hente sak fra?
                    fagsakId = fagsakId,
                    // TODO: Skal være klagebehandlers enhet. Bør kunne hentes fra klageBehandling?
                    forrigeBehandlendeEnhet = "4408",
                )
            }
        resultat shouldBe Result.success(HttpStatusCode.OK)
        requestBody?.shouldEqualSpecifiedJsonIgnoringOrder(
            (
                """
                {
                  "type": "KLAGE",
                  "sakenGjelder": {
                    "id": {
                      "type": "PERSON",
                      "verdi": "11111111111"
                    }
                  },
                  "prosessFullmektig": {
                    "navn": "Djevelens Advokat",
                    "adresse": {
                      "addresselinje1": "Sydenveien 1",
                      "addresselinje2": "Poste restante",
                      "addresselinje3": "Teisen postkontor",
                      "postnummer": "0666",
                      "poststed": "Oslo",
                      "land": "NO"
                    }
                  },
                  "fagsak": {
                    "fagsakId": "$fagsakId",
                    "fagsystem": "DAGPENGER"
                  },
                  "kildeReferanse": "${klageBehandling.behandlingId}",
                  "hjemler": [
                    "FTRL_4_2",
                    "FTRL_4_9",
                    "FTRL_4_18"
                  ],
                  "forrigeBehandlendeEnhet": "4408",
                  "tilknyttedeJournalposter": [],
                  "ytelse": "DAG_DAG"
                }
                """.trimIndent()
            ),
        )
    }
}
