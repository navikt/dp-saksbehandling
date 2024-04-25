package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
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
                    get("/IllegalStateException") { throw IllegalStateException() }
                }
            }

            client.get("/IllegalArgumentException").status shouldBe HttpStatusCode.BadRequest
            client.get("/DataNotFoundException").status shouldBe HttpStatusCode.NotFound
            client.get("/DateTimeParseException").status shouldBe HttpStatusCode.BadRequest
            client.get("/IllegalStateException").status shouldBe HttpStatusCode.Conflict
        }
    }
}
