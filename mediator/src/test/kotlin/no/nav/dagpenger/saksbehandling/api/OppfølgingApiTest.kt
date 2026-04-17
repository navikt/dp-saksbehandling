package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TestHelper.ISO_TIMESTAMP
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.autentisert
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.oppfolging.Oppfølging
import no.nav.dagpenger.saksbehandling.oppfolging.OppfølgingAksjon
import no.nav.dagpenger.saksbehandling.oppfolging.OppfølgingMediator
import no.nav.dagpenger.saksbehandling.oppfolging.OpprettetOppfølging
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.stream.Stream

class OppfølgingApiTest {
    companion object {
        @JvmStatic
        fun behandlingsvarianter(): Stream<Arguments> =
            Stream.of(
                Arguments.of("KLAGE", OppfølgingAksjon.Type.OPPRETT_KLAGE),
                Arguments.of("RETT_TIL_DAGPENGER_MANUELL", OppfølgingAksjon.Type.OPPRETT_MANUELL_BEHANDLING),
                Arguments.of("RETT_TIL_DAGPENGER_REVURDERING", OppfølgingAksjon.Type.OPPRETT_REVURDERING_BEHANDLING),
            )
    }

    init {
        mockAzure()
    }

    private val oppfølgingId = UUIDv7.ny()

    @Test
    fun `Skal kaste feil når det mangler autentisering`() {
        val mediator = mockk<OppfølgingMediator>()
        withOppfølgingApi(mediator) {
            client.get("oppfolging/$oppfølgingId").status shouldBe HttpStatusCode.Unauthorized
            client
                .put("oppfolging/$oppfølgingId/ferdigstill") {
                    headers[HttpHeaders.ContentType] = "application/json"
                    //language=json
                    setBody("""{ "vurdering": "test" }""".trimIndent())
                }.let { response ->
                    response.status shouldBe HttpStatusCode.Unauthorized
                }
        }
    }

    @Test
    fun `POST oppretter generell oppgave og returnerer IDs`() {
        val slot = slot<OpprettOppfølgingHendelse>()
        val opprettetId = UUIDv7.ny()
        val oppgaveId = UUIDv7.ny()
        val mediator =
            mockk<OppfølgingMediator>().also {
                every { it.taImot(capture(slot)) } returns
                    OpprettetOppfølging(
                        oppfølgingId = opprettetId,
                        oppgaveId = oppgaveId,
                    )
            }
        withOppfølgingApi(mediator) {
            client
                .post("oppfolging") {
                    autentisert()
                    this.header(HttpHeaders.ContentType, "application/json")
                    //language=json
                    setBody(
                        """
                        {
                            "personIdent": "12345678901",
                            "tittel": "Sjekk sykemelding",
                            "beskrivelse": "Kontroller datoer",
                            "aarsak": "Sykemelding",
                            "strukturertData": { "foo": "bar" }
                        }
                        """.trimIndent(),
                    )
                }.let { response ->
                    response.status shouldBe HttpStatusCode.Created
                    response.bodyAsText() shouldEqualSpecifiedJson
                        """
                        {
                            "oppfølgingId": "$opprettetId",
                            "oppgaveId": "$oppgaveId"
                        }
                        """.trimIndent()

                    slot.captured.let {
                        it.ident shouldBe "12345678901"
                        it.tittel shouldBe "Sjekk sykemelding"
                        it.beskrivelse shouldBe "Kontroller datoer"
                        it.aarsak shouldBe "Sykemelding"
                        it.strukturertData shouldBe mapOf("foo" to "bar")
                        it.frist shouldBe null
                    }
                }
        }
    }

    @Test
    fun `POST med beholdOppgaven=true sender flagget til mediator`() {
        val slot = slot<OpprettOppfølgingHendelse>()
        val mediator =
            mockk<OppfølgingMediator>().also {
                every { it.taImot(capture(slot)) } returns
                    OpprettetOppfølging(
                        oppfølgingId = UUIDv7.ny(),
                        oppgaveId = UUIDv7.ny(),
                    )
            }
        withOppfølgingApi(mediator) {
            client
                .post("oppfolging") {
                    autentisert()
                    this.header(HttpHeaders.ContentType, "application/json")
                    //language=json
                    setBody(
                        """
                        {
                            "personIdent": "12345678901",
                            "tittel": "Sjekk sykemelding",
                            "aarsak": "Sykemelding",
                            "beholdOppgaven": true
                        }
                        """.trimIndent(),
                    )
                }.let { response ->
                    response.status shouldBe HttpStatusCode.Created
                    slot.captured.beholdOppgaven shouldBe true
                }
        }
    }

    @Test
    fun `POST uten beholdOppgaven defaulter til false`() {
        val slot = slot<OpprettOppfølgingHendelse>()
        val mediator =
            mockk<OppfølgingMediator>().also {
                every { it.taImot(capture(slot)) } returns
                    OpprettetOppfølging(
                        oppfølgingId = UUIDv7.ny(),
                        oppgaveId = UUIDv7.ny(),
                    )
            }
        withOppfølgingApi(mediator) {
            client
                .post("oppfolging") {
                    autentisert()
                    this.header(HttpHeaders.ContentType, "application/json")
                    //language=json
                    setBody(
                        """
                        {
                            "personIdent": "12345678901",
                            "tittel": "Sjekk sykemelding",
                            "aarsak": "Sykemelding"
                        }
                        """.trimIndent(),
                    )
                }.let { response ->
                    response.status shouldBe HttpStatusCode.Created
                    slot.captured.beholdOppgaven shouldBe false
                }
        }
    }

    @Test
    fun `POST med frist sender frist til mediator`() {
        val slot = slot<OpprettOppfølgingHendelse>()
        val mediator =
            mockk<OppfølgingMediator>().also {
                every { it.taImot(capture(slot)) } returns
                    OpprettetOppfølging(
                        oppfølgingId = UUIDv7.ny(),
                        oppgaveId = UUIDv7.ny(),
                    )
            }
        val frist = LocalDate.now().plusDays(7)
        withOppfølgingApi(mediator) {
            client
                .post("oppfolging") {
                    autentisert()
                    this.header(HttpHeaders.ContentType, "application/json")
                    //language=json
                    setBody(
                        """
                        {
                            "personIdent": "12345678901",
                            "tittel": "Tidskritisk oppgave",
                            "aarsak": "Meldekort",
                            "frist": "$frist"
                        }
                        """.trimIndent(),
                    )
                }.let { response ->
                    response.status shouldBe HttpStatusCode.Created
                    slot.captured.let {
                        it.frist shouldBe frist
                        it.utførtAv shouldNotBe null
                    }
                }
        }
    }

    @Test
    fun `Skal kunne hente en generell oppgave`() {
        val resultat = Oppfølging.Resultat.RettTilDagpenger(UUIDv7.ny())
        val sak =
            Sak(
                sakId = UUIDv7.ny(),
                søknadId = UUIDv7.ny(),
                opprettet = TestHelper.opprettetNå,
            )
        val oppfølging =
            TestHelper.lagOppfølging(
                id = oppfølgingId,
                tittel = "Test oppgave",
                beskrivelse = "En beskrivelse",
                strukturertData = mapOf("meldekortId" to "MK-2026-01", "timer" to 40),
                vurdering = "Min vurdering",
                resultat = resultat,
                valgtSakId = sak.sakId,
            )
        val mediator =
            mockk<OppfølgingMediator>().also {
                every { it.hent(oppfølgingId, any()) } returns oppfølging
                every { it.hentLovligeSaker(TestHelper.personIdent) } returns listOf(sak)
            }
        withOppfølgingApi(mediator) {
            client
                .get("oppfolging/$oppfølgingId") {
                    autentisert()
                    this.header(HttpHeaders.Accept, "application/json")
                }.bodyAsText() shouldEqualSpecifiedJson
                """
                {
                  "tittel": "Test oppgave",
                  "beskrivelse": "En beskrivelse",
                  "strukturertData": {
                    "meldekortId": "MK-2026-01",
                    "timer": 40
                  },
                  "sakId": "${sak.sakId}",
                  "vurdering": "Min vurdering",
                  "nyBehandling": {
                      "behandlingId": "${resultat.behandlingId}",
                      "behandlingType": "RETT_TIL_DAGPENGER"
                    },
                  "lovligeSaker": [
                    {
                      "sakId": "${sak.sakId}",
                      "opprettetDato": "${sak.opprettet.format(ISO_TIMESTAMP)}"
                    }
                  ]
                }
                """.trimIndent()
        }
    }

    @Test
    fun `Skal kunne hente generell oppgave med frist`() {
        val frist = LocalDate.of(2026, 5, 15)
        val oppfølging =
            TestHelper.lagOppfølging(
                id = oppfølgingId,
                tittel = "Oppgave med frist",
                frist = frist,
            )
        val mediator =
            mockk<OppfølgingMediator>().also {
                every { it.hent(oppfølgingId, any()) } returns oppfølging
                every { it.hentLovligeSaker(TestHelper.personIdent) } returns emptyList()
            }
        withOppfølgingApi(mediator) {
            client
                .get("oppfolging/$oppfølgingId") {
                    autentisert()
                    this.header(HttpHeaders.Accept, "application/json")
                }.bodyAsText() shouldEqualSpecifiedJson
                """
                {
                  "tittel": "Oppgave med frist",
                  "frist": "2026-05-15",
                  "beskrivelse": "",
                  "strukturertData": {},
                  "lovligeSaker": []
                }
                """.trimIndent()
        }
    }

    @Test
    fun `Skal kunne hente generell oppgave uten resultat`() {
        val oppfølging =
            TestHelper.lagOppfølging(
                id = oppfølgingId,
                tittel = "Enkel oppgave",
                beskrivelse = "",
            )
        val mediator =
            mockk<OppfølgingMediator>().also {
                every { it.hent(oppfølgingId, any()) } returns oppfølging
                every { it.hentLovligeSaker(TestHelper.personIdent) } returns emptyList()
            }
        withOppfølgingApi(mediator) {
            client
                .get("oppfolging/$oppfølgingId") {
                    autentisert()
                    this.header(HttpHeaders.Accept, "application/json")
                }.bodyAsText() shouldEqualSpecifiedJson
                """
                {
                  "tittel": "Enkel oppgave",
                  "beskrivelse": "",
                  "strukturertData": {},
                  "lovligeSaker": []
                }
                """.trimIndent()
        }
    }

    @Test
    fun `Skal kunne ferdigstille en generell oppgave uten aksjon`() {
        val sakId = UUIDv7.ny()
        val slot = slot<FerdigstillOppfølgingHendelse>()
        val mediator =
            mockk<OppfølgingMediator>().also {
                every { it.ferdigstill(capture(slot)) } returns Unit
            }
        withOppfølgingApi(mediator) {
            client
                .put("oppfolging/$oppfølgingId/ferdigstill") {
                    autentisert()
                    this.header(HttpHeaders.ContentType, "application/json")
                    //language=json
                    setBody(
                        """
                        {
                            "sakId": "$sakId",
                            "vurdering": "Ferdig vurdert"
                        }
                        """.trimIndent(),
                    )
                }.let { response ->
                    response.status shouldBe HttpStatusCode.NoContent
                    slot.captured.let {
                        it.oppfølgingId shouldBe oppfølgingId
                        it.aksjon.valgtSakId shouldBe sakId
                        it.vurdering shouldBe "Ferdig vurdert"
                        it.aksjon.type shouldBe OppfølgingAksjon.Type.AVSLUTT
                    }
                }
        }
    }

    @ParameterizedTest
    @MethodSource("behandlingsvarianter")
    fun `Skal kunne ferdigstille med behandlingsvariant`(
        behandlingsvariant: String,
        forventetAksjonsType: OppfølgingAksjon.Type,
    ) {
        val sakId = UUIDv7.ny()
        val slot = slot<FerdigstillOppfølgingHendelse>()
        val mediator =
            mockk<OppfølgingMediator>().also {
                every { it.ferdigstill(capture(slot)) } returns Unit
            }
        withOppfølgingApi(mediator) {
            client
                .put("oppfolging/$oppfølgingId/ferdigstill") {
                    autentisert()
                    this.header(HttpHeaders.ContentType, "application/json")
                    //language=json
                    setBody(
                        """
                        {
                            "sakId": "$sakId",
                            "vurdering": "Ferdig vurdert",
                            "behandlingsvariant": "$behandlingsvariant"
                        }
                        """.trimIndent(),
                    )
                }.let { response ->
                    response.status shouldBe HttpStatusCode.NoContent
                    slot.captured.let {
                        it.aksjon.type shouldBe forventetAksjonsType
                        it.aksjon.valgtSakId shouldBe sakId
                    }
                }
        }
    }

    @Test
    fun `Skal kunne ferdigstille med OPPFOLGING variant og nyOppgave`() {
        val sakId = UUIDv7.ny()
        val frist = LocalDate.now().plusDays(14)
        val slot = slot<FerdigstillOppfølgingHendelse>()
        val mediator =
            mockk<OppfølgingMediator>().also {
                every { it.ferdigstill(capture(slot)) } returns Unit
            }
        withOppfølgingApi(mediator) {
            client
                .put("oppfolging/$oppfølgingId/ferdigstill") {
                    autentisert()
                    this.header(HttpHeaders.ContentType, "application/json")
                    //language=json
                    setBody(
                        """
                        {
                            "sakId": "$sakId",
                            "vurdering": "Opprett ny generell oppgave",
                            "behandlingsvariant": "OPPFOLGING",
                            "nyOppgave": {
                                "tittel": "Følg opp meldekort",
                                "beskrivelse": "Sjekk timer neste periode",
                                "aarsak": "Meldekort",
                                "frist": "$frist",
                                "beholdOppgaven": true
                            }
                        }
                        """.trimIndent(),
                    )
                }.let { response ->
                    response.status shouldBe HttpStatusCode.NoContent
                    slot.captured.let {
                        it.aksjon.type shouldBe OppfølgingAksjon.Type.OPPRETT_OPPFOLGING
                        it.aksjon.valgtSakId shouldBe sakId
                        it.vurdering shouldBe "Opprett ny generell oppgave"
                    }
                    (slot.captured.aksjon as OppfølgingAksjon.OpprettOppfølging).let {
                        it.tittel shouldBe "Følg opp meldekort"
                        it.beskrivelse shouldBe "Sjekk timer neste periode"
                        it.aarsak shouldBe "Meldekort"
                        it.frist shouldBe frist
                        it.beholdOppgaven shouldBe true
                    }
                }
        }
    }

    private fun withOppfølgingApi(
        oppfølgingMediator: OppfølgingMediator,
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            this.application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    produksjonsstatistikkRepository = mockk(),
                    klageMediator = mockk(),
                    klageDTOMapper = mockk(),
                    personMediator = mockk(),
                    sakMediator = mockk(),
                    innsendingMediator = mockk(relaxed = true),
                    meldingOmVedtakMediator = mockk(relaxed = true),
                    oppfølgingMediator = oppfølgingMediator,
                )
            }
            test()
        }
    }
}
