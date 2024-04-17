package no.nav.dagpenger.saksbehandling.api

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
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
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.db.DataNotFoundException
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class OppgaveApiTest {
    private val testIdent = "12345612345"
    private val mockAzure = mockAzure()
    private val gyldigToken = mockAzure.lagTokenMedClaims(mapOf("groups" to listOf("SaksbehandlerADGruppe")))

    @Test
    fun `Skal avvise kall uten autoriserte AD grupper`() {
        withOppgaveApi {
            client.get("/oppgave") { autentisert(token = mockAzure.lagTokenMedClaims(mapOf("groups" to "UgyldigADGruppe"))) }
                .status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Skal kunne hente ut alle oppgaver`() {
        val mediatorMock = mockk<Mediator>().also {
            every { it.hentOppgaverKlarTilBehandling() } returns listOf(
                lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
                lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
            )
        }

        withOppgaveApi(mediatorMock) {
            client.get("/oppgave") { autentisert() }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val oppgaver =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        object : TypeReference<List<OppgaveOversiktDTO>>() {},
                    )
                oppgaver.size shouldBe 2
            }
        }
    }

    @Test
    fun `Skal kunne ta en oppgave til behandling`() {
        val mediatorMock = mockk<Mediator>()
        val pdlMock = mockk<PDLKlient>()
        val fødselsdato = LocalDate.of(2000, 1, 1)
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)

        coEvery { mediatorMock.tildelOppgave(any<TildelOppgaveHendelse>()) } returns testOppgave
        withOppgaveApi(mediatorMock) {
            client.put("/oppgave/${testOppgave.oppgaveId}/behandle") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
            }
        }
    }

    @Test
    fun `Henter en spesifikk oppgave med tilhørende personinfo`() {
        val mediatorMock = mockk<Mediator>()
        val pdlMock = mockk<PDLKlient>()
        val testOppgave = lagTestOppgaveMedTilstand(FERDIG_BEHANDLET)
        val fødselsdato = LocalDate.of(2000, 1, 1)

        coEvery { mediatorMock.hentOppgave(any()) } returns testOppgave
        coEvery { pdlMock.person(any()) } returns Result.success(
            PDLPersonIntern(
                ident = "12345612345",
                fornavn = "PETTER",
                etternavn = "SMART",
                mellomnavn = null,
                fødselsdato = fødselsdato,
                alder = 0,
                statsborgerskap = "NOR",
                kjønn = PDLPerson.Kjonn.UKJENT,
            ),
        )
        withOppgaveApi(mediator = mediatorMock, pdlKlient = pdlMock) {
            client.get("/oppgave/${testOppgave.oppgaveId}") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                //language=JSON
                json shouldEqualSpecifiedJsonIgnoringOrder """ {
                      "behandlingId": "${testOppgave.behandlingId}",
                      "personIdent": "12345612345",
                      "person": {
                        "ident": "12345612345",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "kjonn": "UKJENT",
                        "statsborgerskap": "NOR"
                      },
                      "emneknagger": ["Søknadsbehandling"],
                      "tilstand": "FERDIG_BEHANDLET"
                      }
                """.trimIndent()
            }
        }
    }

    @Test
    fun `Får 404 Not Found ved forsøk på å hente oppgave som ikke finnes`() {
        val ikkeEksisterendeOppgaveId = UUIDv7.ny()
        val mediator =
            mockk<Mediator>().also {
                coEvery { it.hentOppgave(any()) } throws DataNotFoundException("Fant ikke testoppgave")
            }
        withOppgaveApi(mediator) {
            client.get("/oppgave/$ikkeEksisterendeOppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NotFound
            }
        }
    }

    @Test
    fun `Får feil ved ugyldig oppgaveId`() {
        val ugyldigId = "noeSomIkkeKanParsesTilUUID"
        withOppgaveApi {
            client.get("/oppgave/$ugyldigId") { autentisert() }.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Skal kunne hente ut alle oppgaver for en gitt person`() {
        val mediatorMock = mockk<Mediator>().also {
            every { it.finnOppgaverFor(testIdent) } returns listOf(
                lagTestOppgaveMedTilstand(FERDIG_BEHANDLET),
                lagTestOppgaveMedTilstand(FERDIG_BEHANDLET),
            )
        }
        withOppgaveApi(mediatorMock) {
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
                        object : TypeReference<List<OppgaveOversiktDTO>>() {},
                    )
                oppgaver.size shouldBe 2
            }
        }
    }

    private fun withOppgaveApi(
        mediator: Mediator = mockk<Mediator>(relaxed = true),
        pdlKlient: PDLKlient = mockk(relaxed = true),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application { oppgaveApi(mediator, pdlKlient) }
            test()
        }
    }

    private fun HttpRequestBuilder.autentisert(token: String = gyldigToken) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private fun lagTestOppgaveMedTilstand(tilstand: Oppgave.Tilstand.Type): Oppgave {
        return Oppgave(
            oppgaveId = UUIDv7.ny(),
            ident = testIdent,
            emneknagger = setOf("Søknadsbehandling"),
            opprettet = ZonedDateTime.now(),
            behandlingId = UUIDv7.ny(),
            tilstand = tilstand,
        )
    }
}
