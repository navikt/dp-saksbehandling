package no.nav.dagpenger.saksbehandling.api

import com.fasterxml.jackson.core.type.TypeReference
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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.maskinell.BehandlingKlient
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID

class OppgaveApiTest {
    val testIdent = "13083826694"
    private val testToken by mockAzure {
        claims = mapOf("NAVident" to "123")
    }

    @Test
    fun `Skal kunne hente ut alle oppgaver`() {
        val oppgaveId = UUIDv7.ny()
        val opprettet = ZonedDateTime.now()
        val behandlingId = UUIDv7.ny()
        val mediatorMock =
            mockk<Mediator>().also {
                every { it.hentAlleOppgaver() } returns
                    mutableListOf(
                        Oppgave(
                            oppgaveId = oppgaveId,
                            ident = "123",
                            opprettet = opprettet.toLocalDateTime(),
                            behandlingId = behandlingId,
                        ),
                    )
            }

        withOppgaveApi(mediator = mediatorMock) {
            client.get("/oppgave") { autentisert() }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val oppgaver =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        object : TypeReference<List<OppgaveDTO>>() {},
                    )
                oppgaver shouldContain oppgaveTilBehandlingDTO
                oppgaver.shouldContain(oppgaveFerdigBehandletDTO)
            }
        }
    }

    @Test
    fun `Når saksbehandler henter en oppgave, oppdater den med steg og opplysninger`() {
        val behandlingKlient = mockk<BehandlingKlient>()
        val mediatorMock = Mediator(personRepository = mockk(relaxed = true), behandlingKlient = behandlingKlient)
        val oppgaveId = UUIDv7.ny()
        val oppgave = testOppgave(oppgaveId)
        val oppdaterOppgaveHendelse = OppdaterOppgaveHendelse(oppgaveId, "en signatur")

        coEvery { mediatorMock.oppdaterOppgaveMedSteg(oppdaterOppgaveHendelse) } returns oppgave

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
    fun `Får 404 Not Found ved forsøk på å hente oppgave som ikke finnes`() {
        val ikkeEksisterendeOppgaveId = UUID.randomUUID()
        val mediator = mockk<Mediator>()
        every { mediator.hent(ikkeEksisterendeOppgaveId) }.returns(null)
        withOppgaveApi(mediator) {
            client.get("/oppgave/$ikkeEksisterendeOppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldBe "Fant ingen oppgave med UUID $ikkeEksisterendeOppgaveId"
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
    fun `Får feil ved ugyldig oppgaveId`() {
        val ugyldigId = "noeSomIkkeKanParsesTilUUID"
        withOppgaveApi {
            shouldThrow<IllegalArgumentException> {
                client.get("/oppgave/$ugyldigId") { autentisert() }
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
        mediator: Mediator = mockk<Mediator>(relaxed = true),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application { oppgaveApi(mediator) }
            test()
        }
    }

    private fun HttpRequestBuilder.autentisert() {
        header(HttpHeaders.Authorization, "Bearer $testToken")
    }

    private fun testOppgave(oppgaveId: UUID) =
        Oppgave(
            oppgaveId = oppgaveId,
            ident = "12345612345",
            emneknagger = setOf("Søknadsbehandling"),
            opprettet = LocalDateTime.now(),
            behandlingId = UUIDv7.ny(),
        )
}
