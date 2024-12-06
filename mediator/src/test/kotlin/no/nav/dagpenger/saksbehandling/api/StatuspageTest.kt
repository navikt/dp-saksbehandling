package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.ManglendeBeslutterTilgang
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UlovligTilstandsendringException
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.statusPages
import no.nav.dagpenger.saksbehandling.behandling.GodkjennBehandlingFeiletException
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
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
                statusPages()
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
                statusPages()
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
    fun `Error håndtering av UlovligTilstandsendringException`() {
        testApplication {
            val message = "Kan ikke håndtere hendelsen i denne tilstanden"
            val path = "/UlovligTilstandsendringException"
            application {
                statusPages()
                routing {
                    get(path) { throw UlovligTilstandsendringException(message) }
                }
            }

            client.get(path).let { response ->
                response.status shouldBe HttpStatusCode.Conflict
                response.bodyAsText() shouldEqualSpecifiedJson
                    //language=JSON
                    """
                    {
                      "type": "dagpenger.nav.no/saksbehandling:problem:oppgave-ulovlig-tilstandsendring",
                      "title": "Ulovlig tilstandsendring på oppgave",
                      "detail": "$message",
                      "status": 409,
                      "instance": "$path"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Error håndtering av IllegalArgumentException`() {
        testApplication {
            val message = "Kunne ikke finne oppgaveId i path"
            val path = "/IllegalArgumentException"
            application {
                statusPages()
                routing {
                    get(path) { throw IllegalArgumentException(message) }
                }
            }

            client.get(path).let { response ->
                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldEqualSpecifiedJson
                    //language=JSON
                    """
                    {
                      "type": "dagpenger.nav.no/saksbehandling:problem:ugyldig-verdi",
                      "title": "Ugyldig verdi",
                      "detail": "$message",
                      "status": 400,
                      "instance": "$path"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Error håndtering av ManglendeTilgang`() {
        testApplication {
            val message = "Mangler tilgang"
            val path = "/ManglendeTilgang"
            application {
                statusPages()
                routing {
                    get(path) { throw ManglendeBeslutterTilgang(message) }
                }
            }

            client.get(path).let { response ->
                response.status shouldBe HttpStatusCode.Forbidden
                response.bodyAsText() shouldEqualSpecifiedJson
                    //language=JSON
                    """
                    {
                      "type": "dagpenger.nav.no/saksbehandling:problem:manglende-tilgang",
                      "title": "ManglendeBeslutterTilgang",
                      "detail": "$message",
                      "status": 403,
                      "instance": "$path"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Error håndtering av DateTimeParseException`() {
        testApplication {
            val message = "Feil ved parsing av dato/tid"
            val path = "/DateTimeParseException"
            application {
                statusPages()
                routing {
                    get(path) {
                        throw DateTimeParseException(message, "syttende mai 2024 klokka 19:43", 1)
                    }
                }
            }

            client.get(path).let { response ->
                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldEqualSpecifiedJson
                    //language=JSON
                    """
                    {
                      "type": "dagpenger.nav.no/saksbehandling:problem:dato-tid-feil",
                      "title": "Dato/tid feil",
                      "detail": "$message",
                      "status": 400,
                      "instance": "$path"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Error håndtering ved uhåndterte feil`() {
        testApplication {
            val message = "Uhåndtert feil i koden"
            val path = "/Exception"
            application {
                statusPages()
                routing {
                    get(path) {
                        throw RuntimeException(message)
                    }
                }
            }

            client.get(path).let { response ->
                response.status shouldBe HttpStatusCode.InternalServerError
                response.bodyAsText() shouldEqualSpecifiedJson
                    //language=JSON
                    """
                    {
                      "type": "dagpenger.nav.no/saksbehandling:problem:uhåndtert-feil",
                      "title": "Uhåndtert feil",
                      "detail": "$message",
                      "status": 500,
                      "instance": "$path"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Error håndtering av AlleredeTildeltException`() {
        testApplication {
            val message = "Oppgaven eies av noen andre"
            val path = "/AlleredeTildeltException"
            application {
                statusPages()
                routing {
                    get(path) { throw Oppgave.AlleredeTildeltException(message) }
                }
            }

            client.get(path).let { response ->
                response.status shouldBe HttpStatusCode.Conflict
                response.bodyAsText() shouldEqualSpecifiedJson
                    //language=JSON
                    """
                    {
                      "type": "dagpenger.nav.no/saksbehandling:problem:oppgave-eies-av-annen-behandler",
                      "title": "Oppgaven eies av en annen. Operasjon kan ikke utføres.",
                      "detail": "$message",
                      "status": 409,
                      "instance": "$path"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Error håndtering av GodkjennBehandlingFeiletException`() {
        testApplication {
            val message = "Kall mot dp-behandling feilet"
            val path = "/GodkjennBehandlingFeiletException"
            application {
                statusPages()
                routing {
                    get(path) { throw GodkjennBehandlingFeiletException(message) }
                }
            }

            client.get(path).let { response ->
                response.status shouldBe HttpStatusCode.InternalServerError
                response.bodyAsText() shouldEqualSpecifiedJson
                    //language=JSON
                    """
                    {
                      "type": "dagpenger.nav.no/saksbehandling:problem:godkjenning-av-behandling-feilet",
                      "title": "Godkjenning av behandling feilet",
                      "detail": "$message",
                      "status": 500,
                      "instance": "$path"
                    }
                    """.trimIndent()
            }
        }
    }
}
