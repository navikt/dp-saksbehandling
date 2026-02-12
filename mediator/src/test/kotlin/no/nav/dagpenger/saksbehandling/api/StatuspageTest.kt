package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.ManglendeBeslutterTilgang
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UlovligTilstandsendringException
import no.nav.dagpenger.saksbehandling.behandling.BehandlingException
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.klage.UgyldigOpplysningException
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
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
                mockApi()
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
    fun `Error håndtering av KanIkkeLageMeldingOmVedtak`() {
        testApplication {
            val message = "Kan ikke lage melding om vedtak"
            val path = "/KanIkkeLageMeldingOmVedtak"
            application {
                mockApi()
                routing {
                    get(path) { throw MeldingOmVedtakKlient.KanIkkeLageMeldingOmVedtak(message) }
                }
            }

            client.get(path).let { response ->
                response.status shouldBe HttpStatusCode.InternalServerError
                response.bodyAsText() shouldEqualSpecifiedJson
                    //language=JSON
                    """
                    {
                      "type": "dagpenger.nav.no/saksbehandling:problem:feil-lag-melding-om-vedtak",
                      "title": "Feil ved laging av melding om vedtak",
                      "detail": "$message",
                      "status": 500,
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
                mockApi()
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
                mockApi()
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
                mockApi()
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
    fun `Error håndtering av UgyldigOpplysningException`() {
        testApplication {
            val path = "/UgyldigOpplysningException"
            application {
                mockApi()
                routing {
                    get(path) {
                        throw UgyldigOpplysningException(
                            Opplysning(
                                type = OpplysningType.KLAGEN_GJELDER,
                                verdi = Verdi.TomVerdi,
                            ),
                        )
                    }
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
                      "detail": "Ugyldig opplysning: Hva klagen gjelder med verdi TomVerdi",
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
                mockApi()
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
            val message = "Feil jved parsing av dato/tid"
            val path = "/DateTimeParseException"
            application {
                mockApi()
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
                mockApi()
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
                mockApi()
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
    fun `Error håndtering av BehandlingException`() {
        val path = "/BehandlingException"
        val httpProblem = """
                    {
                      "type": "dagpenger.nav.no/saksbehandling:problem:behandling-feil",
                      "title": "Feil ved kall mot dp-behandling",
                      "detail": "403",
                      "status": 403,
                      "instance": "instance"
                    }
                    """
        testApplication {
            application {
                mockApi()
                routing {
                    get("/BehandlingException") { throw BehandlingException(httpProblem, 403) }
                }
            }

            client.get(path).let { response ->
                response.status.value shouldBe 403
                response.bodyAsText() shouldEqualSpecifiedJson
                    //language=JSON
                    httpProblem.trimIndent()
            }
        }
    }

    private fun Application.mockApi() {
        installerApis(
            oppgaveMediator = mockk(),
            oppgaveDTOMapper = mockk(),
            produksjonsstatistikkRepository = mockk(),
            klageMediator = mockk(),
            klageDTOMapper = mockk(),
            personMediator = mockk(),
            sakMediator = mockk(),
            innsendingMediator = mockk(),
        )
    }
}
