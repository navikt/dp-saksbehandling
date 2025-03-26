package no.nav.dagpenger.saksbehandling.api

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.delete
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
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.Emneknagg.PåVent.AVVENT_RAPPORTERINGSFRIST
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.Companion.søkbareTilstander
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UlovligTilstandsendringException
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.BESLUTTER_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.SAKSBEHANDLER_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.SOKNAD_ID
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.TEST_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.autentisert
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.gyldigMaskinToken
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.gyldigSaksbehandlerToken
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.lagTestOppgaveMedTilstand
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.lagTestOppgaveMedTilstandOgBehandling
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.testPerson
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.withOppgaveApi
import no.nav.dagpenger.saksbehandling.api.models.AdressebeskyttelseGraderingDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerEnhetDTO
import no.nav.dagpenger.saksbehandling.api.models.KjonnDTO
import no.nav.dagpenger.saksbehandling.api.models.LovligeEndringerDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkBehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktResultatDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonDTO
import no.nav.dagpenger.saksbehandling.api.models.SikkerhetstiltakDTO
import no.nav.dagpenger.saksbehandling.api.models.UtsettOppgaveAarsakDTO
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKreverIkkeTotrinnskontrollException
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SlettNotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
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

    private val saksbehandler =
        Saksbehandler(
            SAKSBEHANDLER_IDENT,
            setOf(Configuration.saksbehandlerADGruppe),
            setOf(
                SAKSBEHANDLER,
            ),
        )

    private val beslutter =
        Saksbehandler(
            BESLUTTER_IDENT,
            setOf(Configuration.saksbehandlerADGruppe, Configuration.beslutterADGruppe),
            setOf(BESLUTTER, SAKSBEHANDLER),
        )
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
                Arguments.of("/oppgave/oppgaveId/tildel", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/notat", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/utsett", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/legg-tilbake", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/send-til-kontroll", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/returner-til-saksbehandler", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/ferdigstill/melding-om-vedtak", HttpMethod.Put),
                Arguments.of("/oppgave/oppgaveId/ferdigstill/melding-om-vedtak-arena", HttpMethod.Put),
                Arguments.of("/person/oppgaver", HttpMethod.Post),
                Arguments.of("/behandling/behandlingId/oppgaveId", HttpMethod.Get),
            )
        }
    }

    @Test
    fun `Skal avvise kall uten gyldig maskin til maskin token`() {
        withOppgaveApi {
            client.request("/person/skal-varsle-om-ettersending") {
                method = HttpMethod.Post
                autentisert(token = ugyldigToken)
            }.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @ParameterizedTest
    @MethodSource("endepunktOgHttpMetodeProvider")
    fun `Skal avvise kall uten gyldig saksbehandler token`(
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
                } returns PostgresOppgaveRepository.OppgaveSøkResultat(listOf(oppgave1, oppgave2), 2)
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
                          "personIdent": "${oppgave1.behandling.person.ident}",
                          "emneknagger": [],
                          "skjermesSomEgneAnsatte": ${oppgave1.behandling.person.skjermesSomEgneAnsatte},
                          "adressebeskyttelseGradering": "${AdressebeskyttelseGraderingDTO.UGRADERT}",
                          "tilstand": "${OppgaveTilstandDTO.KLAR_TIL_BEHANDLING}",
                          "behandlerIdent": "${oppgave1.behandlerIdent}",
                          "utsattTilDato": "${oppgave1.utsattTil()}"
                        },
                        {
                          "oppgaveId": "${oppgave2.oppgaveId}",
                          "behandlingId": "${oppgave2.behandling.behandlingId}",
                          "personIdent": "${oppgave2.behandling.person.ident}",
                          "emneknagger": [],
                          "skjermesSomEgneAnsatte": ${oppgave2.behandling.person.skjermesSomEgneAnsatte},
                          "adressebeskyttelseGradering": "${AdressebeskyttelseGraderingDTO.UGRADERT}",
                          "tilstand": "${OppgaveTilstandDTO.KLAR_TIL_BEHANDLING}"
                        }
                      ],
                      "totaltAntallOppgaver" : 2
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
                        lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
                        lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
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
            client.get("/oppgave?tilstand=KLAR_TIL_BEHANDLING&tilstand=UNDER_BEHANDLING") { autentisert() }
                .let { response ->
                    response.status shouldBe HttpStatusCode.OK
                    "${response.contentType()}" shouldContain "application/json"
                    val oppgaveOversiktResultatDTO =
                        objectMapper.readValue(
                            response.bodyAsText(),
                            object : TypeReference<OppgaveOversiktResultatDTO>() {},
                        )
                    oppgaveOversiktResultatDTO.totaltAntallOppgaver shouldBe 2
                    oppgaveOversiktResultatDTO.oppgaver?.size shouldBe 2
                }
        }
    }

    @Test
    fun `Hent alle oppgaver basert på emneknagg`() {
        val søkResultat =
            listOf(
                lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
                lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING),
            ).let {
                PostgresOppgaveRepository.OppgaveSøkResultat(it, it.size)
            }

        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.søk(
                        Søkefilter(
                            periode = Periode.UBEGRENSET_PERIODE,
                            tilstander = setOf(KLAR_TIL_BEHANDLING),
                            emneknagger = setOf("TULLBALL", "KLAGE"),
                        ),
                    )
                } returns søkResultat
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client.get("/oppgave?tilstand=${KLAR_TIL_BEHANDLING}&emneknagg=TULLBALL&emneknagg=KLAGE") { autentisert() }
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
                lagTestOppgaveMedTilstand(UNDER_BEHANDLING),
                lagTestOppgaveMedTilstand(UNDER_BEHANDLING),
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
                            saksbehandlerIdent = SAKSBEHANDLER_IDENT,
                        ),
                    )
                } returns søkResultat
            }
        withOppgaveApi(oppgaveMediatorMock) {
            client.get("/oppgave?tilstand=$UNDER_BEHANDLING&fom=2021-01-01&tom=2023-01-01&mineOppgaver=true") { autentisert() }
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
                navIdent = beslutter.navIdent,
            )
        val notat = "Dette er et notat"
        val sisteEndretTidspunkt = LocalDateTime.of(2021, 1, 1, 12, 0)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.lagreNotat(NotatHendelse(oppgaveId, notat, beslutter))
                } returns sisteEndretTidspunkt
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client.put("/oppgave/$oppgaveId/notat") {
                autentisert(token = beslutterToken)
                setBody(notat)
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
                navIdent = beslutter.navIdent,
            )
        val sisteEndretTidspunkt = LocalDateTime.of(2021, 1, 1, 12, 0)
        val slettNotatHendelse = SlettNotatHendelse(oppgaveId, beslutter)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every {
                    it.slettNotat(slettNotatHendelse)
                } returns sisteEndretTidspunkt
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client.delete("/oppgave/$oppgaveId/notat") {
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
    fun `Skal kunne ferdigstille en oppgave med melding om vedtak v2`() {
        val oppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING, SAKSBEHANDLER_IDENT)
        val saksbehandlerToken = gyldigSaksbehandlerToken(navIdent = SAKSBEHANDLER_IDENT)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                coEvery { it.ferdigstillOppgave(oppgave.oppgaveId, any(), saksbehandlerToken) } just Runs
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client.put("/oppgave/${oppgave.oppgaveId}/ferdigstill/melding-om-vedtak") {
                autentisert(token = saksbehandlerToken)
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }

            coVerify(exactly = 1) {
                oppgaveMediatorMock.ferdigstillOppgave(oppgave.oppgaveId, any(), saksbehandlerToken)
            }
        }
    }

    @Test
    fun `Feilhåndtering for melding om vedtak v2`() {
        val oppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING, SAKSBEHANDLER_IDENT)
        val saksbehandlerToken = gyldigSaksbehandlerToken(navIdent = SAKSBEHANDLER_IDENT)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                coEvery {
                    it.ferdigstillOppgave(any<UUID>(), any(), any())
                } throws MeldingOmVedtakKlient.KanIkkeLageMeldingOmVedtak("Testmelding")
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client.put("/oppgave/${oppgave.oppgaveId}/ferdigstill/melding-om-vedtak") {
                autentisert(token = saksbehandlerToken)
            }.let { response ->
                response.status shouldBe HttpStatusCode.InternalServerError
                response.bodyAsText() shouldContain "Feil ved laging av melding om vedtak"
            }

            coVerify(exactly = 1) {
                oppgaveMediatorMock.ferdigstillOppgave(oppgave.oppgaveId, any(), saksbehandlerToken)
            }
        }
    }

    @Test
    fun `Skal kunne sende en oppgave til kontroll`() {
        val oppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING, SAKSBEHANDLER_IDENT)
        val saksbehandlerToken = gyldigSaksbehandlerToken(navIdent = SAKSBEHANDLER_IDENT)
        val sendTilKontrollHendelse = SendTilKontrollHendelse(oppgave.oppgaveId, saksbehandler)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.sendTilKontroll(sendTilKontrollHendelse, saksbehandlerToken) } just Runs
            }
        withOppgaveApi(oppgaveMediatorMock) {
            client.put("/oppgave/${oppgave.oppgaveId}/send-til-kontroll") {
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
        val oppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING, SAKSBEHANDLER_IDENT)
        val saksbehandlerToken = gyldigSaksbehandlerToken(navIdent = SAKSBEHANDLER_IDENT)
        val sendTilKontrollHendelse = SendTilKontrollHendelse(oppgave.oppgaveId, saksbehandler)
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
            client.put("/oppgave/${oppgave.oppgaveId}/send-til-kontroll") {
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
        val oppgave = lagTestOppgaveMedTilstand(UNDER_KONTROLL, BESLUTTER_IDENT)
        val beslutterToken =
            gyldigSaksbehandlerToken(navIdent = BESLUTTER_IDENT, adGrupper = listOf(Configuration.beslutterADGruppe))
        val returnerTilSaksbehandlingHendelse = ReturnerTilSaksbehandlingHendelse(oppgave.oppgaveId, beslutter)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.returnerTilSaksbehandling(returnerTilSaksbehandlingHendelse, any()) } just Runs
            }
        withOppgaveApi(oppgaveMediatorMock) {
            client.put("/oppgave/${oppgave.oppgaveId}/returner-til-saksbehandler") {
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
    fun `Skal kunne ferdigstille en oppgave med melding om vedtak i Arena`() {
        val oppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING, SAKSBEHANDLER_IDENT)
        val saksbehandlerToken = gyldigSaksbehandlerToken()
        val godkjennBehandlingMedBrevIArena =
            GodkjennBehandlingMedBrevIArena(
                oppgaveId = oppgave.oppgaveId,
                utførtAv = saksbehandler,
            )
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.ferdigstillOppgave(godkjennBehandlingMedBrevIArena, any()) } just Runs
            }

        withOppgaveApi(oppgaveMediatorMock) {
            client.put("/oppgave/${oppgave.oppgaveId}/ferdigstill/melding-om-vedtak-arena") {
                autentisert(token = saksbehandlerToken)
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
            }
            verify(exactly = 1) {
                oppgaveMediatorMock.ferdigstillOppgave(godkjennBehandlingMedBrevIArena, any())
            }
        }
    }

    @Test
    fun `Skal kunne hente og få tildelt neste oppgave`() {
        val oppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING, SAKSBEHANDLER_IDENT)
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.tildelOgHentNesteOppgave(any(), any()) } returns oppgave
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
                      "behandlingId": "${oppgave.behandling.behandlingId}",
                      "person": {
                        "ident": "$TEST_IDENT",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "kjonn": "UKJENT",
                        "statsborgerskap": "NOR",
                        "skjermesSomEgneAnsatte": ${oppgave.behandling.person.skjermesSomEgneAnsatte}
                      },
                      "emneknagger": [],
                      "tilstand": "${OppgaveTilstandDTO.UNDER_BEHANDLING}",
                      "soknadId": "${SOKNAD_ID}"
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
    fun `Saksbehandler skal kunne ta en oppgave som er KLAR_TIL_BEHANDLING`() {
        val oppgaveMediatorMock = mockk<OppgaveMediator>()
        val testOppgave = lagTestOppgaveMedTilstand(KLAR_TIL_BEHANDLING)

        coEvery {
            oppgaveMediatorMock.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = testOppgave.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )
        } returns Oppgave.UnderBehandling

        withOppgaveApi(oppgaveMediatorMock) {
            client.put("/oppgave/${testOppgave.oppgaveId}/tildel") { autentisert() }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                response.bodyAsText() shouldEqualSpecifiedJson
                    """
                    {
                      "nyTilstand" : "${OppgaveTilstandDTO.UNDER_BEHANDLING}"
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
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )
        } throws DataNotFoundException("Oppgave ikke funnet")

        coEvery {
            oppgaveMediatorMock.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgaveSomAlleredeErUnderKontroll,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )
        } throws UlovligTilstandsendringException("Oppgaven er allerede under kontroll")

        withOppgaveApi(oppgaveMediatorMock) {
            client.put("/oppgave/$oppgaveSomIkkeFinnes/tildel") { autentisert() }
                .also { response ->
                    response.status shouldBe HttpStatusCode.NotFound
                }
            client.put("/oppgave/$oppgaveSomAlleredeErUnderKontroll/tildel") { autentisert() }
                .also { response ->
                    response.status shouldBe HttpStatusCode.Conflict
                }
        }
    }

    @Test
    fun `Saksbehandler skal kunne gi fra seg ansvar for en oppgave`() {
        val oppgaveMediatorMock = mockk<OppgaveMediator>()
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
        val oppgaveMediatorMock = mockk<OppgaveMediator>()
        val testOppgave = lagTestOppgaveMedTilstand(UNDER_BEHANDLING)
        val utsettTilDato = LocalDate.now().plusDays(1)
        val utsettOppgaveHendelse =
            UtsettOppgaveHendelse(
                oppgaveId = testOppgave.oppgaveId,
                navIdent = saksbehandler.navIdent,
                utsattTil = utsettTilDato,
                beholdOppgave = true,
                utførtAv = saksbehandler,
                årsak = AVVENT_RAPPORTERINGSFRIST,
            )

        coEvery { oppgaveMediatorMock.hentOppgave(any(), any()) } returns testOppgave
        coEvery {
            oppgaveMediatorMock.utsettOppgave(utsettOppgaveHendelse)
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
    fun `Hent oppgave med tilhørende personinfo, sikkerhetstiltak og journalpostIder`() {
        val oppgaveMediatorMock = mockk<OppgaveMediator>()
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
        val oppgaveDTOMapper =
            mockk<OppgaveDTOMapper>().also {
                val tidspunkt = LocalDateTime.of(2021, 1, 1, 12, 0)
                coEvery { it.lagOppgaveDTO(testOppgave) } returns
                    OppgaveDTO(
                        oppgaveId = testOppgave.oppgaveId,
                        behandlingId = testOppgave.behandling.behandlingId,
                        person =
                            PersonDTO(
                                ident = testPerson.ident,
                                fornavn = testPerson.fornavn,
                                etternavn = testPerson.etternavn,
                                fodselsdato = testPerson.fødselsdato,
                                alder = testPerson.alder,
                                kjonn = KjonnDTO.UKJENT,
                                skjermesSomEgneAnsatte = testOppgave.behandling.person.skjermesSomEgneAnsatte,
                                adressebeskyttelseGradering = AdressebeskyttelseGraderingDTO.UGRADERT,
                                mellomnavn = testPerson.mellomnavn,
                                statsborgerskap = testPerson.statsborgerskap,
                                sikkerhetstiltak =
                                    listOf(
                                        SikkerhetstiltakDTO(
                                            beskrivelse = testPerson.sikkerhetstiltak.first().beskrivelse,
                                            gyldigTom = testPerson.sikkerhetstiltak.first().gyldigTom,
                                        ),
                                    ),
                            ),
                        tidspunktOpprettet = testOppgave.opprettet,
                        emneknagger = testOppgave.emneknagger.toList(),
                        tilstand = testOppgave.tilstand().tilOppgaveTilstandDTO(),
                        saksbehandler =
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
                            ),
                        beslutter =
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
                            ),
                        utsattTilDato = null,
                        journalpostIder = listOf("123456789"),
                        historikk =
                            listOf(
                                OppgaveHistorikkDTO(
                                    type = OppgaveHistorikkDTO.Type.notat,
                                    tidspunkt = tidspunkt,
                                    behandler =
                                        OppgaveHistorikkBehandlerDTO(
                                            navn = "Ole Doffen",
                                            rolle = OppgaveHistorikkBehandlerDTO.Rolle.beslutter,
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
                                        UNDER_BEHANDLING -> UtsettOppgaveAarsakDTO.entries.map { it.value }
                                        else -> emptyList()
                                    },
                            ),
                    )
            }

        withOppgaveApi(
            oppgaveMediator = oppgaveMediatorMock,
            oppgaveDTOMapper = oppgaveDTOMapper,
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
                        "ident": "${testOppgave.behandling.person.ident}",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "kjonn": "UKJENT",
                        "statsborgerskap": "NOR",
                        "sikkerhetstiltak": [
                          {
                            "beskrivelse": "${testPerson.sikkerhetstiltak.first().beskrivelse}",
                            "gyldigTom": "${testPerson.sikkerhetstiltak.first().gyldigTom}"
                          }
                        ],
                        "skjermesSomEgneAnsatte": ${testOppgave.behandling.person.skjermesSomEgneAnsatte}
                      },
                      "emneknagger": [],
                      "tilstand": "${OppgaveTilstandDTO.UNDER_KONTROLL}",
                      "journalpostIder": ["123456789"],
                      "saksbehandler": {
                        "ident": "${saksbehandler.navIdent}"
                      },
                      "beslutter": {
                        "ident": "${beslutter.navIdent}"
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
        val oppgaveMediatorMock = mockk<OppgaveMediator>()
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
        val oppgaveDTOMapper =
            mockk<OppgaveDTOMapper>().also {
                val tidspunkt = LocalDateTime.of(2021, 1, 1, 12, 0)
                coEvery { it.lagOppgaveDTO(testOppgave) } returns
                    OppgaveDTO(
                        oppgaveId = testOppgave.oppgaveId,
                        behandlingId = testOppgave.behandling.behandlingId,
                        person =
                            PersonDTO(
                                ident = testPerson.ident,
                                fornavn = testPerson.fornavn,
                                etternavn = testPerson.etternavn,
                                fodselsdato = testPerson.fødselsdato,
                                alder = testPerson.alder,
                                kjonn = KjonnDTO.UKJENT,
                                skjermesSomEgneAnsatte = testOppgave.behandling.person.skjermesSomEgneAnsatte,
                                adressebeskyttelseGradering = AdressebeskyttelseGraderingDTO.UGRADERT,
                                mellomnavn = testPerson.mellomnavn,
                                statsborgerskap = testPerson.statsborgerskap,
                                sikkerhetstiltak = emptyList(),
                            ),
                        tidspunktOpprettet = testOppgave.opprettet,
                        emneknagger = testOppgave.emneknagger.toList(),
                        tilstand = testOppgave.tilstand().tilOppgaveTilstandDTO(),
                        saksbehandler =
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
                            ),
                        beslutter =
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
                            ),
                        utsattTilDato = null,
                        journalpostIder = listOf("123456789"),
                        historikk =
                            listOf(
                                OppgaveHistorikkDTO(
                                    type = OppgaveHistorikkDTO.Type.notat,
                                    tidspunkt = tidspunkt,
                                    behandler =
                                        OppgaveHistorikkBehandlerDTO(
                                            navn = "Ole Doffen",
                                            rolle = OppgaveHistorikkBehandlerDTO.Rolle.beslutter,
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
                                        UNDER_BEHANDLING -> UtsettOppgaveAarsakDTO.entries.map { it.value }
                                        else -> emptyList()
                                    },
                            ),
                    )
            }

        withOppgaveApi(
            oppgaveMediator = oppgaveMediatorMock,
            oppgaveDTOMapper = oppgaveDTOMapper,
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
                        "ident": "${testOppgave.behandling.person.ident}",
                        "fornavn": "PETTER",
                        "etternavn": "SMART",
                        "fodselsdato": "2000-01-01",
                        "kjonn": "UKJENT",
                        "statsborgerskap": "NOR",
                        "sikkerhetstiltak": [],
                        "skjermesSomEgneAnsatte": ${testOppgave.behandling.person.skjermesSomEgneAnsatte}
                      },
                      "emneknagger": [],
                      "tilstand": "${OppgaveTilstandDTO.UNDER_KONTROLL}",
                      "journalpostIder": ["123456789"],
                      "saksbehandler": {
                        "ident": "${saksbehandler.navIdent}"
                      },
                      "beslutter": {
                        "ident": "${beslutter.navIdent}"
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

    @Test
    fun `Skal sjekke om det finnes korresponderende oppgave som saksbehandler har sett på`() {
        val søknadId = UUIDv7.ny()
        val søknadIdFalse = UUIDv7.ny()
        val oppgaveMediatorMock =
            mockk<OppgaveMediator>().also {
                every { it.skalEttersendingTilSøknadVarsles(søknadId = søknadId, ident = TEST_IDENT) } returns true
                every {
                    it.skalEttersendingTilSøknadVarsles(
                        søknadId = søknadIdFalse,
                        ident = TEST_IDENT,
                    )
                } returns false
            }
        withOppgaveApi(oppgaveMediatorMock) {
            client.post("/person/skal-varsle-om-ettersending") {
                autentisert(token = gyldigMaskinToken())
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """{"ident": "$TEST_IDENT", 
                        "soknadId":  "$søknadId"}
                    """.trimMargin(),
                )
            }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "true"
            }

            client.post("/person/skal-varsle-om-ettersending") {
                autentisert(token = gyldigMaskinToken())
                contentType(ContentType.Application.Json)
                setBody(
                    //language=JSON
                    """{"ident": "$TEST_IDENT", 
                        "soknadId":  "$søknadIdFalse"}
                    """.trimMargin(),
                )
            }.also { response ->
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "false"
            }
        }
    }
}
