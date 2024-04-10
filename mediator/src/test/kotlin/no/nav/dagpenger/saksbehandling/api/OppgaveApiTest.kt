package no.nav.dagpenger.saksbehandling.api

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.assertions.throwables.shouldThrow
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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import no.nav.dagpenger.saksbehandling.api.models.KjonnDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonDTO
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class OppgaveApiTest {
    val testIdent = "13083826694"
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
    fun `Henter ut beriket oppgave`() {
        val mediatorMock = mockk<Mediator>()
        val oppgaveId = UUIDv7.ny()
        val oppgave = testOppgaveFerdigBehandlet(oppgaveId)
        val fødselsdato = LocalDate.of(2000, 1, 1)

        coEvery { mediatorMock.lagOppgaveDTO(any()) } returns OppgaveDTO(
            oppgaveId = oppgaveId,
            behandlingId = oppgave.behandlingId,
            personIdent = oppgave.ident,
            person = PersonDTO(
                ident = oppgave.ident,
                fornavn = "PETTER",
                etternavn = "SMART",
                fodselsdato = fødselsdato,
                alder = ChronoUnit.YEARS.between(fødselsdato, LocalDate.now()).toInt(),
                statsborgerskap = "NOR",
                kjonn = KjonnDTO.UKJENT,
            ),
            tidspunktOpprettet = oppgave.opprettet,
            emneknagger = emptyList(),
            tilstand = OppgaveTilstandDTO.FERDIG_BEHANDLET,
            journalpostIder = emptyList(),

        )

        withOppgaveApi(mediator = mediatorMock) {
            client.get("/oppgave/$oppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                //language=JSON
                json shouldEqualSpecifiedJsonIgnoringOrder """ {
                      "behandlingId": "${oppgave.behandlingId}",
                      "personIdent": "12345612345",
                      "person": {
                        "ident": "12345612345",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "kjonn": "UKJENT",
                        "statsborgerskap": "NOR"
                      },
                      "emneknagger": [],
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
                coEvery { it.lagOppgaveDTO(any()) } returns null
            }
        withOppgaveApi(mediator) {
            client.get("/oppgave/$ikkeEksisterendeOppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldBe "Fant ingen oppgave med UUID $ikkeEksisterendeOppgaveId"
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
        val mediatorMock = mockk<Mediator>().also {
            every { it.finnOppgaverFor(testIdent) } returns listOf(
                testOppgaveFerdigBehandlet(UUIDv7.ny()),
                testOppgaveFerdigBehandlet(UUIDv7.ny()),
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
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application { oppgaveApi(mediator) }
            test()
        }
    }

    private fun HttpRequestBuilder.autentisert(token: String = gyldigToken) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private fun lagTestOppgaveMedTilstand(tilstand: Oppgave.Tilstand.Type): Oppgave {
        return Oppgave(
            oppgaveId = UUIDv7.ny(),
            ident = "12345612345",
            emneknagger = setOf("Søknadsbehandling"),
            opprettet = ZonedDateTime.now(),
            behandlingId = UUIDv7.ny(),
            tilstand = tilstand,
        )
    }

    private fun testOppgaveFerdigBehandlet(
        oppgaveId: UUID,
        opprettet: ZonedDateTime = ZonedDateTime.now(),
    ): Oppgave {
        return Oppgave(
            oppgaveId = oppgaveId,
            ident = "12345612345",
            emneknagger = setOf("Søknadsbehandling"),
            opprettet = opprettet,
            behandlingId = UUIDv7.ny(),
            tilstand = Oppgave.Tilstand.Type.FERDIG_BEHANDLET,
        )
    }
}
