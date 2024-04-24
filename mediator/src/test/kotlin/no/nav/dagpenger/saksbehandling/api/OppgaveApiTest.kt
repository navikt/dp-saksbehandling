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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.db.DataNotFoundException
import no.nav.dagpenger.saksbehandling.db.Søkefilter
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class OppgaveApiTest {
    private val testIdent = "12345612345"
    private val testNAVIdent = "Z999999"
    private val fødselsdato = LocalDate.of(2000, 1, 1)
    private val mockAzure = mockAzure()
    private val gyldigToken = mockAzure.lagTokenMedClaims(
        mapOf(
            "groups" to listOf("SaksbehandlerADGruppe"),
            "NAVident" to testNAVIdent,
        ),
    )

    @Test
    fun `Skal avvise kall uten autoriserte AD grupper`() {
        withOppgaveApi {
            client.get("/oppgave") { autentisert(token = mockAzure.lagTokenMedClaims(mapOf("groups" to "UgyldigADGruppe"))) }
                .status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Hent alle oppgaver klar til behandling hvis ingen query parametere er gitt`() {
        val mediatorMock = mockk<Mediator>().also {
            every { it.søk(Søkefilter.DEFAULT_SØKEFILTER) } returns listOf(
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
    fun `Hent alle oppgaver med tilstander basert på query parameter`() {
        val mediatorMock = mockk<Mediator>().also {
            every {
                it.søk(
                    Søkefilter(
                        periode = Søkefilter.Periode.UBEGRENSET_PERIODE,
                        tilstand = setOf(KLAR_TIL_BEHANDLING, UNDER_BEHANDLING),
                    ),
                )
            } returns listOf(
                lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
                lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
            )
        }

        withOppgaveApi(mediatorMock) {
            client.get("/oppgave?tilstand=KLAR_TIL_BEHANDLING&tilstand=UNDER_BEHANDLING") { autentisert() }
                .let { response ->
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
    fun `Hent alle oppgaver fom, tom, mine  og tilstand`() {
        val mediatorMock = mockk<Mediator>().also {
            every {
                it.søk(
                    Søkefilter(
                        periode = Søkefilter.Periode(
                            fom = LocalDate.parse("2021-01-01"),
                            tom = LocalDate.parse("2023-01-01"),
                        ),
                        tilstand = setOf(UNDER_BEHANDLING),
                        saksbehandlerIdent = testNAVIdent,
                    ),
                )
            } returns listOf(
                lagTestOppgaveMedTilstand(UNDER_BEHANDLING),
                lagTestOppgaveMedTilstand(UNDER_BEHANDLING),
            )
        }

        withOppgaveApi(mediatorMock) {
            client.get("/oppgave?tilstand=$UNDER_BEHANDLING&fom=2021-01-01&tom=2023-01-01&mineOppgaver=true") { autentisert() }
                .let { response ->
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
    fun `Skal kunne hente og få tildelt neste oppgave`() {
        val oppgave = lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING)
        oppgave.tilstand = UNDER_BEHANDLING
        oppgave.saksbehandlerIdent = testNAVIdent
        val mediatorMock = mockk<Mediator>().also {
            every { it.hentNesteOppgavenTil(testNAVIdent) } returns oppgave
        }
        val pdlMock = mockk<PDLKlient>()
        coEvery { pdlMock.person(any()) } returns Result.success(testPerson)

        withOppgaveApi(mediatorMock, pdlMock) {
            client.put("/oppgave/neste") { autentisert() }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                //language=JSON
                json shouldEqualSpecifiedJsonIgnoringOrder """ {
                      "behandlingId": "${oppgave.behandlingId}",
                      "personIdent": "$testIdent",
                      "person": {
                        "ident": "$testIdent",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "kjonn": "UKJENT",
                        "statsborgerskap": "NOR"
                      },
                      "emneknagger": ["Søknadsbehandling"],
                      "tilstand": "${OppgaveTilstandDTO.UNDER_BEHANDLING}"
                      }
                """.trimIndent()
            }
        }
    }

    @Test
    fun `404 når det ikke finnes noen neste oppgave for saksbehandler`() {
        val mediatorMock = mockk<Mediator>().also {
            every { it.hentNesteOppgavenTil(testNAVIdent) } returns null
        }
        val pdlMock = mockk<PDLKlient>()

        withOppgaveApi(mediatorMock, pdlMock) {
            client.put("/oppgave/neste") { autentisert() }.status shouldBe HttpStatusCode.NotFound
        }
        coVerify(exactly = 0) { pdlMock.person(any()) }
    }

    @Test
    fun `Saksbehandler skal kunne ta en oppgave`() {
        val mediatorMock = mockk<Mediator>()
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)

        coEvery {
            mediatorMock.tildelOppgave(
                OppgaveAnsvarHendelse(
                    oppgaveId = testOppgave.oppgaveId,
                    navIdent = testNAVIdent,
                ),
            )
        } returns testOppgave
        val pdlMock = mockk<PDLKlient>()
        coEvery { pdlMock.person(any()) } returns Result.success(testPerson)

        withOppgaveApi(mediatorMock, pdlMock) {
            client.put("/oppgave/${testOppgave.oppgaveId}/tildel") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                //language=JSON
                json shouldEqualSpecifiedJsonIgnoringOrder """ {
                      "behandlingId": "${testOppgave.behandlingId}",
                      "personIdent": "$testIdent",
                      "person": {
                        "ident": "$testIdent",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "kjonn": "UKJENT",
                        "statsborgerskap": "NOR"
                      },
                      "emneknagger": ["Søknadsbehandling"],
                      "tilstand": "${OppgaveTilstandDTO.UNDER_BEHANDLING}"
                      }
                """.trimIndent()
            }
        }
    }

    @Test
    fun `Saksbehandler skal kunne gi fra seg ansvar for en oppgave`() {
        val mediatorMock = mockk<Mediator>()
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)

        coEvery { mediatorMock.hentOppgave(any()) } returns testOppgave
        coEvery {
            mediatorMock.fristillOppgave(
                OppgaveAnsvarHendelse(
                    testOppgave.oppgaveId,
                    testNAVIdent,
                ),
            )
        } just runs

        withOppgaveApi(mediator = mediatorMock) {
            client.put("oppgave/${testOppgave.oppgaveId}/legg-tilbake") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }
        }

        verify(exactly = 1) {
            mediatorMock.fristillOppgave(
                OppgaveAnsvarHendelse(
                    testOppgave.oppgaveId,
                    testNAVIdent,
                ),
            )
        }
    }

    @Test
    fun `Hent oppgave med tilhørende personinfo`() {
        val mediatorMock = mockk<Mediator>()
        val pdlMock = mockk<PDLKlient>()
        val testOppgave = lagTestOppgaveMedTilstand(FERDIG_BEHANDLET)

        coEvery { mediatorMock.hentOppgave(any()) } returns testOppgave
        coEvery { pdlMock.person(any()) } returns Result.success(testPerson)

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
                      "tilstand": "${OppgaveTilstandDTO.FERDIG_BEHANDLET}"
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

    @Test
    fun `Skal hente oppgaveId basert på behandlingId`() {
        val behandlingIdSomFinnes = UUIDv7.ny()
        val behandlingIdSomIkkeFinnes = UUIDv7.ny()

        val oppgaveId = UUIDv7.ny()
        val mediatorMock = mockk<Mediator>().also {
            every { it.hentOppgaveIdFor(behandlingIdSomFinnes) } returns oppgaveId
            every { it.hentOppgaveIdFor(behandlingIdSomIkkeFinnes) } returns null
        }
        withOppgaveApi(mediator = mediatorMock) {
            client.get("/behandling/$behandlingIdSomFinnes/oppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "text/plain"
                response.bodyAsText() shouldBe "$oppgaveId"
            }

            client.get("/behandling/$behandlingIdSomIkkeFinnes/oppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NotFound
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

    private fun lagTestOppgaveMedTilstand(
        tilstand: Oppgave.Tilstand.Type,
        saksbehandlerIdent: String? = null,
    ): Oppgave {
        return Oppgave.rehydrer(
            oppgaveId = UUIDv7.ny(),
            ident = testIdent,
            emneknagger = setOf("Søknadsbehandling"),
            opprettet = ZonedDateTime.now(),
            behandlingId = UUIDv7.ny(),
            tilstand = tilstand,
            saksbehandlerIdent = saksbehandlerIdent,
        )
    }

    private val testPerson = PDLPersonIntern(
        ident = testIdent,
        fornavn = "PETTER",
        etternavn = "SMART",
        mellomnavn = null,
        fødselsdato = fødselsdato,
        alder = 0,
        statsborgerskap = "NOR",
        kjønn = PDLPerson.Kjonn.UKJENT,
    )
}
