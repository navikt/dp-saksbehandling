package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UlovligTilstandsendringException
import no.nav.dagpenger.saksbehandling.api.config.apiConfig
import no.nav.dagpenger.saksbehandling.db.DataNotFoundException
import org.junit.jupiter.api.Test
import java.time.format.DateTimeParseException

class StatuspageTest {
    init {
        // required to setup jwt
        mockAzure()
    }

    @Test
    fun `Error håndtering av DataNotFoundException`() {
        testApplication {
            val message = "Fant ikke oppgave med id"
            val path = "/v1/oppgave/id/DataNotFoundException"
            application {
                apiConfig()
                routing {
                    get(path) { throw DataNotFoundException(message) }
                }
            }

            client.get(path).let { response ->
                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldEqualSpecifiedJson
                    //language=JSON
                    """
                    {
                      "type": "dagpenger.nav.no/saksbehandling:problem:ressurs-ikke-funnet",
                      "title": "Ressurs ikke funnet",
                      "detail": "$message",
                      "status": 404,
                      "instance": "$path"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Error håndtering av UgyldigTilstandException`() {
        testApplication {
            val message = "Kunne ikke rehydrere med ugyldig tilstand"
            val path = "/UkjentTilstandException"
            application {
                apiConfig()
                routing {
                    get(path) { throw Tilstand.UgyldigTilstandException(message) }
                }
            }

            client.get(path).let { response ->
                response.status shouldBe HttpStatusCode.InternalServerError
                response.bodyAsText() shouldEqualSpecifiedJson
                    //language=JSON
                    """
                    {
                      "type": "dagpenger.nav.no/saksbehandling:problem:ugyldig-oppgavetilstand",
                      "title": "Ugyldig oppgavetilstand",
                      "detail": "$message",
                      "status": 500,
                      "instance": "$path"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Skal mappe exceptions til riktig statuskode`() {
        testApplication {
            application {
                apiConfig()
                routing {
                    get("/IllegalArgumentException") { throw IllegalArgumentException() }
                    get("/DateTimeParseException") { throw DateTimeParseException("test", "test", 1) }
                    get("/UlovligTilstandsendringException") { throw UlovligTilstandsendringException("test") }
                }
            }

            client.get("/IllegalArgumentException").status shouldBe HttpStatusCode.BadRequest
            client.get("/DateTimeParseException").status shouldBe HttpStatusCode.BadRequest
            client.get("/UlovligTilstandsendringException").status shouldBe HttpStatusCode.Conflict
        }
    }
}
