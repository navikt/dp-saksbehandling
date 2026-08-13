package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import io.kotest.matchers.collections.shouldHaveSize
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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.aktivitetslogg.AuditOperasjon
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.autentisert
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.gyldigMaskinToken
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.gyldigSaksbehandlerToken
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTOEnhetDTO
import no.nav.dagpenger.saksbehandling.audit.Auditlogg
import no.nav.dagpenger.saksbehandling.audit.TestAuditlogg
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageBehandlingFerdigstilt
import no.nav.dagpenger.saksbehandling.hendelser.KlageBehandlingUtført
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageinstansVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellKlageMottattHendelse
import no.nav.dagpenger.saksbehandling.klage.FormkravSteg
import no.nav.dagpenger.saksbehandling.klage.FristvurderingSteg
import no.nav.dagpenger.saksbehandling.klage.FullmektigSteg
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.Companion.rehydrer
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLING_UTFORT
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.FERDIGSTILT
import no.nav.dagpenger.saksbehandling.klage.KlageTilstandslogg
import no.nav.dagpenger.saksbehandling.klage.KlageinstansVedtak
import no.nav.dagpenger.saksbehandling.klage.KlagenGjelderSteg
import no.nav.dagpenger.saksbehandling.klage.OpplysningBygger
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.klage.OversendKlageinstansSteg
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.klage.VurderUtfallSteg
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class KlageApiTest {
    private val klageBehandlingId = UUIDv7.ny()
    private val ident = "12345612345"
    private val sakId = UUIDv7.ny()
    private val opplysningId = UUIDv7.ny()
    private val opprettet = LocalDateTime.of(2025, 1, 1, 1, 1)

    @Test
    fun `Skal kaste feil når det mangler autentisering`() {
        val mediator = mockk<KlageMediator>()
        withKlageApi(mediator) {
            client.get("klage/$klageBehandlingId").status shouldBe HttpStatusCode.Unauthorized
            client
                .post("klage/opprett") {
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
        val klageBehandling =
            rehydrer(
                behandlingId = klageBehandlingId,
                opprettet = opprettet,
                opplysninger = OpplysningBygger.lagOpplysninger(OpplysningType.entries.toSet()),
                tilstand = KlageBehandling.Ferdigstilt,
                journalpostId = null,
                behandlendeEnhet = "4449",
                tilstandslogg =
                    KlageTilstandslogg(
                        Tilstandsendring(
                            tilstand = BEHANDLES,
                            hendelse =
                                KlageMottattHendelse(
                                    ident = ident,
                                    opprettet = opprettet,
                                    journalpostId = "JP1",
                                    sakId = sakId,
                                ),
                        ),
                        Tilstandsendring(
                            tilstand = BEHANDLING_UTFORT,
                            hendelse =
                                KlageBehandlingUtført(
                                    behandlingId = klageBehandlingId,
                                    utførtAv = TestHelper.saksbehandler,
                                ),
                        ),
                        Tilstandsendring(
                            tilstand = FERDIGSTILT,
                            hendelse =
                                KlageinstansVedtakHendelse(
                                    type = KlageinstansVedtakHendelse.KlageinstansVedtakType.KLAGE,
                                    klageId = klageBehandlingId,
                                    klageinstansVedtakId = UUIDv7.ny(),
                                    avsluttet = LocalDateTime.now(),
                                    utfall = "MEDHOLD",
                                    journalpostIder = listOf("KA12345"),
                                ),
                        ),
                    ),
                steg =
                    listOf(
                        KlagenGjelderSteg,
                        FristvurderingSteg,
                        FormkravSteg,
                        VurderUtfallSteg,
                        OversendKlageinstansSteg,
                        FullmektigSteg,
                    ),
                klageinstansVedtak =
                    KlageinstansVedtak.from(
                        KlageinstansVedtakHendelse(
                            type = KlageinstansVedtakHendelse.KlageinstansVedtakType.KLAGE,
                            klageId = klageBehandlingId,
                            klageinstansVedtakId = sakId,
                            avsluttet = LocalDateTime.now(),
                            utfall = "MEDHOLD",
                            journalpostIder = listOf("12345"),
                        ),
                    ),
            )
        val klageMediator =
            mockk<KlageMediator>().also {
                every {
                    it.hentKlageBehandling(
                        behandlingId = klageBehandlingId,
                        saksbehandler = TestHelper.saksbehandler,
                    )
                } returns klageBehandling
            }

        withKlageApi(klageMediator) {
            client.get("klage/$klageBehandlingId") { autentisert() }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                "${response.contentType()}" shouldContain "application/json"
                val json = response.bodyAsText()
                json shouldContain klageBehandlingId.toString()
            }
        }
    }

    @Test
    fun `Skal kunne opprette en klage med maskintoken`() {
        val token = gyldigMaskinToken()
        val sakId = UUIDv7.ny()
        val oppgave =
            TestHelper.lagOppgave(
                behandling = TestHelper.lagBehandling(utløstAvType = HendelseBehandler.Intern.Klage),
                opprettet = opprettet,
            )
        val ident = oppgave.personIdent()
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.opprettKlage(
                        klageMottattHendelse =
                            KlageMottattHendelse(
                                ident = oppgave.personIdent(),
                                sakId = sakId,
                                opprettet = opprettet,
                                journalpostId = "journalpostId",
                            ),
                    )
                } returns oppgave
            }

        withKlageApi(mediator) {
            client
                .post("klage/opprett") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.ContentType, "application/json")
                    //language=json
                    setBody(
                        """
                        {
                            "journalpostId": "journalpostId",
                            "opprettet": "$opprettet",
                            "sakId": "$sakId",
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
                           "utlostAv": "KLAGE"
                        }
                        """.trimIndent()
                }
        }

        verify(exactly = 1) {
            mediator.opprettKlage(
                klageMottattHendelse =
                    KlageMottattHendelse(
                        ident = ident,
                        sakId = sakId,
                        opprettet = opprettet,
                        journalpostId = "journalpostId",
                    ),
            )
        }
    }

    @Test
    fun `Skal kunne opprette en manuell klage med saksbehandlertoken`() {
        val token = gyldigSaksbehandlerToken()
        val oppgave =
            TestHelper.lagOppgave(
                behandling = TestHelper.lagBehandling(utløstAvType = HendelseBehandler.Intern.Klage),
                opprettet = opprettet,
            )
        val ident = oppgave.personIdent()
        val sakId = UUIDv7.ny()
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.opprettManuellKlage(
                        manuellKlageMottattHendelse =
                            ManuellKlageMottattHendelse(
                                ident = oppgave.personIdent(),
                                sakId = sakId,
                                opprettet = opprettet,
                                journalpostId = "journalpostId",
                                utførtAv = TestHelper.saksbehandler,
                            ),
                    )
                } returns oppgave
            }

        withKlageApi(mediator) {
            client
                .post("klage/opprett-manuelt") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.ContentType, "application/json")
                    //language=json
                    setBody(
                        """
                        {
                            "journalpostId": "journalpostId",
                            "opprettet": "$opprettet",
                            "sakId": "$sakId",
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
                           "utlostAv": "KLAGE"
                        }
                        """.trimIndent()
                }
        }

        verify(exactly = 1) {
            mediator.opprettManuellKlage(
                manuellKlageMottattHendelse =
                    ManuellKlageMottattHendelse(
                        ident = ident,
                        sakId = sakId,
                        opprettet = opprettet,
                        journalpostId = "journalpostId",
                        utførtAv = TestHelper.saksbehandler,
                    ),
            )
        }
    }

    @Test
    fun `Skal ikke kunne opprette klager med feil type token`() {
        val saksbehandlerToken = gyldigSaksbehandlerToken()
        val maskinToken = gyldigMaskinToken()

        val mediatorMock = mockk<KlageMediator>()

        withKlageApi(mediatorMock) {
            client
                .post("klage/opprett") {
                    header(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
                    header(HttpHeaders.ContentType, "application/json")
                    //language=json
                    setBody(
                        """
                        {
                            "ikke": "så viktig"
                        }
                        """.trimIndent(),
                    )
                }.let { response ->
                    response.status shouldBe HttpStatusCode.Unauthorized
                }
            client
                .post("klage/opprett-manuelt") {
                    header(HttpHeaders.Authorization, "Bearer $maskinToken")
                    header(HttpHeaders.ContentType, "application/json")
                    //language=json
                    setBody(
                        """
                        {
                            "ikke": "så viktig"
                        }
                        """.trimIndent(),
                    )
                }.let { response ->
                    response.status shouldBe HttpStatusCode.Unauthorized
                }
        }
    }

    @Test
    fun `Skal kunne trekke en klage`() {
        val avbruttHendelse =
            AvbruttHendelse(
                behandlingId = klageBehandlingId,
                utførtAv = TestHelper.saksbehandler,
            )
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.avbrytKlage(
                        hendelse = avbruttHendelse,
                    )
                } returns mockk<KlageBehandling>(relaxed = true)
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
        val saksbehandlerToken = gyldigSaksbehandlerToken()
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.behandlingUtført(
                        klageBehandlingUtført =
                            KlageBehandlingUtført(
                                behandlingId = klageBehandlingId,
                                utførtAv = TestHelper.saksbehandler,
                            ),
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } returns mockk<KlageBehandling>(relaxed = true)
            }

        withKlageApi(mediator) {
            client
                .put("klage/$klageBehandlingId/ferdigstill") { autentisert(token = saksbehandlerToken) }
                .status shouldBe HttpStatusCode.NoContent
        }

        verify(exactly = 1) {
            mediator.behandlingUtført(
                klageBehandlingUtført =
                    KlageBehandlingUtført(
                        behandlingId = klageBehandlingId,
                        utførtAv = TestHelper.saksbehandler,
                    ),
                saksbehandlerToken = saksbehandlerToken,
            )
        }
    }

    @Test
    fun `Skal kunne ferdigstille behandling av klage med medhold (steg 1)`() {
        val saksbehandlerToken = gyldigSaksbehandlerToken()
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.ferdigstillBehandling(
                        hendelse =
                            KlageBehandlingFerdigstilt(
                                behandlingId = klageBehandlingId,
                                utførtAv = TestHelper.saksbehandler,
                            ),
                    )
                } returns mockk<KlageBehandling>(relaxed = true)
            }

        withKlageApi(mediator) {
            client
                .put("klage/$klageBehandlingId/ferdigstill-behandling") { autentisert(token = saksbehandlerToken) }
                .status shouldBe HttpStatusCode.NoContent
        }

        verify(exactly = 1) {
            mediator.ferdigstillBehandling(
                hendelse =
                    KlageBehandlingFerdigstilt(
                        behandlingId = klageBehandlingId,
                        utførtAv = TestHelper.saksbehandler,
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
                    it.oppdaterKlageOpplysning(klageBehandlingId, opplysningId, tekstListe, TestHelper.saksbehandler)
                } returns mockk<KlageBehandling>(relaxed = true)
            }
        withKlageApi(mediator) {
            client
                .put("klage/$klageBehandlingId/opplysning/$opplysningId") {
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
                            saksbehandler = TestHelper.saksbehandler,
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
                    it.oppdaterKlageOpplysning(klageBehandlingId, opplysningId, tekst, TestHelper.saksbehandler)
                } returns mockk<KlageBehandling>(relaxed = true)
            }
        withKlageApi(mediator) {
            client
                .put("klage/$klageBehandlingId/opplysning/$opplysningId") {
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
                            saksbehandler = TestHelper.saksbehandler,
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
                    it.oppdaterKlageOpplysning(klageBehandlingId, opplysningId, boolsk, TestHelper.saksbehandler)
                } returns mockk<KlageBehandling>(relaxed = true)
            }
        withKlageApi(mediator) {
            client
                .put("klage/$klageBehandlingId/opplysning/$opplysningId") {
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
                            saksbehandler = TestHelper.saksbehandler,
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
                    it.oppdaterKlageOpplysning(klageBehandlingId, opplysningId, dato, TestHelper.saksbehandler)
                } returns mockk<KlageBehandling>(relaxed = true)
            }
        withKlageApi(mediator) {
            client
                .put("klage/$klageBehandlingId/opplysning/$opplysningId") {
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
                            saksbehandler = TestHelper.saksbehandler,
                        )
                    }
                }
        }
    }

    private val oppslagMock: Oppslag =
        mockk<Oppslag>().also {
            coEvery { it.hentBehandler(TestHelper.saksbehandler.navIdent) } returns
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

    @Test
    fun `Skal auditlogge READ ved visning av klagebehandling`() {
        val auditlogg = TestAuditlogg()
        val klageBehandling =
            mockk<KlageBehandling>(relaxed = true).also {
                every { it.behandlingId } returns klageBehandlingId
                every { it.personIdent() } returns "12345678901"
            }
        val mediator =
            mockk<KlageMediator>().also {
                every { it.hentKlageBehandling(klageBehandlingId, any()) } returns klageBehandling
            }

        withKlageApi(mediator, auditlogg = auditlogg) {
            client.get("klage/$klageBehandlingId") { autentisert() }
        }

        auditlogg.hendelser shouldHaveSize 1
        auditlogg.hendelser.first().let {
            it.operasjon shouldBe AuditOperasjon.READ
            it.melding shouldBe "Så en klagebehandling"
            it.ident shouldBe "12345678901"
            it.saksbehandler shouldBe TestHelper.saksbehandler.navIdent
        }
    }

    @Test
    fun `Skal auditlogge UPDATE ved avbryt av klage`() {
        val auditlogg = TestAuditlogg()
        val klageBehandling =
            mockk<KlageBehandling>(relaxed = true).also {
                every { it.personIdent() } returns "12345678901"
            }
        val mediator =
            mockk<KlageMediator>(relaxed = true).also {
                every { it.avbrytKlage(any()) } returns klageBehandling
            }

        withKlageApi(mediator, auditlogg = auditlogg) {
            client.put("klage/$klageBehandlingId/trekk") {
                autentisert()
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"årsak": "Klagen er trukket"}""")
            }
        }

        auditlogg.hendelser shouldHaveSize 1
        auditlogg.hendelser.first().let {
            it.operasjon shouldBe AuditOperasjon.UPDATE
            it.melding shouldBe "Avbrutte en klage"
            it.ident shouldBe "12345678901"
        }
    }

    private fun withKlageApi(
        klageMediator: KlageMediator,
        oppslag: Oppslag = oppslagMock,
        auditlogg: Auditlogg = TestAuditlogg(),
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            this.application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    produksjonsstatistikkRepository = mockk(),
                    klageMediator = klageMediator,
                    klageDTOMapper = KlageDTOMapper(oppslag = oppslag),
                    personMediator = mockk(),
                    sakMediator = mockk(),
                    innsendingMediator = mockk(),
                    meldingOmVedtakMediator = mockk(relaxed = true),
                    oppfølgingMediator = mockk(relaxed = true),
                    auditlogg = auditlogg,
                )
            }
            test()
        }
    }
}
