package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
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
    // required to setup jwt
    private val mockAzure = mockAzure()

    @Test
    fun `Skal mappe exceptions til riktig statuskode`() {
        testApplication {
            application {
                apiConfig()
                routing {
                    get("/IllegalArgumentException") { throw IllegalArgumentException() }
                    get("/DataNotFoundException") { throw DataNotFoundException("test") }
                    get("/DateTimeParseException") { throw DateTimeParseException("test", "test", 1) }
                    get("/UkjentTilstandException") { throw Tilstand.UkjentTilstandException("test") }
                    get("/UlovligTilstandsendringException") { throw UlovligTilstandsendringException("test") }
                    get("/InternDataException") { throw InternDataException("test") }
                }
            }

            client.get("/IllegalArgumentException").status shouldBe HttpStatusCode.BadRequest
            client.get("/DataNotFoundException").status shouldBe HttpStatusCode.NotFound
            client.get("/DateTimeParseException").status shouldBe HttpStatusCode.BadRequest
            client.get("/UkjentTilstandException").status shouldBe HttpStatusCode.InternalServerError
            client.get("/UlovligTilstandsendringException").status shouldBe HttpStatusCode.Conflict
            client.get("/InternDataException").status shouldBe HttpStatusCode.InternalServerError
        }
    }
}
