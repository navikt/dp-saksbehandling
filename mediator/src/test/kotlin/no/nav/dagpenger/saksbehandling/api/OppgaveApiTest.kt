package no.nav.dagpenger.saksbehandling.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.json.objectMapper
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class OppgaveApiTest {
    val testIdent = "13083826694"
    private val testToken by mockAzure {
        claims = mapOf("NAVident" to "123")
    }
    private val oppgaveId = UUID.randomUUID()

    @Test
    fun `Skal kunne hente ut alle oppgaver`() {
        val oppgaveId = UUIDv7.ny()
        val opprettet = LocalDateTime.now()
        val mediatorMock =
            mockk<Mediator>().also {
                every { it.hentAlleOppgaver() } returns
                    mutableListOf(
                        Oppgave(
                            oppgaveId = oppgaveId,
                            ident = "123",
                            opprettet = opprettet,
                        ),
                    )
            }
        val forventetOppgaveDTO =
            OppgaveDTO(
                uuid = oppgaveId,
                personIdent = "123",
                datoOpprettet = opprettet.toLocalDate(),
                tilstand = OppgaveTilstandDTO.TilBehandling,
                journalpostIder = listOf(),
                emneknagger = listOf(),
                steg = listOf(),
            )

        withOppgaveApi(mediator = mediatorMock) {
            client.get("/oppgave") { autentisert() }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val oppgaver =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        object : TypeReference<List<OppgaveDTO>>() {},
                    )
                oppgaver.shouldContain(oppgaveTilBehandlingDTO)
                oppgaver.shouldContain(oppgaveFerdigBehandletDTO)
                oppgaver.shouldContain(forventetOppgaveDTO)
            }
        }
    }

    @Test
    fun `Skal kunne hente ut en oppgave av type TilBehandling med gitt id`() {
        val mediatorMock: Mediator = mockk<Mediator>()
        val oppgaveId = UUIDv7.ny()
        val oppgave =
            Oppgave(
                oppgaveId = oppgaveId,
                opprettet = LocalDateTime.now(),
                ident = "12345612345",
                emneknagger = setOf("Søknadsbehandling"),
            )
        every { mediatorMock.hent(oppgaveId) } returns oppgave

        withOppgaveApi(mediator = mediatorMock) {
            client.get("/oppgave/$oppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val actualOppgave =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        OppgaveDTO::class.java,
                    )
                actualOppgave shouldBe oppgave.tilOppgaveDTO()
            }
        }
    }

    @Test
    fun `Skal kunne hente ut en oppgave av type FerdigBehandlet med gitt id`() {
        withOppgaveApi {
            client.get("/oppgave/$oppgaveFerdigBehandletUUID") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val oppgave =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        OppgaveDTO::class.java,
                    )
                oppgave shouldBe oppgaveFerdigBehandletDTO
            }
        }
    }

    @Test
    fun `Skal kunne motta info om utført steg`() {
        withOppgaveApi {
            client.put("/oppgave/$oppgaveFerdigBehandletUUID/steg/$stegIdGjenopptak8Uker") {
                autentisert()
                contentType(ContentType.Application.Json)
                setBody(objectMapper.writeValueAsString(opplysningerGjenopptak8uker))
            }.also { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }
        }
    }

    @Test
    fun `Skal kunne avslå på bakgrunn av kravet til minsteinntekt`() {
        withOppgaveApi {
            client.put("/oppgave/$oppgaveTilBehandlingUUID/avslag") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }
        }
    }

    @Test
    fun `Skal kunne lukke oppgave`() {
        withOppgaveApi {
            client.put("/oppgave/$oppgaveTilBehandlingUUID/lukk") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NoContent
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
                    """{"fnr": $testIdent}""",
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

    private fun HttpRequestBuilder.autentisert() {
        header(HttpHeaders.Authorization, "Bearer $testToken")
    }
}
