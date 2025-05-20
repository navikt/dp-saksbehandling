package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.BehandlingType.KLAGE
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.SAKSBEHANDLER_IDENT
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.autentisert
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.defaultSaksbehandlerADGruppe
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.gyldigMaskinToken
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.gyldigSaksbehandlerToken
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageFerdigbehandletHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.lagBehandling
import no.nav.dagpenger.saksbehandling.lagOppgave
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class KlageApiTest {
    init {
        mockAzure()
    }

    private val klageBehandlingId = UUIDv7.ny()
    private val journalpostId = "journalpostId"
    private val opplysningId = UUIDv7.ny()
    private val saksbehandler =
        Saksbehandler(
            navIdent = SAKSBEHANDLER_IDENT,
            grupper = defaultSaksbehandlerADGruppe.toSet(),
            tilganger = setOf(TilgangType.SAKSBEHANDLER),
        )
    private val dato = LocalDateTime.of(2025, 1, 1, 1, 1)

    @Test
    fun `Skal kaste feil når det mangler autentisering`() {
        val mediator = mockk<KlageMediator>()
        withKlageApi(mediator) {
            client.get("klage/$klageBehandlingId").let { response ->
                response.status shouldBe HttpStatusCode.Unauthorized
            }
            client.post("klage/opprett") {
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "tullebody": "tull" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }

    @Test
    fun `Skal hente klageDTO`() {
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.hentKlageBehandling(
                        behandlingId = klageBehandlingId,
                        saksbehandler = saksbehandler,
                    )
                } returns
                    KlageBehandling.rehydrer(
                        behandlingId = klageBehandlingId,
                        journalpostId = journalpostId,
                        tilstand = KlageBehandling.Behandles,
                        behandlendeEnhet = null,
                    )
            }

        withKlageApi(mediator) {
            client.get("klage/$klageBehandlingId") { autentisert() }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                json shouldContain klageBehandlingId.toString()
            }
        }
    }

    private fun doKlageOpprettelseTest(token: String) {
        val oppgave = lagOppgave(behandling = lagBehandling(type = KLAGE), opprettet = dato)
        val ident = oppgave.behandling.person.ident
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.opprettKlage(
                        klageMottattHendelse =
                            KlageMottattHendelse(
                                ident = oppgave.behandling.person.ident,
                                opprettet = dato,
                                journalpostId = "journalpostId",
                            ),
                    )
                } returns oppgave
            }

        withKlageApi(mediator) {
            client.post("klage/opprett") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, "application/json")
                //language=json
                setBody(
                    """
                    {
                        "journalpostId": "journalpostId",
                        "opprettet": "$dato",
                        "sakId": "sakId",
                        "personIdent": {"ident":  "$ident"}
                    }
                    """.trimIndent(),
                )
            }.let { response ->
                response.status shouldBe HttpStatusCode.Created
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                json shouldEqualSpecifiedJsonIgnoringOrder //language=json
                    """
                    {
                       "oppgaveId": "${oppgave.oppgaveId}",
                       "behandlingId": "${oppgave.behandling.behandlingId}",
                       "personIdent": "$ident",
                       "tidspunktOpprettet": "2025-01-01T01:01:00",
                       "behandlingType": "KLAGE"
                    }
                    """.trimIndent()
            }
        }

        verify(exactly = 1) {
            mediator.opprettKlage(
                klageMottattHendelse =
                    KlageMottattHendelse(
                        ident = ident,
                        opprettet = dato,
                        journalpostId = "journalpostId",
                    ),
            )
        }
    }

    @Test
    fun `Skal kunne opprette en klage med saksbehandler token eller maskintoken`() {
        doKlageOpprettelseTest(gyldigSaksbehandlerToken())
        doKlageOpprettelseTest(gyldigMaskinToken())
    }

    @Test
    fun `Skal kunne trekke en klage`() {
        val avbruttHendelse =
            AvbruttHendelse(
                behandlingId = klageBehandlingId,
                utførtAv = saksbehandler,
            )
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.avbrytKlage(
                        hendelse = avbruttHendelse,
                    )
                } just Runs
            }

        withKlageApi(mediator) {
            client.put("klage/$klageBehandlingId/trekk") { autentisert() }.status shouldBe HttpStatusCode.NoContent
        }

        verify(exactly = 1) {
            mediator.avbrytKlage(hendelse = avbruttHendelse)
        }
    }

    @Test
    fun `Skal kunne ferdigstille en klage`() {
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.ferdigstill(
                        KlageFerdigbehandletHendelse(
                            behandlingId = klageBehandlingId,
                            utførtAv = saksbehandler,
                        ),
                    )
                } just Runs
            }

        withKlageApi(mediator) {
            client.put("klage/$klageBehandlingId/ferdigstill") { autentisert() }.status shouldBe HttpStatusCode.NoContent
        }

        verify(exactly = 1) {
            mediator.ferdigstill(
                KlageFerdigbehandletHendelse(
                    behandlingId = klageBehandlingId,
                    utførtAv = saksbehandler,
                ),
            )
        }
    }

    @Test
    fun `Skal kunne oppdatere en  opplysning av type flervalg`() {
        val tekstListe = Verdi.Flervalg("tekst1", "tekst2")
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.oppdaterKlageOpplysning(klageBehandlingId, opplysningId, tekstListe, saksbehandler)
                } returns Unit
            }
        withKlageApi(mediator) {
            client.put("klage/$klageBehandlingId/opplysning/$opplysningId") {
                autentisert()
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "verdi" : [ "tekst1", "tekst2" ], "type" : "FLER_LISTEVALG" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
                verify(exactly = 1) {
                    mediator.oppdaterKlageOpplysning(
                        behandlingId = klageBehandlingId,
                        opplysningId = opplysningId,
                        verdi = tekstListe,
                        saksbehandler = saksbehandler,
                    )
                }
            }
        }
    }

    @Test
    fun `Skal kunne oppdatere en opplysning av type tekst`() {
        val tekst = Verdi.TekstVerdi("tekst")
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.oppdaterKlageOpplysning(klageBehandlingId, opplysningId, tekst, saksbehandler)
                } returns Unit
            }
        withKlageApi(mediator) {
            client.put("klage/$klageBehandlingId/opplysning/$opplysningId") {
                autentisert()
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "verdi" : "tekst", "type" : "TEKST" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
                verify(exactly = 1) {
                    mediator.oppdaterKlageOpplysning(
                        behandlingId = klageBehandlingId,
                        opplysningId = opplysningId,
                        verdi = tekst,
                        saksbehandler = saksbehandler,
                    )
                }
            }
        }
    }

    @Test
    fun `Skal kunne oppdatere en opplysning av type boolean`() {
        val boolsk = Verdi.Boolsk(false)
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.oppdaterKlageOpplysning(klageBehandlingId, opplysningId, boolsk, saksbehandler)
                } returns Unit
            }
        withKlageApi(mediator) {
            client.put("klage/$klageBehandlingId/opplysning/$opplysningId") {
                autentisert()
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "verdi" : ${boolsk.value}, "type" : "BOOLSK" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
                verify(exactly = 1) {
                    mediator.oppdaterKlageOpplysning(
                        behandlingId = klageBehandlingId,
                        opplysningId = opplysningId,
                        verdi = boolsk,
                        saksbehandler = saksbehandler,
                    )
                }
            }
        }
    }

    @Test
    fun `Skal kunne oppdatere en opplysning av type dato`() {
        val dato = Verdi.Dato(LocalDate.of(2021, 1, 1))
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.oppdaterKlageOpplysning(klageBehandlingId, opplysningId, dato, saksbehandler)
                } returns Unit
            }
        withKlageApi(mediator) {
            client.put("klage/$klageBehandlingId/opplysning/$opplysningId") {
                autentisert()
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "verdi" : "2021-01-01", "type" : "DATO" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
                verify(exactly = 1) {
                    mediator.oppdaterKlageOpplysning(
                        behandlingId = klageBehandlingId,
                        opplysningId = opplysningId,
                        verdi = dato,
                        saksbehandler = saksbehandler,
                    )
                }
            }
        }
    }

    private val oppslagMock: Oppslag =
        mockk<Oppslag>().also {
            coEvery { it.hentBehandler(saksbehandler.navIdent) } returns
                BehandlerDTO(
                    ident = "navIdent",
                    fornavn = "fornavn",
                    etternavn = "etternavn",
                    enhet =
                        BehandlerDTOEnhetDTO(
                            navn = "navn",
                            enhetNr = "enhetNr",
                            postadresse = "postadresse",
                        ),
                )
        }

    private fun withKlageApi(
        klageMediator: KlageMediator,
        oppslag: Oppslag = oppslagMock,
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            this.application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    statistikkTjeneste = mockk(),
                    klageMediator = klageMediator,
                    klageDTOMapper = KlageDTOMapper(oppslag = oppslag),
                )
            }
            test()
        }
    }
}
