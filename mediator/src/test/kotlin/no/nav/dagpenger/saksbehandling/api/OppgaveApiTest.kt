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
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.FerdigBehandlet
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.Opprettet
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.config.apiConfig
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdClient
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OppgaveApiTest {
    private val testIdent = "12345612345"
    private val testNAVIdent = "Z999999"
    private val fødselsdato = LocalDate.of(2000, 1, 1)
    private val mockAzure = mockAzure()
    private val gyldigSaksbehandlerToken =
        mockAzure.lagTokenMedClaims(
            mapOf(
                "groups" to listOf("SaksbehandlerADGruppe"),
                "NAVident" to testNAVIdent,
            ),
        )

    private val gyldigSaksbehandlerMedTilgangTilEgneAnsatteToken =
        mockAzure.lagTokenMedClaims(
            mapOf(
                "groups" to listOf("SaksbehandlerADGruppe", "EgneAnsatteADGruppe"),
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
    fun `Skal avvise kall på oppgaver tilhører egne ansatte (skjermet) dersom saksbehandler ikke har riktig adgruppe`() {
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)

        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.personSkjermesSomEgneAnsatte(any()) } returns true
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client.get("/oppgave/${testOppgave.oppgaveId}") { autentisert(token = gyldigSaksbehandlerToken) }
                .status shouldBe HttpStatusCode.Forbidden

            client.put("/oppgave/${testOppgave.oppgaveId}/tildel") { autentisert(token = gyldigSaksbehandlerToken) }
                .status shouldBe HttpStatusCode.Forbidden

            client.put("/oppgave/${testOppgave.oppgaveId}/legg-tilbake") { autentisert(token = gyldigSaksbehandlerToken) }
                .status shouldBe HttpStatusCode.Forbidden

            client.put("/oppgave/${testOppgave.oppgaveId}/utsett") { autentisert(token = gyldigSaksbehandlerToken) }
                .status shouldBe HttpStatusCode.Forbidden
        }
    }

    @Test
    fun `Skal godta kall på oppgaver som gjelder egne ansatte (skjermet) dersom saksbehandler har riktig ad-gruppe`() {
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)
        val pdlMock = mockk<PDLKlient>()
        coEvery { pdlMock.person(any()) } returns Result.success(testPerson)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.personSkjermesSomEgneAnsatte(any()) } returns true
                every { it.tildelOppgave(any()) } returns testOppgave
                every { it.hentOppgave(any()) } returns testOppgave
                every { it.fristillOppgave(any()) } just Runs
                every { it.utsettOppgave(any()) } just Runs
            }

        withOppgaveApi(oppgaveMediatorMock, pdlMock) {
            client.get("/oppgave/${testOppgave.oppgaveId}") {
                autentisert(token = gyldigSaksbehandlerMedTilgangTilEgneAnsatteToken)
            }.status shouldBe HttpStatusCode.OK

            client.put("/oppgave/${testOppgave.oppgaveId}/tildel") {
                autentisert(token = gyldigSaksbehandlerMedTilgangTilEgneAnsatteToken)
            }.status shouldBe HttpStatusCode.OK

            client.put("/oppgave/${testOppgave.oppgaveId}/legg-tilbake") {
                autentisert(token = gyldigSaksbehandlerMedTilgangTilEgneAnsatteToken)
            }.status shouldBe HttpStatusCode.NoContent

            client.put("/oppgave/${testOppgave.oppgaveId}/utsett") {
                autentisert(token = gyldigSaksbehandlerMedTilgangTilEgneAnsatteToken)
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """
                        {
                          "utsettTilDato":"${LocalDate.now()}",
                          "beholdOppgave":"true"
                        }
                    """.trimMargin(),
                )
            }
                .status shouldBe HttpStatusCode.NoContent
        }
    }

    @Test
    fun `GET på oppgaver uten query parameters`() {
        val oppgave1 = lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING, saksbehandlerIdent = testNAVIdent)
        val oppgave2 = lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING, saksbehandlerIdent = null)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.søk(
                        Søkefilter(
                            periode = Periode.UBEGRENSET_PERIODE,
                            tilstand = Oppgave.Tilstand.Type.defaultOppgaveListTilstander,
                            saksbehandlerIdent = null,
                            personIdent = null,
                            oppgaveId = null,
                            behandlingId = null,
                        ),
                    )
                } returns listOf(oppgave1, oppgave2)
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client.get("/oppgave") { autentisert() }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                //language=JSON
                json shouldEqualSpecifiedJsonIgnoringOrder
                    """
                     [{
                      "oppgaveId": "${oppgave1.oppgaveId}",
                      "behandlingId": "${oppgave1.behandlingId}",
                      "personIdent": "${oppgave1.ident}",
                      "emneknagger": [
                        "Søknadsbehandling"
                      ],
                      "tilstand": "${OppgaveTilstandDTO.KLAR_TIL_BEHANDLING}" ,
                      "saksbehandlerIdent": "${oppgave1.saksbehandlerIdent}"
                    },
                    {
                    "oppgaveId": "${oppgave2.oppgaveId}",
                    "behandlingId": "${oppgave2.behandlingId}",
                    "personIdent": "${oppgave2.ident}",
                    "emneknagger": [
                    "Søknadsbehandling"
                    ],
                      "tilstand": "${OppgaveTilstandDTO.KLAR_TIL_BEHANDLING}" 
                    }
                    ]
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Hent alle oppgaver med tilstander basert på query parameter`() {
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.søk(
                        Søkefilter(
                            periode = Periode.UBEGRENSET_PERIODE,
                            tilstand = setOf(KLAR_TIL_BEHANDLING, UNDER_BEHANDLING),
                        ),
                    )
                } returns
                    listOf(
                        lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
                        lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
                    )
            }

        withOppgaveApi(oppgaveMediatorMock) {
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
    fun `Hent alle oppgaver basert på emneknagg`() {
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.søk(
                        Søkefilter(
                            periode = Periode.UBEGRENSET_PERIODE,
                            tilstand = setOf(KLAR_TIL_BEHANDLING),
                            emneknagg = setOf("SØKNADSBEHANDLING", "KLAGE"),
                        ),
                    )
                } returns
                    listOf(
                        lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
                        lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
                    )
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client.get("/oppgave?tilstand=${KLAR_TIL_BEHANDLING}&emneknagg=SØKNADSBEHANDLING&emneknagg=KLAGE") { autentisert() }
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
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.søk(
                        Søkefilter(
                            periode =
                                Periode(
                                    fom = LocalDate.parse("2021-01-01"),
                                    tom = LocalDate.parse("2023-01-01"),
                                ),
                            tilstand = setOf(UNDER_BEHANDLING),
                            saksbehandlerIdent = testNAVIdent,
                        ),
                    )
                } returns
                    listOf(
                        lagTestOppgaveMedTilstand(UNDER_BEHANDLING),
                        lagTestOppgaveMedTilstand(UNDER_BEHANDLING),
                    )
            }

        withOppgaveApi(oppgaveMediatorMock) {
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
        val oppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING, testNAVIdent)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.tildelNesteOppgaveTil(testNAVIdent, any()) } returns oppgave
            }
        val pdlMock = mockk<PDLKlient>()
        coEvery { pdlMock.person(any()) } returns Result.success(testPerson)

        withOppgaveApi(oppgaveMediatorMock, pdlMock) {
            client.put("/oppgave/neste") {
                autentisert()
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """
                        {"queryParams":"emneknagg=knagg1&emneknagg=knagg2&fom=2021-01-01&tom=2023-01-01"}
                    """.trimMargin(),
                )
            }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                //language=JSON
                json shouldEqualSpecifiedJsonIgnoringOrder
                    """
                     {
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
                    "saksbehandlerIdent": "$testNAVIdent",
                    "emneknagger": ["Søknadsbehandling"],
                    "tilstand": "${OppgaveTilstandDTO.UNDER_BEHANDLING}"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `404 når det ikke finnes noen neste oppgave for saksbehandler`() {
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.tildelNesteOppgaveTil(testNAVIdent, any()) } returns null
            }
        val pdlMock = mockk<PDLKlient>()

        withOppgaveApi(oppgaveMediatorMock, pdlMock) {
            client.put("/oppgave/neste") {
                autentisert()
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """
                        {"queryParams":"emneknagg=knagg1&emneknagg=knagg2&fom=2021-01-01&tom=2023-01-01"}
                    """.trimMargin(),
                )
            }.status shouldBe HttpStatusCode.NotFound
        }
        coVerify(exactly = 0) { pdlMock.person(any()) }
    }

    @Test
    fun `Saksbehandler skal kunne ta en oppgave`() {
        val oppgaveMediatorMock = lagMediatorMock()
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)

        coEvery {
            oppgaveMediatorMock.tildelOppgave(
                OppgaveAnsvarHendelse(
                    oppgaveId = testOppgave.oppgaveId,
                    navIdent = testNAVIdent,
                ),
            )
        } returns testOppgave
        val pdlMock = mockk<PDLKlient>()
        coEvery { pdlMock.person(any()) } returns Result.success(testPerson)

        withOppgaveApi(oppgaveMediatorMock, pdlMock) {
            client.put("/oppgave/${testOppgave.oppgaveId}/tildel") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                //language=JSON
                json shouldEqualSpecifiedJsonIgnoringOrder
                    """
                     {
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
        val oppgaveMediatorMock = lagMediatorMock()
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)

        coEvery { oppgaveMediatorMock.hentOppgave(any()) } returns testOppgave
        coEvery {
            oppgaveMediatorMock.fristillOppgave(
                OppgaveAnsvarHendelse(
                    testOppgave.oppgaveId,
                    testNAVIdent,
                ),
            )
        } just runs

        withOppgaveApi(oppgaveMediator = oppgaveMediatorMock) {
            client.put("oppgave/${testOppgave.oppgaveId}/legg-tilbake") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }
        }

        verify(exactly = 1) {
            oppgaveMediatorMock.fristillOppgave(
                OppgaveAnsvarHendelse(
                    testOppgave.oppgaveId,
                    testNAVIdent,
                ),
            )
        }
    }

    @Test
    fun `Saksbehandler skal kunne utsette oppgave`() {
        val oppgaveMediatorMock = lagMediatorMock()
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)
        val utsettTilDato = LocalDate.now().plusDays(1)
        val utsettOppgaveHendelse =
            UtsettOppgaveHendelse(
                oppgaveId = testOppgave.oppgaveId,
                navIdent = testNAVIdent,
                utsattTil = utsettTilDato,
                beholdOppgave = true,
            )

        coEvery { oppgaveMediatorMock.hentOppgave(any()) } returns testOppgave
        coEvery {
            oppgaveMediatorMock.utsettOppgave(
                utsettOppgaveHendelse,
            )
        } just runs

        withOppgaveApi(oppgaveMediator = oppgaveMediatorMock) {
            client.put("oppgave/${testOppgave.oppgaveId}/utsett") {
                autentisert(token = gyldigSaksbehandlerToken)
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """
                        {
                          "utsettTilDato":"$utsettTilDato",
                          "beholdOppgave":"true"
                        }
                    """.trimMargin(),
                )
            }.also { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }
        }

        verify(exactly = 1) {
            oppgaveMediatorMock.utsettOppgave(utsettOppgaveHendelse)
        }
    }

    @Test
    fun `Hent oppgave med tilhørende personinfo og journalpostIder `() {
        val oppgaveMediatorMock = lagMediatorMock()
        val pdlMock = mockk<PDLKlient>()
        val journalpostIdClientMock = mockk<JournalpostIdClient>()
        val testOppgave =
            lagTestOppgaveMedTilstandOgBehandling(
                tilstand = FERDIG_BEHANDLET,
                behandling =
                    Behandling(
                        behandlingId = UUIDv7.ny(),
                        opprettet = LocalDateTime.now(),
                        person =
                            Person(
                                id = UUIDv7.ny(),
                                ident = testIdent,
                            ),
                        hendelse =
                            SøknadsbehandlingOpprettetHendelse(
                                søknadId = UUIDv7.ny(),
                                behandlingId = UUIDv7.ny(),
                                ident = testIdent,
                                opprettet = LocalDateTime.now(),
                            ),
                    ),
            )

        coEvery { oppgaveMediatorMock.hentOppgave(any()) } returns testOppgave
        coEvery { pdlMock.person(any()) } returns Result.success(testPerson)
        coEvery { journalpostIdClientMock.hentJournalpostId(any()) } returns Result.success("123456789")

        withOppgaveApi(
            oppgaveMediator = oppgaveMediatorMock,
            pdlKlient = pdlMock,
            journalpostIdClient = journalpostIdClientMock,
        ) {
            client.get("/oppgave/${testOppgave.oppgaveId}") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                //language=JSON
                json shouldEqualSpecifiedJsonIgnoringOrder
                    """
                     {
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
                    "tilstand": "${OppgaveTilstandDTO.FERDIG_BEHANDLET}",
                    "journalpostIder": ["123456789"]
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Får 404 Not Found ved forsøk på å hente oppgave som ikke finnes`() {
        val ikkeEksisterendeOppgaveId = UUIDv7.ny()
        val oppgaveMediator =
            lagMediatorMock().also {
                every { it.hentOppgave(any()) } throws DataNotFoundException("Fant ikke testoppgave")
            }
        withOppgaveApi(oppgaveMediator) {
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
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.finnOppgaverFor(testIdent) } returns
                    listOf(
                        lagTestOppgaveMedTilstand(FERDIG_BEHANDLET),
                        lagTestOppgaveMedTilstand(FERDIG_BEHANDLET),
                    )
            }
        withOppgaveApi(oppgaveMediatorMock) {
            client.post("/person/oppgaver") {
                autentisert()
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """{"ident": $testIdent}""",
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
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.hentOppgaveIdFor(behandlingIdSomFinnes) } returns oppgaveId
                every { it.hentOppgaveIdFor(behandlingIdSomIkkeFinnes) } returns null
            }
        withOppgaveApi(oppgaveMediator = oppgaveMediatorMock) {
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
        oppgaveMediator: OppgaveMediator = mockk<OppgaveMediator>(relaxed = true),
        pdlKlient: PDLKlient = mockk(relaxed = true),
        journalpostIdClient: JournalpostIdClient = mockk(relaxed = true),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            application {
                apiConfig()
                oppgaveApi(oppgaveMediator, pdlKlient, journalpostIdClient)
            }
            test()
        }
    }

    private fun lagMediatorMock(): OppgaveMediator {
        return mockk<OppgaveMediator>().also {
            every { it.personSkjermesSomEgneAnsatte(any()) } returns false
        }
    }

    private fun HttpRequestBuilder.autentisert(token: String = gyldigSaksbehandlerToken) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private fun lagTestOppgaveMedTilstandOgBehandling(
        tilstand: Oppgave.Tilstand.Type,
        saksbehandlerIdent: String? = null,
        behandling: Behandling,
    ): Oppgave {
        return Oppgave.rehydrer(
            oppgaveId = UUIDv7.ny(),
            ident = testIdent,
            saksbehandlerIdent = saksbehandlerIdent,
            behandlingId = behandling.behandlingId,
            opprettet = LocalDateTime.now(),
            emneknagger = setOf("Søknadsbehandling"),
            tilstand =
                when (tilstand) {
                    OPPRETTET -> Opprettet
                    KLAR_TIL_BEHANDLING -> KlarTilBehandling
                    UNDER_BEHANDLING -> UnderBehandling
                    FERDIG_BEHANDLET -> FerdigBehandlet
                    PAA_VENT -> TODO()
                },
            behandling = behandling,
            utsattTil = null,
        )
    }

    private fun lagTestOppgaveMedTilstand(
        tilstand: Oppgave.Tilstand.Type,
        saksbehandlerIdent: String? = null,
    ): Oppgave {
        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                person = Person(id = UUIDv7.ny(), ident = testIdent),
                opprettet = LocalDateTime.now(),
            )
        return lagTestOppgaveMedTilstandOgBehandling(tilstand, saksbehandlerIdent, behandling)
    }

    private val testPerson =
        PDLPersonIntern(
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
