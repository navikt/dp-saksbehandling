package no.nav.dagpenger.behandling.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.dagpenger.behandling.Mediator
import no.nav.dagpenger.behandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.behandling.api.json.objectMapper
import no.nav.dagpenger.behandling.api.models.OppgaveDTO
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.UUID

class OppgaveApiTest {
    private val testToken by mockAzure {
        claims = mapOf("NAVident" to "123")
    }
    private val oppgaveId = UUID.randomUUID()

    @Test
    @Disabled
    fun `skal ikke json serialisere null verdier`() {
        withOppgaveApi {
            client.get("/oppgave/$oppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                response.bodyAsText().let { json ->
                    json shouldContainJsonKey "$.steg[0].svar.type"
                    json shouldNotContainJsonKey "$.steg[0].svar.svar"
                }
            }
        }
    }

    @Test
    fun `Skal kunne hente ut alle oppgaver`() {
        withOppgaveApi {
            client.get("/oppgave") { autentisert() }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val oppgaver =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        object : TypeReference<List<OppgaveDTO>>() {},
                    )
                oppgaver shouldBe oppgaveDtos
            }
        }
    }

    @Test
    @Disabled
    fun `skal kunne svare på et steg`() {
        withOppgaveApi {
            val oppgaveJSON = client.get("/oppgave/$oppgaveId") { autentisert() }.bodyAsText()
            val stegId = oppgaveJSON.findStegUUID("vilkår1")

            /*
            val oppgave = mockPersistence.hentOppgave(oppgaveId)
            val steg = oppgave.steg(UUID.fromString(stegId))
            steg.svar.verdi shouldBe null
            steg.tilstand shouldBe Tilstand.IkkeUtført
            client.put("/oppgave/$oppgaveId/steg/$stegId") {
                autentisert()
                contentType(ContentType.Application.Json)
                this.setBody(
                    //language=JSON
                    """
                    {
                      "type": "Boolean",
                      "svar": "true",
                      "begrunnelse": {
                        "tekst": "Har itte"
                      }
                    }
                    """.trimIndent(),
                )
            }.status shouldBe HttpStatusCode.OK

            steg.svar.verdi shouldBe true
            steg.svar.sporing should beInstanceOf<ManuellSporing>()
            (steg.svar.sporing as ManuellSporing).begrunnelse shouldBe "Har itte"
            steg.tilstand shouldBe Tilstand.Utført

             */
        }
    }

    @Test
    fun `Skal kunne hente ut en oppgave med en gitt id`() {
        withOppgaveApi {
            client.get("/oppgave/$oppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val oppgave =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        OppgaveDTO::class.java,
                    )
                oppgave shouldBe oppgaveDTO
            }
        }
    }

    @Test
    @Disabled
    fun `Får feil ved ugyldig oppgaveId`() {
        val ugyldigId = "noeSomIkkeKanParsesTilUUID"
        withOppgaveApi {
            shouldThrow<IllegalArgumentException> {
                client.get("/oppgave/$ugyldigId") { autentisert() }
            }
        }
    }

    @Test
    @Disabled
    fun `Får 404 Not Found ved forsøk på å hente oppgave som ikke finnes`() {
        val randomUUID = UUID.randomUUID()
        withOppgaveApi {
            client.get("/oppgave/$randomUUID") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldBe "Fant ingen oppgave med UUID $randomUUID"
            }
        }
    }

    @Test
    fun `Skal kunne hente ut alle oppgaver for en gitt person`() {
        withOppgaveApi {
            client.post("/oppgave/sok") {
                autentisert()
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """{"fnr": ${testPerson.ident}}""",
                )
            }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val oppgaver =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        object : TypeReference<List<OppgaveDTO>>() {},
                    )
                oppgaver shouldBe oppgaveDtos
            }
        }
    }

    @Test
    @Disabled
    fun `Får 200 OK og tom liste dersom det ikke finnes oppgaver for et gitt fnr`() {
        withOppgaveApi {
            client.post("/oppgave/sok") {
                autentisert()
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """{"fnr": "789"}""",
                )
            }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val oppgaver = jacksonObjectMapper().readTree(response.bodyAsText())
                oppgaver.shouldBeEmpty()
            }
        }
    }

    private fun withOppgaveApi(
        mediator: Mediator = mockk<Mediator>(),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application { oppgaveApi(mediator) }
            test()
        }
    }

    private fun String.findStegUUID(id: String): String {
        jacksonObjectMapper().readTree(this).let { root ->
            return root["steg"].first { it["id"].asText() == id }["uuid"].asText()
        }
    }

    /*
    private val mockPersistence =
        InMemoryOppgaveRepository().apply {
            val hendelse =
                SøknadInnsendtHendelse(
                    søknadId = UUID.randomUUID(),
                    journalpostId = "123",
                    ident = testIdent,
                    innsendtDato = LocalDate.now(),
                )
            testPerson.håndter(hendelse)
            lagreOppgave(
                Oppgave(
                    UUID.randomUUID(),
                    behandling(testPerson, hendelse) {
                        steg {
                            vilkår("vilkår1")
                        }
                        steg {
                            fastsettelse<Int>("fastsettelse1") {
                                avhengerAvFastsettelse<LocalDate>("vilkår 1 dato")
                            }
                        }
                    },
                    emneknagger = setOf("Søknadsbehandling", "VurderAvslagAvMinsteinntekt"),
                ).also { oppgaveId = it.uuid },
            )
            lagreOppgave(
                Oppgave(
                    UUID.randomUUID(),
                    behandling(testPerson, hendelse) {
                        steg {
                            vilkår("vilkår2")
                        }
                    },
                    emneknagger = setOf("Søknadsbehandling", "VurderAvslagAvMinsteinntekt"),
                ),
            )
            lagreOppgave(
                Oppgave(
                    UUID.randomUUID(),
                    behandling(
                        Person("45678910112").also {
                            it.håndter(hendelse)
                        },
                        hendelse,
                    ) {
                        steg {
                            vilkår("vilkår3")
                        }
                    },
                    emneknagger = setOf("Søknadsbehandling", "VurderAvslagAvMinsteinntekt"),
                ),
            )
        }


    private val mockPersistencePerson =
        InMemoryPersonRepository.apply {
            lagrePerson(testPerson)
        }

     */
    private fun HttpRequestBuilder.autentisert() {
        header(HttpHeaders.Authorization, "Bearer $testToken")
    }
}
