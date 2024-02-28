package no.nav.dagpenger.saksbehandling.api

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.assertions.throwables.shouldThrow
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
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Steg
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class OppgaveApiTest {
    val testIdent = "13083826694"
    private val testToken by mockAzure {
        claims = mapOf("NAVident" to "123")
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
                oppgaver.size shouldBe 2
                oppgaver[0].oppgaveId shouldBe oppgaveTilBehandlingUUID
                oppgaver[1].oppgaveId shouldBe oppgaveFerdigBehandletUUID
            }
        }
    }

    @Test
    fun `Når saksbehandler henter en oppgave, oppdater den med steg og opplysninger`() {
        val mediatorMock = mockk<Mediator>()
        val oppgaveId = UUIDv7.ny()
        val oppgave = testOppgaveMedSteg(oppgaveId)

        coEvery { mediatorMock.oppdaterOppgaveMedSteg(any()) } returns oppgave

        withOppgaveApi(mediator = mediatorMock) {
            client.get("/oppgave/$oppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val actualOppgave =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        OppgaveDTO::class.java,
                    )
                actualOppgave.steg.size shouldBe 1
                actualOppgave.steg[0].stegNavn shouldBe "Teststeg"
            }
        }
    }

    @Test
    fun `Får 404 Not Found ved forsøk på å hente oppgave som ikke finnes`() {
        val ikkeEksisterendeOppgaveId = UUIDv7.ny()
        val mediator =
            mockk<Mediator>().also {
                coEvery { it.oppdaterOppgaveMedSteg(any()) } returns null
            }
        withOppgaveApi(mediator) {
            client.get("/oppgave/$ikkeEksisterendeOppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldBe "Fant ingen oppgave med UUID $ikkeEksisterendeOppgaveId"
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
                oppgaver.size shouldBe 2
                oppgaver.first() sammenlign oppgaveDtos.first()
                oppgaver.last() sammenlign oppgaveDtos.last()
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

    private fun testOppgaveMedSteg(
        oppgaveId: UUID,
        opprettet: ZonedDateTime = ZonedDateTime.now(),
    ) = Oppgave(
        oppgaveId = oppgaveId,
        ident = "12345612345",
        emneknagger = setOf("Søknadsbehandling"),
        opprettet = opprettet,
        behandlingId = UUIDv7.ny(),
    ).also {
        it.steg.add(
            Steg("Teststeg", emptyList()),
        )
    }

    private infix fun OppgaveDTO.sammenlign(expected: OppgaveDTO) {
        this.oppgaveId shouldBe expected.oppgaveId
        this.behandlingId shouldBe expected.behandlingId
        this.personIdent shouldBe expected.personIdent
        // Vi er kun interresert i at tidspunktet er lik, uavhengig av tidssone
        this.tidspunktOpprettet.isEqual(expected.tidspunktOpprettet) shouldBe true
        this.emneknagger shouldBe expected.emneknagger
        this.tilstand shouldBe expected.tilstand
        this.steg shouldBe expected.steg
        this.journalpostIder shouldBe expected.journalpostIder
    }
}
