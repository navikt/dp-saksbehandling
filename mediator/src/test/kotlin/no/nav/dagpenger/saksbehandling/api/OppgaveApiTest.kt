package no.nav.dagpenger.saksbehandling.api

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UlovligTilstandsendringException
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.SecureOppgaveMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.BESLUTTER_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.SAKSBEHANDLER_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.TEST_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.autentisert
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.gyldigBeslutterToken
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.gyldigSaksbehandlerToken
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.lagTestOppgaveMedTilstand
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.lagTestOppgaveMedTilstandOgBehandling
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.testPerson
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.withOppgaveApi
import no.nav.dagpenger.saksbehandling.api.models.AdressebeskyttelseGraderingDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerEnhetDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ToTrinnskontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdClient
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream

class OppgaveApiTest {
    val meldingOmVedtakHtml = "<h1>Melding om vedtak</h1>"
    private val saksbehandler = Saksbehandler(SAKSBEHANDLER_IDENT, emptySet())
    private val beslutter = Saksbehandler(BESLUTTER_IDENT, emptySet())
    private val mockAzure = mockAzure()
    private val ugyldigToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDI" +
            "yfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

    companion object {
        @JvmStatic
        private fun endepunktOgHttpMetodeProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("/oppgave", HttpMethod.Get),
                Arguments.of("/oppgave/neste", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId", HttpMethod.Get),
                Arguments.of("/oppgave/oppgaveId/klar-til-kontroll", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/kontroller", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/ferdigstill/melding-om-vedtak", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/ferdigstill/melding-om-vedtak-arena", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/utsett", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/tildel", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/legg-tilbake", HttpMethod.Put),
                Arguments.of("/person/oppgaver", HttpMethod.Post),
                Arguments.of("/behandling/behandlingId/oppgaveId", HttpMethod.Get),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("endepunktOgHttpMetodeProvider")
    fun `Skal avvise kall uten gyldig token`(
        endepunkt: String,
        httpMethod: HttpMethod,
    ) {
        withOppgaveApi {
            client.request(endepunkt) {
                method = httpMethod
                autentisert(token = ugyldigToken)
            }.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `GET på oppgaver uten query parameters`() {
        val iMorgen = LocalDate.now().plusDays(1)
        val oppgave1 =
            lagTestOppgaveMedTilstand(
                KLAR_TIL_BEHANDLING,
                saksbehandlerIdent = SAKSBEHANDLER_IDENT,
                utsattTil = iMorgen,
            )
        val oppgave2 =
            lagTestOppgaveMedTilstand(
                KLAR_TIL_BEHANDLING,
                saksbehandlerIdent = null,
                skjermesSomEgneAnsatte = true,
            )
        val oppgaveMediatorMock =
            mockk<SecureOppgaveMediator>().also {
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
                      "skjermesSomEgneAnsatte": ${oppgave1.behandling.person.skjermesSomEgneAnsatte},
                      "adressebeskyttelseGradering": "${AdressebeskyttelseGraderingDTO.UGRADERT}",
                      "tilstand": "${OppgaveTilstandDTO.KLAR_TIL_BEHANDLING}" ,
                      "saksbehandlerIdent": "${oppgave1.behandlerIdent}",
                      "utsattTilDato": "${oppgave1.utsattTil()}"
                    },
                    {
                      "oppgaveId": "${oppgave2.oppgaveId}",
                      "behandlingId": "${oppgave2.behandlingId}",
                      "personIdent": "${oppgave2.ident}",
                      "emneknagger": [
                        "Søknadsbehandling"
                      ],
                      "skjermesSomEgneAnsatte": ${oppgave2.behandling.person.skjermesSomEgneAnsatte},
                      "adressebeskyttelseGradering": "${AdressebeskyttelseGraderingDTO.UGRADERT}",
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
            mockk<SecureOppgaveMediator>().also {
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
            mockk<SecureOppgaveMediator>().also {
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
            mockk<SecureOppgaveMediator>().also {
                every {
                    it.søk(
                        Søkefilter(
                            periode =
                                Periode(
                                    fom = LocalDate.parse("2021-01-01"),
                                    tom = LocalDate.parse("2023-01-01"),
                                ),
                            tilstand = setOf(UNDER_BEHANDLING),
                            saksbehandlerIdent = SAKSBEHANDLER_IDENT,
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
    fun `Skal kunne ferdigstille en oppgave med melding om vedtak`() {
        val oppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING, SAKSBEHANDLER_IDENT)
        val saksbehandlerToken = gyldigSaksbehandlerToken(navIdent = SAKSBEHANDLER_IDENT)
        val godkjentBehandlingHendelse =
            GodkjentBehandlingHendelse(
                oppgave.oppgaveId,
                meldingOmVedtakHtml,
                utførtAv = saksbehandler,
            )
        val oppgaveMediatorMock =
            mockk<SecureOppgaveMediator>().also {
                every { it.ferdigstillOppgave(godkjentBehandlingHendelse, any(), any()) } just Runs
            }
        val pdlMock = mockk<PDLKlient>()
        coEvery { pdlMock.person(any()) } returns Result.success(testPerson)

        withOppgaveApi(oppgaveMediatorMock, pdlMock) {
            client.put("/oppgave/${oppgave.oppgaveId}/ferdigstill/melding-om-vedtak") {
                autentisert(token = saksbehandlerToken)
                setBody(meldingOmVedtakHtml)
                contentType(ContentType.Text.Html)
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }

            verify(exactly = 1) {
                oppgaveMediatorMock.ferdigstillOppgave(godkjentBehandlingHendelse, any(), any())
            }
        }
    }

    @Test
    fun `Ferdigstilling av en oppgave feiler dersom content type ikke er HTML `() {
        val meldingOmVedtakHtml = "<h1>Melding om vedtak</h1>"
        withOppgaveApi {
            client.put("/oppgave/${UUIDv7.ny()}/ferdigstill/melding-om-vedtak") {
                autentisert(gyldigSaksbehandlerToken(navIdent = "G151133"))
                setBody(meldingOmVedtakHtml)
                contentType(ContentType.Text.Plain)
            }.let { response ->
                response.status shouldBe HttpStatusCode.UnsupportedMediaType
            }
        }
    }

    @Test
    fun `Skal kunne ferdigstille en oppgave med melding om vedtak i Arena`() {
        val oppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING, SAKSBEHANDLER_IDENT)
        val saksbehandlerToken = gyldigSaksbehandlerToken()
        val godkjennBehandlingMedBrevIArena =
            GodkjennBehandlingMedBrevIArena(
                oppgaveId = oppgave.oppgaveId,
                utførtAv = saksbehandler,
            )
        val oppgaveMediatorMock =
            mockk<SecureOppgaveMediator>().also {
                every { it.ferdigstillOppgave(godkjennBehandlingMedBrevIArena, any(), any()) } just Runs
            }
        val pdlMock = mockk<PDLKlient>()
        coEvery { pdlMock.person(any()) } returns Result.success(testPerson)

        withOppgaveApi(oppgaveMediatorMock, pdlMock) {
            client.put("/oppgave/${oppgave.oppgaveId}/ferdigstill/melding-om-vedtak-arena") {
                autentisert(token = saksbehandlerToken)
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }
            verify(exactly = 1) {
                oppgaveMediatorMock.ferdigstillOppgave(godkjennBehandlingMedBrevIArena, any(), any())
            }
        }
    }

    @Test
    fun `Skal kunne hente og få tildelt neste oppgave`() {
        val oppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING, SAKSBEHANDLER_IDENT)
        val oppgaveMediatorMock =
            mockk<SecureOppgaveMediator>().also {
                every { it.tildelNesteOppgaveTil(any(), any()) } returns oppgave
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
                      "person": {
                        "ident": "$TEST_IDENT",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "kjonn": "UKJENT",
                        "statsborgerskap": "NOR",
                        "skjermesSomEgneAnsatte": ${oppgave.behandling.person.skjermesSomEgneAnsatte}
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
        val oppgaveMediatorMock =
            mockk<SecureOppgaveMediator>().also {
                every { it.tildelNesteOppgaveTil(any(), any()) } returns null
            }
        val pdlMock = mockk<PDLKlient>()

        withOppgaveApi(oppgaveMediator = oppgaveMediatorMock, pdlKlient = pdlMock) {
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
        val oppgaveMediatorMock = mockk<SecureOppgaveMediator>()
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)

        coEvery {
            oppgaveMediatorMock.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = testOppgave.oppgaveId,
                    ansvarligIdent = SAKSBEHANDLER_IDENT,
                    utførtAv = saksbehandler,
                ),
                any(),
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
                      "person": {
                        "ident": "$TEST_IDENT",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "kjonn": "UKJENT",
                        "statsborgerskap": "NOR",
                        "skjermesSomEgneAnsatte": ${testOppgave.behandling.person.skjermesSomEgneAnsatte},
                        "adressebeskyttelseGradering": "${AdressebeskyttelseGraderingDTO.UGRADERT}"
                      },
                      "emneknagger": ["Søknadsbehandling"],
                      "tilstand": "${OppgaveTilstandDTO.UNDER_BEHANDLING}"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Beslutter skal kunne ta en kontrolloppgave`() {
        val oppgaveMediatorMock = mockk<SecureOppgaveMediator>()
        val oppgaveId = UUIDv7.ny()

        coEvery {
            oppgaveMediatorMock.tildelTotrinnskontroll(
                ToTrinnskontrollHendelse(
                    oppgaveId = oppgaveId,
                    ansvarligIdent = BESLUTTER_IDENT,
                    utførtAv = beslutter,
                ),
                any(),
            )
        } just runs

        withOppgaveApi(oppgaveMediatorMock) {
            client.put("/oppgave/$oppgaveId/kontroller") { autentisert(token = gyldigBeslutterToken()) }
                .also { response ->
                    response.status shouldBe HttpStatusCode.NoContent
                }
        }
    }

    @Test
    fun `Feilstatuser når beslutter forsøker å ta en kontrolloppgave`() {
        val oppgaveMediatorMock = mockk<SecureOppgaveMediator>()
        val oppgaveSomIkkeFinnes = UUIDv7.ny()
        val oppgaveSomAlleredeErUnderKontroll = UUIDv7.ny()

        coEvery {
            oppgaveMediatorMock.tildelTotrinnskontroll(
                ToTrinnskontrollHendelse(
                    oppgaveId = oppgaveSomIkkeFinnes,
                    ansvarligIdent = beslutter.navIdent,
                    utførtAv = beslutter,
                ),
                any(),
            )
        } throws DataNotFoundException("Oppgave ikke funnet")

        coEvery {
            oppgaveMediatorMock.tildelTotrinnskontroll(
                ToTrinnskontrollHendelse(
                    oppgaveId = oppgaveSomAlleredeErUnderKontroll,
                    ansvarligIdent = beslutter.navIdent,
                    utførtAv = beslutter,
                ),
                any(),
            )
        } throws UlovligTilstandsendringException("Oppgaven er allerede under kontroll")

        withOppgaveApi(oppgaveMediatorMock) {
            client.put("/oppgave/$oppgaveSomIkkeFinnes/kontroller") { autentisert(token = gyldigBeslutterToken()) }
                .also { response ->
                    response.status shouldBe HttpStatusCode.NotFound
                }
            client.put("/oppgave/$oppgaveSomAlleredeErUnderKontroll/kontroller") { autentisert(token = gyldigBeslutterToken()) }
                .also { response ->
                    response.status shouldBe HttpStatusCode.Conflict
                }
        }
    }

    @Test
    fun `Saksbehandler skal kunne gi fra seg ansvar for en oppgave`() {
        val oppgaveMediatorMock = mockk<SecureOppgaveMediator>()
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)

        coEvery { oppgaveMediatorMock.hentOppgave(any(), any()) } returns testOppgave
        coEvery {
            oppgaveMediatorMock.fristillOppgave(
                FjernOppgaveAnsvarHendelse(
                    oppgaveId = testOppgave.oppgaveId,
                    utførtAv = saksbehandler,
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
                FjernOppgaveAnsvarHendelse(
                    oppgaveId = testOppgave.oppgaveId,
                    utførtAv = saksbehandler,
                ),
            )
        }
    }

    @Test
    fun `Saksbehandler skal kunne utsette oppgave`() {
        val oppgaveMediatorMock = mockk<SecureOppgaveMediator>()
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)
        val utsettTilDato = LocalDate.now().plusDays(1)
        val utsettOppgaveHendelse =
            UtsettOppgaveHendelse(
                oppgaveId = testOppgave.oppgaveId,
                navIdent = saksbehandler.navIdent,
                utsattTil = utsettTilDato,
                beholdOppgave = true,
                utførtAv = saksbehandler,
            )

        coEvery { oppgaveMediatorMock.hentOppgave(any(), any()) } returns testOppgave
        coEvery {
            oppgaveMediatorMock.utsettOppgave(
                utsettOppgaveHendelse, any(),
            )
        } just runs

        withOppgaveApi(oppgaveMediator = oppgaveMediatorMock) {
            client.put("oppgave/${testOppgave.oppgaveId}/utsett") {
                autentisert(token = gyldigSaksbehandlerToken())
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
            oppgaveMediatorMock.utsettOppgave(utsettOppgaveHendelse, any())
        }
    }

    @Test
    fun `Hent oppgave med tilhørende personinfo og journalpostIder `() {
        val oppgaveMediatorMock = mockk<SecureOppgaveMediator>()
        val pdlMock = mockk<PDLKlient>()
        val journalpostIdClientMock = mockk<JournalpostIdClient>()
        val saksbehandlerOppslagMock = mockk<SaksbehandlerOppslag>()

        val testOppgave =
            lagTestOppgaveMedTilstandOgBehandling(
                tilstand = UNDER_KONTROLL,
                tildeltBehandlerIdent = beslutter.navIdent,
                behandling =
                    Behandling(
                        behandlingId = UUIDv7.ny(),
                        opprettet = LocalDateTime.now(),
                        person =
                            Person(
                                id = UUIDv7.ny(),
                                ident = TEST_IDENT,
                                skjermesSomEgneAnsatte = true,
                                adressebeskyttelseGradering = UGRADERT,
                            ),
                        hendelse =
                            SøknadsbehandlingOpprettetHendelse(
                                søknadId = UUIDv7.ny(),
                                behandlingId = UUIDv7.ny(),
                                ident = TEST_IDENT,
                                opprettet = LocalDateTime.now(),
                            ),
                    ),
            )

        coEvery { oppgaveMediatorMock.hentOppgave(any(), any()) } returns testOppgave
        coEvery { pdlMock.person(any()) } returns Result.success(testPerson)
        coEvery { journalpostIdClientMock.hentJournalpostId(any()) } returns Result.success("123456789")
        coEvery { saksbehandlerOppslagMock.hentSaksbehandler(saksbehandler.navIdent) } returns
            BehandlerDTO(
                ident = saksbehandler.navIdent,
                fornavn = "Saksbehandler fornavn",
                etternavn = "Saksbehandler etternavn",
                enhet =
                    BehandlerEnhetDTO(
                        navn = "Enhet navn",
                        enhetNr = "1234",
                        postadresse = "Adresseveien 3, 0101 ADRESSA",
                    ),
            )
        coEvery { saksbehandlerOppslagMock.hentSaksbehandler(beslutter.navIdent) } returns
            BehandlerDTO(
                ident = beslutter.navIdent,
                fornavn = "Saksbeandler fornavn",
                etternavn = "Saksbehandler etternavn",
                enhet =
                    BehandlerEnhetDTO(
                        navn = "Enhet navn",
                        enhetNr = "1234",
                        postadresse = "Adresseveien 3, 0101 ADRESSA",
                    ),
            )

        withOppgaveApi(
            oppgaveMediator = oppgaveMediatorMock,
            pdlKlient = pdlMock,
            journalpostIdClient = journalpostIdClientMock,
            saksbehandlerOppslag = saksbehandlerOppslagMock,
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
                      "person": {
                        "ident": "${testOppgave.behandling.person.ident}",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "kjonn": "UKJENT",
                        "statsborgerskap": "NOR",
                        "skjermesSomEgneAnsatte": ${testOppgave.behandling.person.skjermesSomEgneAnsatte}
                      },
                      "emneknagger": ["Søknadsbehandling"],
                      "tilstand": "${OppgaveTilstandDTO.UNDER_KONTROLL}",
                      "journalpostIder": ["123456789"],
                      "saksbehandler": {
                        "ident": "${beslutter.navIdent}"
                      },
                      "tildeltBehandler": {
                        "ident": "${beslutter.navIdent}"
                      },
                      "sisteSaksbehandler": {
                        "ident": "${saksbehandler.navIdent}"
                      },
                      "sisteBeslutter": {
                        "ident": "${beslutter.navIdent}"
                      }
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Får 404 Not Found ved forsøk på å hente oppgave som ikke finnes`() {
        val ikkeEksisterendeOppgaveId = UUIDv7.ny()
        val oppgaveMediator =
            mockk<SecureOppgaveMediator>().also {
                every { it.hentOppgave(any(), any()) } throws DataNotFoundException("Fant ikke testoppgave")
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
            mockk<SecureOppgaveMediator>().also {
                every { it.finnOppgaverFor(TEST_IDENT) } returns
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
                    """{"ident": $TEST_IDENT}""",
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
            mockk<SecureOppgaveMediator>().also {
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
}
