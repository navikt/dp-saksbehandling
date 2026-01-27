package no.nav.dagpenger.saksbehandling.api

import PersonMediator
import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.Emneknagg.AvbrytBehandling.AVBRUTT_ANNET
import no.nav.dagpenger.saksbehandling.Emneknagg.PåVent.AVVENT_RAPPORTERINGSFRIST
import no.nav.dagpenger.saksbehandling.EmneknaggKategori
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.Companion.søkbareTilstander
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UlovligTilstandsendringException
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.autentisert
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.gyldigSaksbehandlerToken
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.withOppgaveApi
import no.nav.dagpenger.saksbehandling.api.models.AdressebeskyttelseGraderingDTO
import no.nav.dagpenger.saksbehandling.api.models.AvbrytOppgaveAarsakDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTORolleDTO
import no.nav.dagpenger.saksbehandling.api.models.KjonnDTO
import no.nav.dagpenger.saksbehandling.api.models.KontrollertBrevDTO
import no.nav.dagpenger.saksbehandling.api.models.LovligeEndringerDTO
import no.nav.dagpenger.saksbehandling.api.models.MeldingOmVedtakKildeDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTOBehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTOTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveIdDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktResultatDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonIdDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.SikkerhetstiltakDTO
import no.nav.dagpenger.saksbehandling.api.models.UtsettOppgaveAarsakDTO
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKreverIkkeTotrinnskontrollException
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.hendelser.AvbrytOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SlettNotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.stream.Stream

class OppgaveApiTest {
    init {
        mockAzure()
    }

    private val ugyldigToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDI" +
            "yfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

    companion object {
        @JvmStatic
        private fun endepunktOgHttpMetodeProvider(): Stream<Arguments> =
            Stream.of(
                Arguments.of("/oppgave", HttpMethod.Get),
                Arguments.of("/oppgave/neste", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId", HttpMethod.Get),
                Arguments.of("/oppgave/oppgaveId/tildel", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/notat", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/notat", HttpMethod.Delete),
                Arguments.of("/oppgave/oppgaveId/utsett", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/legg-tilbake", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/avbryt", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/send-til-kontroll", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/returner-til-saksbehandler", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/ferdigstill", HttpMethod.Put),
                Arguments.of("/person/personId", HttpMethod.Post),
                Arguments.of("/person/${UUIDv7.ny()}", HttpMethod.Get),
                Arguments.of("/person/oppgaver", HttpMethod.Post),
                Arguments.of("/behandling/behandlingId/oppgaveId", HttpMethod.Get),
            )
    }

    @ParameterizedTest
    @MethodSource("endepunktOgHttpMetodeProvider")
    fun `Skal avvise kall uten gyldig saksbehandler token`(
        endepunkt: String,
        httpMethod: HttpMethod,
    ) {
        withOppgaveApi {
            client
                .request(endepunkt) {
                    method = httpMethod
                    autentisert(token = ugyldigToken)
                }.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `GET på oppgaver uten query parameters`() {
        val iMorgen = LocalDate.now().plusDays(1)
        val oppgave1 =
            TestHelper.lagOppgave(
                tilstand = Oppgave.KlarTilBehandling,
                saksbehandlerIdent = TestHelper.saksbehandler.navIdent,
                utsattTil = iMorgen,
                person = TestHelper.testPerson,
            )
        val oppgave2 =
            TestHelper.lagOppgave(
                tilstand = Oppgave.KlarTilBehandling,
                person =
                    TestHelper.lagPerson(
                        skjermesSomEgneAnsatte = true,
                    ),
            )
        val oppgave3 =
            TestHelper.lagOppgave(
                tilstand = Oppgave.UnderBehandling,
                person = TestHelper.testPerson,
            )
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.søk(
                        Søkefilter(
                            periode = Periode.UBEGRENSET_PERIODE,
                            tilstander = søkbareTilstander,
                            saksbehandlerIdent = null,
                            personIdent = null,
                            oppgaveId = null,
                            behandlingId = null,
                        ),
                    )
                } returns PostgresOppgaveRepository.OppgaveSøkResultat(listOf(oppgave1, oppgave2, oppgave3), 3)
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client.get("/oppgave") { autentisert() }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                //language=JSON
                json shouldEqualSpecifiedJsonIgnoringOrder
                    """
                    {
                        "oppgaver": [
                            {
                                "oppgaveId": "${oppgave1.oppgaveId}",
                                "behandlingId": "${oppgave1.behandling.behandlingId}",
                                "personIdent": "${oppgave1.personIdent()}",
                                "emneknagger": [],
                                "skjermesSomEgneAnsatte": ${oppgave1.person.skjermesSomEgneAnsatte},
                                "adressebeskyttelseGradering": "${AdressebeskyttelseGraderingDTO.UGRADERT}",
                                "tilstand": "${OppgaveTilstandDTO.KLAR_TIL_BEHANDLING}",
                                "lovligeEndringer": {
                                    "paaVentAarsaker": [],
                                    "avbrytAarsaker": []
                                },
                                "behandlerIdent": "${oppgave1.behandlerIdent}",
                                "utsattTilDato": "${oppgave1.utsattTil()}"
                            },
                            {
                                "oppgaveId": "${oppgave2.oppgaveId}",
                                "behandlingId": "${oppgave2.behandling.behandlingId}",
                                "personIdent": "${oppgave2.personIdent()}",
                                "emneknagger": [],
                                "skjermesSomEgneAnsatte": ${oppgave2.person.skjermesSomEgneAnsatte},
                                "adressebeskyttelseGradering": "${AdressebeskyttelseGraderingDTO.UGRADERT}",
                                "tilstand": "${OppgaveTilstandDTO.KLAR_TIL_BEHANDLING}",
                                "lovligeEndringer": {
                                    "paaVentAarsaker": [],
                                    "avbrytAarsaker": []
                                }
                            },
                            {
                                "oppgaveId": "${oppgave3.oppgaveId}",
                                "behandlingId": "${oppgave3.behandling.behandlingId}",
                                "personIdent": "${oppgave3.personIdent()}",
                                "emneknagger": [],
                                "skjermesSomEgneAnsatte": ${oppgave3.person.skjermesSomEgneAnsatte},
                                "adressebeskyttelseGradering": "${AdressebeskyttelseGraderingDTO.UGRADERT}",
                                "tilstand": "${OppgaveTilstandDTO.UNDER_BEHANDLING}",
                                "lovligeEndringer": {
                                    "paaVentAarsaker": [
                                        "AVVENT_SVAR",
                                        "AVVENT_DOKUMENTASJON",
                                        "AVVENT_MELDEKORT",
                                        "AVVENT_PERMITTERINGSÅRSAK",
                                        "AVVENT_RAPPORTERINGSFRIST",
                                        "AVVENT_SVAR_PÅ_FORESPØRSEL",
                                        "ANNET"
                                        ],
                                    "avbrytAarsaker": [
                                        "BEHANDLES_I_ARENA",
                                        "FLERE_SØKNADER",
                                        "TRUKKET_SØKNAD",
                                        "ANNET"
                                        ]
                                }
                            }
                        ],
                        "totaltAntallOppgaver" : 3
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Hent alle oppgaver med tilstander basert på query parameter`() {
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                val oppgaveSøkResultat =
                    listOf(
                        TestHelper.lagOppgave(tilstand = Oppgave.KlarTilBehandling),
                        TestHelper.lagOppgave(tilstand = Oppgave.KlarTilBehandling),
                    ).let {
                        PostgresOppgaveRepository.OppgaveSøkResultat(it, it.size)
                    }

                every {
                    it.søk(
                        Søkefilter(
                            periode = Periode.UBEGRENSET_PERIODE,
                            tilstander = setOf(KLAR_TIL_BEHANDLING, UNDER_BEHANDLING),
                        ),
                    )
                } returns
                    oppgaveSøkResultat
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client
                .get("/oppgave?tilstand=KLAR_TIL_BEHANDLING&tilstand=UNDER_BEHANDLING") { autentisert() }
                .let { response ->
                    response.status shouldBe HttpStatusCode.OK
                    "${response.contentType()}" shouldContain "application/json"
                    val oppgaveOversiktResultatDTO =
                        objectMapper.readValue(
                            response.bodyAsText(),
                            object : TypeReference<OppgaveOversiktResultatDTO>() {},
                        )
                    oppgaveOversiktResultatDTO.totaltAntallOppgaver shouldBe 2
                    oppgaveOversiktResultatDTO.oppgaver.size shouldBe 2
                }
        }
    }

    @Test
    fun `Hent alle oppgaver basert på emneknagg`() {
        val søkResultat =
            listOf(
                TestHelper.lagOppgave(tilstand = Oppgave.KlarTilBehandling),
                TestHelper.lagOppgave(tilstand = Oppgave.KlarTilBehandling),
            ).let {
                PostgresOppgaveRepository.OppgaveSøkResultat(it, it.size)
            }

        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.søk(
                        match {
                            it.emneknaggGruppertPerKategori ==
                                mapOf(
                                    EmneknaggKategori.RETTIGHET to
                                        setOf(
                                            "TULLBALL",
                                            "KLAGE",
                                        ),
                                ) &&
                                it.tilstander == setOf(KLAR_TIL_BEHANDLING) &&
                                it.periode == Periode.UBEGRENSET_PERIODE
                        },
                    )
                } returns søkResultat
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client
                .get("/oppgave?tilstand=${KLAR_TIL_BEHANDLING}&rettighet=TULLBALL&rettighet=KLAGE") { autentisert() }
                .let { response ->
                    response.status shouldBe HttpStatusCode.OK
                    "${response.contentType()}" shouldContain "application/json"
                    val oppgaveOversiktResultatDTO =
                        objectMapper.readValue(
                            response.bodyAsText(),
                            object : TypeReference<OppgaveOversiktResultatDTO>() {},
                        )
                    oppgaveOversiktResultatDTO.oppgaver?.size shouldBe 2
                    oppgaveOversiktResultatDTO.totaltAntallOppgaver shouldBe 2
                }
        }
    }

    @Test
    fun `Hent alle oppgaver fom, tom, mine  og tilstand`() {
        val søkResultat =
            listOf(
                TestHelper.lagOppgave(tilstand = Oppgave.UnderBehandling),
                TestHelper.lagOppgave(tilstand = Oppgave.UnderBehandling),
            ).let {
                PostgresOppgaveRepository.OppgaveSøkResultat(it, it.size)
            }

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
                            tilstander = setOf(UNDER_BEHANDLING),
                            saksbehandlerIdent = TestHelper.saksbehandler.navIdent,
                        ),
                    )
                } returns søkResultat
            }
        withOppgaveApi(oppgaveMediatorMock) {
            client
                .get("/oppgave?tilstand=$UNDER_BEHANDLING&fom=2021-01-01&tom=2023-01-01&mineOppgaver=true") { autentisert() }
                .let { response ->
                    response.status shouldBe HttpStatusCode.OK
                    "${response.contentType()}" shouldContain "application/json"
                    val oppgaveOversiktResultatDTO =
                        objectMapper.readValue(
                            response.bodyAsText(),
                            object : TypeReference<OppgaveOversiktResultatDTO>() {},
                        )
                    oppgaveOversiktResultatDTO.oppgaver?.size shouldBe 2
                    oppgaveOversiktResultatDTO.totaltAntallOppgaver shouldBe 2
                }
        }
    }

    @Test
    fun `Skal kunne lagre et notat på en oppgave`() {
        val oppgaveId = UUIDv7.ny()

        val beslutterToken =
            gyldigSaksbehandlerToken(
                adGrupper = listOf(Configuration.beslutterADGruppe),
                navIdent = TestHelper.beslutter.navIdent,
            )

        val notat: String = "Dette er et notat"

        @Language("JSON")
        val notatJson = """ {"tekst": "$notat"} """

        val sisteEndretTidspunkt = LocalDateTime.of(2021, 1, 1, 12, 0)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.lagreNotat(NotatHendelse(oppgaveId, notat, TestHelper.beslutter))
                } returns sisteEndretTidspunkt
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client
                .put("/oppgave/$oppgaveId/notat") {
                    autentisert(token = beslutterToken)
                    header(HttpHeaders.ContentType, "application/json")
                    setBody(notatJson)
                }.let { response ->
                    response.status shouldBe HttpStatusCode.OK
                    "${response.contentType()}" shouldContain "application/json"
                    response.bodyAsText() shouldEqualSpecifiedJson
                        """
                        {
                           "sistEndretTidspunkt" : "2021-01-01T12:00:00"
                        }
                        """.trimIndent()
                }
        }
    }

    @Test
    fun `Skal kunne slette et notat på en oppgave`() {
        val oppgaveId = UUIDv7.ny()
        val beslutterToken =
            gyldigSaksbehandlerToken(
                adGrupper = listOf(Configuration.beslutterADGruppe),
                navIdent = TestHelper.beslutter.navIdent,
            )
        val sisteEndretTidspunkt = LocalDateTime.of(2021, 1, 1, 12, 0)
        val slettNotatHendelse = SlettNotatHendelse(oppgaveId, TestHelper.beslutter)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.slettNotat(slettNotatHendelse)
                } returns sisteEndretTidspunkt
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client
                .delete("/oppgave/$oppgaveId/notat") {
                    autentisert(token = beslutterToken)
                }.let { response ->
                    response.status shouldBe HttpStatusCode.OK
                    "${response.contentType()}" shouldContain "application/json"
                    response.bodyAsText() shouldEqualSpecifiedJson
                        """
                        {
                           "sistEndretTidspunkt" : "2021-01-01T12:00:00"
                        }
                        """.trimIndent()
                }
        }
        verify(exactly = 1) { oppgaveMediatorMock.slettNotat(slettNotatHendelse) }
    }

    @Test
    fun `Skal kunne ferdigstille en oppgave`() {
        val oppgave =
            TestHelper.lagOppgave(
                tilstand = Oppgave.UnderBehandling,
                saksbehandlerIdent = TestHelper.saksbehandler.navIdent,
            )
        val saksbehandlerToken = gyldigSaksbehandlerToken(navIdent = TestHelper.saksbehandler.navIdent)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                coEvery {
                    it.ferdigstillOppgave(
                        oppgaveId = oppgave.oppgaveId,
                        saksbehandler = any(),
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } just Runs
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client
                .put("/oppgave/${oppgave.oppgaveId}/ferdigstill") {
                    autentisert(token = saksbehandlerToken)
                    contentType(ContentType.Application.Json)
                }.let { response ->
                    response.status shouldBe HttpStatusCode.NoContent
                }

            coVerify(exactly = 1) {
                oppgaveMediatorMock.ferdigstillOppgave(
                    oppgaveId = oppgave.oppgaveId,
                    saksbehandler = any(),
                    saksbehandlerToken = saksbehandlerToken,
                )
            }
        }
    }

    @Test
    fun `Skal kunne sende en oppgave til kontroll`() {
        val oppgave =
            TestHelper.lagOppgave(
                tilstand = Oppgave.UnderBehandling,
                saksbehandlerIdent = TestHelper.saksbehandler.navIdent,
            )
        val saksbehandlerToken = gyldigSaksbehandlerToken(navIdent = TestHelper.saksbehandler.navIdent)
        val sendTilKontrollHendelse = SendTilKontrollHendelse(oppgave.oppgaveId, TestHelper.saksbehandler)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.sendTilKontroll(sendTilKontrollHendelse, saksbehandlerToken) } just Runs
            }
        withOppgaveApi(oppgaveMediatorMock) {
            client
                .put("/oppgave/${oppgave.oppgaveId}/send-til-kontroll") {
                    autentisert(token = saksbehandlerToken)
                }.let { response ->
                    response.status shouldBe HttpStatusCode.NoContent
                }

            verify(exactly = 1) {
                oppgaveMediatorMock.sendTilKontroll(sendTilKontrollHendelse, saksbehandlerToken)
            }
        }
    }

    @Test
    fun `Skal feile på send til kontroll hvis oppgaven ikke trenger totrinns`() {
        val oppgave =
            TestHelper.lagOppgave(
                tilstand = Oppgave.UnderBehandling,
                saksbehandlerIdent = TestHelper.saksbehandler.navIdent,
            )
        val saksbehandlerToken = gyldigSaksbehandlerToken(navIdent = TestHelper.saksbehandler.navIdent)
        val sendTilKontrollHendelse = SendTilKontrollHendelse(oppgave.oppgaveId, TestHelper.saksbehandler)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.sendTilKontroll(
                        sendTilKontrollHendelse,
                        saksbehandlerToken,
                    )
                } throws BehandlingKreverIkkeTotrinnskontrollException("Testmelding")
            }
        withOppgaveApi(oppgaveMediatorMock) {
            client
                .put("/oppgave/${oppgave.oppgaveId}/send-til-kontroll") {
                    autentisert(token = saksbehandlerToken)
                }.let { response ->
                    response.status shouldBe HttpStatusCode.Conflict
                }

            verify(exactly = 1) {
                oppgaveMediatorMock.sendTilKontroll(sendTilKontrollHendelse, saksbehandlerToken)
            }
        }
    }

    @Test
    fun `Beslutter skal kunne sende en oppgave tilbake til saksbehandler`() {
        val oppgave =
            TestHelper.lagOppgave(
                tilstand = Oppgave.UnderKontroll(),
                saksbehandlerIdent = TestHelper.beslutter.navIdent,
            )
        val beslutterToken =
            gyldigSaksbehandlerToken(
                navIdent = TestHelper.beslutter.navIdent,
                adGrupper = listOf(Configuration.beslutterADGruppe),
            )
        val returnerTilSaksbehandlingHendelse =
            ReturnerTilSaksbehandlingHendelse(oppgave.oppgaveId, TestHelper.beslutter)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.returnerTilSaksbehandling(returnerTilSaksbehandlingHendelse, any()) } just Runs
            }
        withOppgaveApi(oppgaveMediatorMock) {
            client
                .put("/oppgave/${oppgave.oppgaveId}/returner-til-saksbehandler") {
                    autentisert(token = beslutterToken)
                }.let { response ->
                    response.status shouldBe HttpStatusCode.NoContent
                }
            verify(exactly = 1) {
                oppgaveMediatorMock.returnerTilSaksbehandling(returnerTilSaksbehandlingHendelse, beslutterToken)
            }
        }
    }

    @Test
    fun `Skal kunne hente og få tildelt neste oppgave`() {
        val oppgave =
            TestHelper.lagOppgave(
                tilstand = Oppgave.KlarTilBehandling,
                saksbehandlerIdent = TestHelper.saksbehandler.navIdent,
                tilstandslogg =
                    OppgaveTilstandslogg(
                        Tilstandsendring(
                            tilstand = KLAR_TIL_BEHANDLING,
                            hendelse =
                                ForslagTilVedtakHendelse(
                                    ident = TestHelper.personIdent,
                                    behandletHendelseId = TestHelper.søknadId.toString(),
                                    behandletHendelseType = "Søknad",
                                    behandlingId = UUID.randomUUID(),
                                ),
                            tidspunkt = TestHelper.opprettetNå,
                        ),
                    ),
            )

        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.tildelOgHentNesteOppgave(any(), any()) } returns oppgave
            }
        val pdlMock = mockk<PDLKlient>()
        coEvery { pdlMock.person(any()) } returns Result.success(TestHelper.pdlPerson)

        withOppgaveApi(oppgaveMediatorMock, pdlMock) {
            client
                .put("/oppgave/neste") {
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
                          "behandlingId": "${oppgave.behandling.behandlingId}",
                          "person": {
                            "ident": "${oppgave.personIdent()}",
                            "fornavn": "PETTER",
                            "etternavn": "SMART",
                            "fodselsdato": "2000-01-01",
                            "kjonn": "UKJENT",
                            "statsborgerskap": "NOR",
                            "skjermesSomEgneAnsatte": ${oppgave.person.skjermesSomEgneAnsatte}
                          },
                          "emneknagger": [],
                          "tilstand": "${OppgaveTilstandDTO.KLAR_TIL_BEHANDLING}",
                          "soknadId": "${TestHelper.søknadId}"
                        }
                        """.trimIndent()
                }
        }
    }

    @Test
    fun `404 når det ikke finnes noen neste oppgave for saksbehandler`() {
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.tildelOgHentNesteOppgave(any(), any()) } returns null
            }
        val pdlMock = mockk<PDLKlient>()

        withOppgaveApi(oppgaveMediator = oppgaveMediatorMock, pdlKlient = pdlMock) {
            client
                .put("/oppgave/neste") {
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
    fun `Saksbehandler skal kunne ta en oppgave som er KLAR_TIL_BEHANDLING`() {
        val oppgaveMediatorMock = mockk<OppgaveMediator>()
        val testOppgaveId = UUIDv7.ny()
        coEvery {
            oppgaveMediatorMock.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = testOppgaveId,
                    ansvarligIdent = TestHelper.saksbehandler.navIdent,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )
        } returns TestHelper.lagOppgave(oppgaveId = testOppgaveId, tilstand = Oppgave.UnderBehandling)

        withOppgaveApi(oppgaveMediatorMock) {
            client.put("/oppgave/$testOppgaveId/tildel") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                response.bodyAsText() shouldEqualSpecifiedJson
                    """
                    {
                      "nyTilstand" : "${OppgaveTilstandDTO.UNDER_BEHANDLING}",
                      "behandlingType" : "RETT_TIL_DAGPENGER",
                      "utlostAv" : "SØKNAD"
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Feilstatuser ved tildeling av kontrolloppgave`() {
        val oppgaveMediatorMock = mockk<OppgaveMediator>()
        val oppgaveSomIkkeFinnes = UUIDv7.ny()
        val oppgaveSomAlleredeErUnderKontroll = UUIDv7.ny()

        coEvery {
            oppgaveMediatorMock.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgaveSomIkkeFinnes,
                    ansvarligIdent = TestHelper.saksbehandler.navIdent,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )
        } throws DataNotFoundException("Oppgave ikke funnet")

        coEvery {
            oppgaveMediatorMock.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgaveSomAlleredeErUnderKontroll,
                    ansvarligIdent = TestHelper.saksbehandler.navIdent,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )
        } throws UlovligTilstandsendringException("Oppgaven er allerede under kontroll")

        withOppgaveApi(oppgaveMediatorMock) {
            client
                .put("/oppgave/$oppgaveSomIkkeFinnes/tildel") { autentisert() }
                .also { response ->
                    response.status shouldBe HttpStatusCode.NotFound
                }
            client
                .put("/oppgave/$oppgaveSomAlleredeErUnderKontroll/tildel") { autentisert() }
                .also { response ->
                    response.status shouldBe HttpStatusCode.Conflict
                }
        }
    }

    @Test
    fun `Saksbehandler skal kunne gi fra seg ansvar for en oppgave`() {
        val oppgaveMediatorMock = mockk<OppgaveMediator>()
        val oppgave =
            TestHelper.lagOppgave(
                tilstand = Oppgave.UnderBehandling,
                saksbehandlerIdent = TestHelper.saksbehandler.navIdent,
            )

        coEvery {
            oppgaveMediatorMock.fristillOppgave(
                FjernOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )
        } just runs

        withOppgaveApi(oppgaveMediator = oppgaveMediatorMock) {
            client.put("oppgave/${oppgave.oppgaveId}/legg-tilbake") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }
        }

        verify(exactly = 1) {
            oppgaveMediatorMock.fristillOppgave(
                FjernOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )
        }
    }

    @Test
    fun `Saksbehandler skal kunne utsette oppgave`() {
        val oppgaveMediatorMock = mockk<OppgaveMediator>()
        val oppgave =
            TestHelper.lagOppgave(
                tilstand = Oppgave.UnderBehandling,
                saksbehandlerIdent = TestHelper.saksbehandler.navIdent,
            )
        val utsettTilDato = LocalDate.now().plusDays(1)
        val utsettOppgaveHendelse =
            UtsettOppgaveHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = TestHelper.saksbehandler.navIdent,
                utsattTil = utsettTilDato,
                beholdOppgave = true,
                utførtAv = TestHelper.saksbehandler,
                årsak = AVVENT_RAPPORTERINGSFRIST,
            )

        coEvery {
            oppgaveMediatorMock.utsettOppgave(utsettOppgaveHendelse)
        } just runs

        withOppgaveApi(oppgaveMediator = oppgaveMediatorMock) {
            client
                .put("oppgave/${oppgave.oppgaveId}/utsett") {
                    autentisert(token = gyldigSaksbehandlerToken())
                    contentType(ContentType.Application.Json)
                    setBody(
                        //language=JSON
                        """
                        {
                          "utsettTilDato":"$utsettTilDato",
                          "beholdOppgave":"true",
                          "aarsak":"AVVENT_RAPPORTERINGSFRIST"
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
    fun `Saksbehandler skal kunne avbryte en oppgave`() {
        val oppgave =
            TestHelper.lagOppgave(
                tilstand = Oppgave.UnderBehandling,
                saksbehandlerIdent = TestHelper.saksbehandler.navIdent,
            )
        val saksbehandlerToken = gyldigSaksbehandlerToken(navIdent = TestHelper.saksbehandler.navIdent)
        val avbrytOppgaveHendelse =
            AvbrytOppgaveHendelse(
                oppgaveId = oppgave.oppgaveId,
                årsak = AVBRUTT_ANNET,
                navIdent = TestHelper.saksbehandler.navIdent,
                utførtAv = TestHelper.saksbehandler,
            )
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.avbryt(
                        avbrytOppgaveHendelse = avbrytOppgaveHendelse,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } just Runs
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client
                .put("/oppgave/${oppgave.oppgaveId}/avbryt") {
                    autentisert(token = saksbehandlerToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        //language=JSON
                        """
                        {
                          "aarsak": "ANNET"
                        }
                        """.trimMargin(),
                    )
                }.let { response ->
                    response.status shouldBe HttpStatusCode.NoContent
                }

            coVerify(exactly = 1) {
                oppgaveMediatorMock.avbryt(
                    avbrytOppgaveHendelse = avbrytOppgaveHendelse,
                    saksbehandlerToken = saksbehandlerToken,
                )
            }
        }
    }

    @Test
    fun `Skal får 404 hvis man forsøker å avbryte en oppgave som ikke finnes`() {
        val oppgave =
            TestHelper.lagOppgave(
                tilstand = Oppgave.UnderBehandling,
                saksbehandlerIdent = TestHelper.saksbehandler.navIdent,
            )
        val saksbehandlerToken = gyldigSaksbehandlerToken(navIdent = TestHelper.saksbehandler.navIdent)
        val avbrytOppgaveHendelse =
            AvbrytOppgaveHendelse(
                oppgaveId = oppgave.oppgaveId,
                årsak = AVBRUTT_ANNET,
                navIdent = TestHelper.saksbehandler.navIdent,
                utførtAv = TestHelper.saksbehandler,
            )
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.avbryt(
                        avbrytOppgaveHendelse = avbrytOppgaveHendelse,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } throws DataNotFoundException("Fant ikke oppgave med id ${oppgave.oppgaveId}")
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client
                .put("/oppgave/${oppgave.oppgaveId}/avbryt") {
                    autentisert(token = saksbehandlerToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        //language=JSON
                        """
                        {
                          "aarsak": "ANNET"
                        }
                        """.trimMargin(),
                    )
                }.let { response ->
                    response.status shouldBe HttpStatusCode.NotFound
                    response.bodyAsText() shouldEqualSpecifiedJsonIgnoringOrder """{
                    "type" : "dagpenger.nav.no/saksbehandling:problem:ressurs-ikke-funnet",
                    "title" : "Ressurs ikke funnet",
                    "status" : 404,
                    "detail" : "Fant ikke oppgave med id ${oppgave.oppgaveId}"
                }"""
                }

            coVerify(exactly = 1) {
                oppgaveMediatorMock.avbryt(
                    avbrytOppgaveHendelse = avbrytOppgaveHendelse,
                    saksbehandlerToken = saksbehandlerToken,
                )
            }
        }
    }

    @Test
    fun `Hent oppgave med tilhørende personinfo, sikkerhetstiltak og journalpostIder`() {
        val oppgaveMediatorMock = mockk<OppgaveMediator>()

        val testOppgave =
            TestHelper.lagOppgave(
                tilstand = Oppgave.UnderKontroll(),
                saksbehandlerIdent = TestHelper.beslutter.navIdent,
                person = TestHelper.testPerson,
            )
        coEvery { oppgaveMediatorMock.hentOppgave(any(), any()) } returns testOppgave
        val oppgaveDTOMapperMock =
            mockk<OppgaveDTOMapper>().also { mapperMock ->
                val tidspunkt = LocalDateTime.of(2021, 1, 1, 12, 0)
                coEvery { mapperMock.lagOppgaveDTO(testOppgave) } returns
                    OppgaveDTO(
                        oppgaveId = testOppgave.oppgaveId,
                        behandlingId = testOppgave.behandling.behandlingId,
                        person =
                            PersonDTO(
                                ident = TestHelper.pdlPerson.ident,
                                id = testOppgave.person.id,
                                fornavn = TestHelper.pdlPerson.fornavn,
                                etternavn = TestHelper.pdlPerson.etternavn,
                                fodselsdato = TestHelper.pdlPerson.fødselsdato,
                                alder = TestHelper.pdlPerson.alder,
                                kjonn = KjonnDTO.UKJENT,
                                skjermesSomEgneAnsatte = testOppgave.person.skjermesSomEgneAnsatte,
                                adressebeskyttelseGradering = AdressebeskyttelseGraderingDTO.UGRADERT,
                                mellomnavn = TestHelper.pdlPerson.mellomnavn,
                                statsborgerskap = TestHelper.pdlPerson.statsborgerskap,
                                sikkerhetstiltak =
                                    listOf(
                                        SikkerhetstiltakDTO(
                                            beskrivelse =
                                                TestHelper.pdlPerson.sikkerhetstiltak
                                                    .first()
                                                    .beskrivelse,
                                            gyldigTom =
                                                TestHelper.pdlPerson.sikkerhetstiltak
                                                    .first()
                                                    .gyldigTom,
                                        ),
                                    ),
                            ),
                        tidspunktOpprettet = testOppgave.opprettet,
                        behandlingType = testOppgave.tilBehandlingTypeDTO(),
                        utlostAv = testOppgave.tilUtlostAvTypeDTO(),
                        emneknagger = testOppgave.emneknagger.tilOppgaveEmneknaggerDTOListe(),
                        tilstand = testOppgave.tilstand().tilOppgaveTilstandDTO(),
                        saksbehandler =
                            BehandlerDTO(
                                ident = TestHelper.saksbehandler.navIdent,
                                fornavn = "Saksbehandler fornavn",
                                etternavn = "Saksbehandler etternavn",
                                enhet =
                                    BehandlerDTOEnhetDTO(
                                        navn = "Enhet navn",
                                        enhetNr = "1234",
                                        postadresse = "Adresseveien 3, 0101 ADRESSA",
                                    ),
                            ),
                        beslutter =
                            BehandlerDTO(
                                ident = TestHelper.beslutter.navIdent,
                                fornavn = "Saksbeandler fornavn",
                                etternavn = "Saksbehandler etternavn",
                                enhet =
                                    BehandlerDTOEnhetDTO(
                                        navn = "Enhet navn",
                                        enhetNr = "1234",
                                        postadresse = "Adresseveien 3, 0101 ADRESSA",
                                    ),
                            ),
                        utsattTilDato = null,
                        journalpostIder = listOf("123456789"),
                        historikk =
                            listOf(
                                OppgaveHistorikkDTO(
                                    type = OppgaveHistorikkDTOTypeDTO.NOTAT,
                                    tidspunkt = tidspunkt,
                                    behandler =
                                        OppgaveHistorikkDTOBehandlerDTO(
                                            navn = "Ole Doffen",
                                            rolle = BehandlerDTORolleDTO.BESLUTTER,
                                        ),
                                    tittel = "Notat",
                                    body = "Dette er et notat",
                                ),
                            ),
                        notat = null,
                        lovligeEndringer =
                            LovligeEndringerDTO(
                                paaVentAarsaker =
                                    when (testOppgave.tilstand().type) {
                                        UNDER_BEHANDLING -> UtsettOppgaveAarsakDTO.entries
                                        else -> emptyList()
                                    },
                                avbrytAarsaker =
                                    when (testOppgave.tilstand().type) {
                                        UNDER_BEHANDLING -> AvbrytOppgaveAarsakDTO.entries
                                        else -> emptyList()
                                    },
                            ),
                        meldingOmVedtakKilde = MeldingOmVedtakKildeDTO.DP_SAK,
                        kontrollertBrev = KontrollertBrevDTO.IKKE_RELEVANT,
                    )
            }

        withOppgaveApi(
            oppgaveMediator = oppgaveMediatorMock,
            oppgaveDTOMapper = oppgaveDTOMapperMock,
        ) {
            client.get("/oppgave/${testOppgave.oppgaveId}") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                //language=JSON
                json shouldEqualSpecifiedJsonIgnoringOrder
                    """
                    {
                      "behandlingId": "${testOppgave.behandling.behandlingId}",
                      "person": {
                        "ident": "${testOppgave.personIdent()}",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "kjonn": "UKJENT",
                        "statsborgerskap": "NOR",
                        "sikkerhetstiltak": [
                          {
                            "beskrivelse": "${TestHelper.pdlPerson.sikkerhetstiltak.first().beskrivelse}",
                            "gyldigTom": "${TestHelper.pdlPerson.sikkerhetstiltak.first().gyldigTom}"
                          }
                        ],
                        "skjermesSomEgneAnsatte": ${testOppgave.person.skjermesSomEgneAnsatte}
                      },
                      "emneknagger": [],
                      "tilstand": "${OppgaveTilstandDTO.UNDER_KONTROLL}",
                      "journalpostIder": ["123456789"],
                      "saksbehandler": {
                        "ident": "${TestHelper.saksbehandler.navIdent}"
                      },
                      "beslutter": {
                        "ident": "${TestHelper.beslutter.navIdent}"
                      },
                      "historikk": [
                        {
                          "type": "notat",
                          "tidspunkt": "2021-01-01T12:00:00",
                          "tittel": "Notat",
                          "behandler": {
                            "navn": "Ole Doffen",
                            "rolle": "beslutter"
                          },
                          "body": "Dette er et notat"
                        }
                      ]
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Hent oppgave for person uten sikkerhetstiltak`() {
        val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)
        val oppgave =
            TestHelper.lagOppgave(
                person = TestHelper.testPerson,
                tilstand = Oppgave.UnderBehandling,
                saksbehandlerIdent = TestHelper.saksbehandler.navIdent,
            )
        coEvery { oppgaveMediatorMock.hentOppgave(any(), any()) } returns oppgave
        val oppgaveDTOMapperMock =
            mockk<OppgaveDTOMapper>().also { mapperMock ->
                val tidspunkt = LocalDateTime.of(2021, 1, 1, 12, 0)
                coEvery { mapperMock.lagOppgaveDTO(oppgave) } returns
                    OppgaveDTO(
                        oppgaveId = oppgave.oppgaveId,
                        behandlingId = oppgave.behandling.behandlingId,
                        person =
                            PersonDTO(
                                ident = TestHelper.pdlPerson.ident,
                                id = UUIDv7.ny(),
                                fornavn = TestHelper.pdlPerson.fornavn,
                                etternavn = TestHelper.pdlPerson.etternavn,
                                fodselsdato = TestHelper.pdlPerson.fødselsdato,
                                alder = TestHelper.pdlPerson.alder,
                                kjonn = KjonnDTO.UKJENT,
                                skjermesSomEgneAnsatte = oppgave.person.skjermesSomEgneAnsatte,
                                adressebeskyttelseGradering = AdressebeskyttelseGraderingDTO.UGRADERT,
                                mellomnavn = TestHelper.pdlPerson.mellomnavn,
                                statsborgerskap = TestHelper.pdlPerson.statsborgerskap,
                                sikkerhetstiltak = emptyList(),
                            ),
                        tidspunktOpprettet = oppgave.opprettet,
                        behandlingType = oppgave.tilBehandlingTypeDTO(),
                        utlostAv = oppgave.tilUtlostAvTypeDTO(),
                        emneknagger = oppgave.emneknagger.tilOppgaveEmneknaggerDTOListe(),
                        tilstand = oppgave.tilstand().tilOppgaveTilstandDTO(),
                        saksbehandler =
                            BehandlerDTO(
                                ident = TestHelper.saksbehandler.navIdent,
                                fornavn = "Saksbehandler fornavn",
                                etternavn = "Saksbehandler etternavn",
                                enhet =
                                    BehandlerDTOEnhetDTO(
                                        navn = "Enhet navn",
                                        enhetNr = "1234",
                                        postadresse = "Adresseveien 3, 0101 ADRESSA",
                                    ),
                            ),
                        utsattTilDato = null,
                        journalpostIder = listOf("123456789"),
                        historikk =
                            listOf(
                                OppgaveHistorikkDTO(
                                    type = OppgaveHistorikkDTOTypeDTO.NOTAT,
                                    tidspunkt = tidspunkt,
                                    behandler =
                                        OppgaveHistorikkDTOBehandlerDTO(
                                            navn = "Ole Doffen",
                                            rolle = BehandlerDTORolleDTO.BESLUTTER,
                                        ),
                                    tittel = "Notat",
                                    body = "Dette er et notat",
                                ),
                            ),
                        notat = null,
                        lovligeEndringer =
                            LovligeEndringerDTO(
                                paaVentAarsaker =
                                    when (oppgave.tilstand().type) {
                                        UNDER_BEHANDLING -> UtsettOppgaveAarsakDTO.entries
                                        else -> emptyList()
                                    },
                                avbrytAarsaker =
                                    when (oppgave.tilstand().type) {
                                        UNDER_BEHANDLING -> AvbrytOppgaveAarsakDTO.entries
                                        else -> emptyList()
                                    },
                            ),
                        meldingOmVedtakKilde = MeldingOmVedtakKildeDTO.DP_SAK,
                        kontrollertBrev = KontrollertBrevDTO.IKKE_RELEVANT,
                    )
            }

        withOppgaveApi(
            oppgaveMediator = oppgaveMediatorMock,
            oppgaveDTOMapper = oppgaveDTOMapperMock,
        ) {
            client.get("/oppgave/${oppgave.oppgaveId}") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                //language=JSON
                json shouldEqualSpecifiedJsonIgnoringOrder
                    """
                    {
                      "behandlingId": "${oppgave.behandling.behandlingId}",
                      "person": {
                        "ident": "${oppgave.personIdent()}",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "kjonn": "UKJENT",
                        "statsborgerskap": "NOR",
                        "sikkerhetstiltak": [],
                        "skjermesSomEgneAnsatte": ${oppgave.person.skjermesSomEgneAnsatte}
                      },
                      "emneknagger": [],
                      "tilstand": "${OppgaveTilstandDTO.UNDER_BEHANDLING}",
                      "journalpostIder": ["123456789"],
                      "saksbehandler": {
                        "ident": "${TestHelper.saksbehandler.navIdent}"
                      },
                      "lovligeEndringer" : {
                          "paaVentAarsaker" : [ "AVVENT_SVAR", "AVVENT_DOKUMENTASJON", "AVVENT_MELDEKORT", "AVVENT_PERMITTERINGSÅRSAK", "AVVENT_RAPPORTERINGSFRIST", "AVVENT_SVAR_PÅ_FORESPØRSEL", "ANNET" ],
                          "avbrytAarsaker" : [ "BEHANDLES_I_ARENA", "FLERE_SØKNADER", "TRUKKET_SØKNAD", "ANNET" ]
                      },
                      "historikk": [
                        {
                          "type": "notat",
                          "tidspunkt": "2021-01-01T12:00:00",
                          "tittel": "Notat",
                          "behandler": {
                            "navn": "Ole Doffen",
                            "rolle": "beslutter"
                          },
                          "body": "Dette er et notat"
                        }
                      ]
                    }
                    """.trimIndent()
            }
        }
    }

    @Test
    fun `Får 404 Not Found ved forsøk på å hente oppgave som ikke finnes`() {
        val ikkeEksisterendeOppgaveId = UUIDv7.ny()
        val oppgaveMediator =
            mockk<OppgaveMediator>().also {
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
            mockk<OppgaveMediator>().also {
                every { it.finnOppgaverFor(TestHelper.personIdent) } returns
                    listOf(
                        TestHelper.lagOppgave(tilstand = Oppgave.FerdigBehandlet),
                        TestHelper.lagOppgave(tilstand = Oppgave.FerdigBehandlet),
                    )
            }
        withOppgaveApi(oppgaveMediatorMock) {
            client
                .post("/person/oppgaver") {
                    autentisert()
                    contentType(ContentType.Application.Json)
                    setBody(
                        //language=JSON
                        """{"ident": ${TestHelper.personIdent}}""",
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
        val forventetOppgaveIdDTO = OppgaveIdDTO(oppgaveId = oppgaveId)

        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.hentOppgaveIdFor(behandlingIdSomFinnes) } returns oppgaveId
                every { it.hentOppgaveIdFor(behandlingIdSomIkkeFinnes) } returns null
            }
        withOppgaveApi(oppgaveMediator = oppgaveMediatorMock) {
            client.get("/behandling/$behandlingIdSomFinnes/oppgaveId") { autentisert() }.also { response ->

                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                response.bodyAsText() shouldEqualSpecifiedJson
                    """
                    {
                        "oppgaveId": "$oppgaveId"
                    }
                    """.trimIndent()
                val oppgaveIdDTO =
                    objectMapper.readValue(
                        response.bodyAsText(),
                        object : TypeReference<OppgaveIdDTO>() {},
                    )
                oppgaveIdDTO shouldBe forventetOppgaveIdDTO
            }

            client.get("/behandling/$behandlingIdSomIkkeFinnes/oppgaveId") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.NotFound
                "${response.contentType()}" shouldContain "application/json"
                response.bodyAsText() shouldEqualSpecifiedJsonIgnoringOrder """{
                    "type" : "dagpenger.nav.no/saksbehandling:problem:ingen-oppgaveId-funnet",
                    "title" : "Ingen oppgaveId funnet",
                    "status" : 404,
                    "detail" : "Ingen oppgaveId funnet for behandlingId: $behandlingIdSomIkkeFinnes"
                }"""
            }
        }
    }

    @Test
    fun `Skal kunne hente person vha personId`() {
        val person = TestHelper.testPerson
        val personMediatorMock =
            mockk<PersonMediator>().also {
                every { it.hentPerson(person.id) } returns person
            }
        val forventetPersonOversikt =
            PersonOversiktDTO(
                person =
                    PersonDTO(
                        ident = person.ident,
                        id = person.id,
                        fornavn = "fornavn",
                        etternavn = "etternavn",
                        mellomnavn = null,
                        fodselsdato = LocalDate.MIN,
                        alder = 0,
                        statsborgerskap = null,
                        kjonn = KjonnDTO.KVINNE,
                        skjermesSomEgneAnsatte = person.skjermesSomEgneAnsatte,
                        adressebeskyttelseGradering = AdressebeskyttelseGraderingDTO.UGRADERT,
                        sikkerhetstiltak = listOf(),
                    ),
                saker = emptyList(),
                oppgaver = emptyList(),
            )
        val oppgaveDTOMapperMock =
            mockk<OppgaveDTOMapper>().also {
                coEvery { it.lagPersonOversiktDTO(person, emptyList()) } returns
                    forventetPersonOversikt
            }
        withOppgaveApi(
            personMediator = personMediatorMock,
            oppgaveDTOMapper = oppgaveDTOMapperMock,
        ) {
            client
                .get("/person/${person.id}") { autentisert() }
                .let { response ->
                    response.status shouldBe HttpStatusCode.OK
                    "${response.contentType()}" shouldContain "application/json"
                    val personOversiktDTO =
                        objectMapper.readValue(
                            response.bodyAsText(),
                            object : TypeReference<PersonOversiktDTO>() {},
                        )
                    personOversiktDTO shouldBe forventetPersonOversikt
                }
        }
    }

    @Test
    fun `Skal kunne hente personId vha fnr`() {
        val person = TestHelper.testPerson
        val personMediator =
            mockk<PersonMediator>().also {
                every { it.hentPerson(person.ident) } returns person
            }
        val forventetPersonIdDTO =
            PersonIdDTO(
                id = person.id,
            )
        withOppgaveApi(
            personMediator = personMediator,
            oppgaveDTOMapper = mockk<OppgaveDTOMapper>(),
        ) {
            client
                .post("/person/personId") {
                    autentisert()
                    contentType(ContentType.Application.Json)
                    setBody(
                        //language=JSON
                        """{"ident": "${person.ident}"}""",
                    )
                }.also { response ->
                    response.status shouldBe HttpStatusCode.OK
                    "${response.contentType()}" shouldContain "application/json"
                    response.bodyAsText() shouldEqualSpecifiedJson
                        """
                        {
                            "id": "${person.id}"
                        }
                        """.trimIndent()
                    val personIdDTO =
                        objectMapper.readValue(
                            response.bodyAsText(),
                            object : TypeReference<PersonIdDTO>() {},
                        )
                    personIdDTO shouldBe forventetPersonIdDTO
                }
        }
    }

    @Test
    fun `Skal kaste feil når person ikke finnes`() {
        val personMediator =
            mockk<PersonMediator>().also {
                every { it.hentPerson(any<String>()) } throws DataNotFoundException("Fant ikke person")
                every { it.hentPerson(any<UUID>()) } throws DataNotFoundException("Fant ikke person")
            }
        withOppgaveApi(
            personMediator = personMediator,
        ) {
            client
                .post("/person/personId") {
                    autentisert()
                    contentType(ContentType.Application.Json)
                    setBody(
                        //language=JSON
                        """{"ident": "123123"}""",
                    )
                }.also { response ->
                    response.status shouldBe HttpStatusCode.NotFound
                    response.bodyAsText() shouldEqualSpecifiedJsonIgnoringOrder """{
                    "type" : "dagpenger.nav.no/saksbehandling:problem:ressurs-ikke-funnet",
                    "title" : "Ressurs ikke funnet",
                    "status" : 404,
                    "detail" : "Fant ikke person"
                }"""
                }

            client
                .get("/person/${UUIDv7.ny()}") { autentisert() }
                .let { response ->
                    response.status shouldBe HttpStatusCode.NotFound
                    response.bodyAsText() shouldEqualSpecifiedJsonIgnoringOrder """{
                    "type" : "dagpenger.nav.no/saksbehandling:problem:ressurs-ikke-funnet",
                    "title" : "Ressurs ikke funnet",
                    "status" : 404,
                    "detail" : "Fant ikke person"
                }"""
                }
        }
    }
}
