package no.nav.dagpenger.behandling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import no.nav.dagpenger.behandling.oppgave.InMemoryOppgaveRepository
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.dagpenger.behandling.prosess.Arbeidsprosess
import no.nav.dagpenger.behandling.prosess.Arbeidsprosess.Overgang
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class OppgaveApiTest {
    private var oppgaveId: UUID
    private val testPerson1 = Person("12345678910")

    @Test
    fun `skal ikke json serialisere null verdier`() {
        withOppgaveApi {
            client.get("/oppgave/$oppgaveId").also { response ->
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
            client.get("/oppgave").let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val oppgaver = jacksonObjectMapper().readTree(response.bodyAsText())
                oppgaver.size() shouldBe 6
            }
        }
    }

    @Test
    fun `skal kunne svare på et steg`() {
        withOppgaveApi {
            val oppgaveJSON: String = client.get("/oppgave/$oppgaveId").bodyAsText()
            val stegId = oppgaveJSON.findStegUUID("vilkår1")
            val oppgave = mockPersistence.hentOppgave(oppgaveId)
            val steg = oppgave.steg(UUID.fromString(stegId))

            steg.svar.verdi shouldBe null
            steg.tilstand shouldBe Tilstand.IkkeUtført

            client.put("/oppgave/$oppgaveId/steg/$stegId") {
                contentType(ContentType.Application.Json)
                this.setBody(
                    //language=JSON
                    """{"type":"Boolean","svar":true,"begrunnelse":{"tekst":"Har itte","kilde":"Høggern"}}""",
                )
            }.status shouldBe HttpStatusCode.OK

            steg.svar.verdi shouldBe true
            steg.tilstand shouldBe Tilstand.Utført
        }
    }

    @Test
    fun `Skal kunne hente ut en oppgave med en gitt id`() {
        withOppgaveApi {
            client.get("/oppgave/$oppgaveId").also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val oppgave = jacksonObjectMapper().readTree(response.bodyAsText())
                oppgave.isObject shouldBe true
                oppgave["uuid"].asText() shouldBe oppgaveId.toString()
            }
        }
    }

    @Test
    fun `Får feil ved ugyldig oppgaveId`() {
        val ugyldigId = "noeSomIkkeKanParsesTilUUID"
        withOppgaveApi {
            shouldThrow<IllegalArgumentException> {
                client.get("/oppgave/$ugyldigId")
            }
        }
    }

    @Test
    fun `Får 404 Not Found ved forsøk på å hente oppgave som ikke finnes`() {
        val randomUUID = UUID.randomUUID()
        withOppgaveApi {
            client.get("/oppgave/$randomUUID").also { response ->
                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldBe "Fant ingen oppgave med UUID $randomUUID"
            }
        }
    }

    @Test
    fun `Skal kunne hente ut alle oppgaver for en gitt person`() {
        withOppgaveApi {
            client.post("/oppgave/sok") {
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """{"fnr": ${testPerson1.ident}}""",
                )
            }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val oppgaver = jacksonObjectMapper().readTree(response.bodyAsText())
                oppgaver.size() shouldBe 3
            }
        }
    }

    @Test
    fun `Får 200 OK og tom liste dersom det ikke finnes oppgaver for et gitt fnr`() {
        withOppgaveApi {
            client.post("/oppgave/sok") {
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

    @Test
    fun `Skal kunne ferdigstille en oppgave`() {
        withOppgaveApi {
            val oppgave = mockPersistence.hentOppgave(oppgaveId)
            oppgave.tilstand() shouldBe "TilBehandling"
            fattet shouldBe false

            client.post("/oppgave/$oppgaveId/tilstand") {
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """{"nyTilstand": "Innstilt"}""",
                )
            }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                fattet shouldBe false
            }
            client.post("/oppgave/$oppgaveId/tilstand") {
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """{"nyTilstand": "Fattet"}""",
                )
            }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                oppgave.tilstand() shouldBe "Fattet"
                fattet shouldBe true
            }
        }
    }

    private fun withOppgaveApi(
        mediator: Mediator = Mediator(
            rapidsConnection = TestRapid(),
            oppgaveRepository = mockPersistence,
        ),
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

    private var fattet = false
    private val mockPersistence = InMemoryOppgaveRepository().apply {
        val fakeProsess = Arbeidsprosess().apply {
            leggTilTilstand("TilBehandling", Overgang("Innstilt"))
            leggTilTilstand("Innstilt", Overgang("Fattet", vedOvergang = { fattet = true }))
            leggTilTilstand("Fattet", emptyList())
            start("TilBehandling")
        }
        lagreOppgave(
            Oppgave(
                behandling(testPerson1) {
                    steg {
                        vilkår("vilkår1") {
                            avhengerAvFastsettelse<LocalDate>("vilkår 1 dato")
                        }
                    }
                    steg {
                        fastsettelse<Int>("fastsettelse1")
                    }
                },
                fakeProsess,
            ).also { oppgaveId = it.uuid },
        )
        lagreOppgave(
            Oppgave(
                behandling(testPerson1) {
                    steg {
                        vilkår("vilkår2")
                    }
                },
                fakeProsess,
            ),
        )
        lagreOppgave(
            Oppgave(
                behandling(Person("45678910112")) {
                    steg {
                        vilkår("vilkår3")
                    }
                },
                fakeProsess,
            ),
        )
    }
}
