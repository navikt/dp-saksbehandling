package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.MeldingOmVedtakMediator
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.autentisert
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.gyldigSaksbehandlerToken
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient.KanIkkeLageMeldingOmVedtak
import org.junit.jupiter.api.Test

class MeldingOmVedtakApiTest {
    init {
        mockAzure()
    }

    private val oppgaveId = UUIDv7.ny()

    @Test
    fun `Skal kaste feil når det mangler autentisering`() {
        val mediator = mockk<MeldingOmVedtakMediator>()
        withMeldingOmVedtakApi(mediator) {
            client.get("oppgave/$oppgaveId/melding-om-vedtak/html").status shouldBe HttpStatusCode.Unauthorized
            client
                .put("oppgave/$oppgaveId/melding-om-vedtak/utvidet-beskrivelse/brevblokk-123") {
                    headers[HttpHeaders.ContentType] = "application/json"
                    //language=json
                    setBody("""{ "tekst": "en tekst" }""")
                }.status shouldBe HttpStatusCode.Unauthorized
            client
                .put("oppgave/$oppgaveId/melding-om-vedtak/brev-variant") {
                    headers[HttpHeaders.ContentType] = "application/json"
                    //language=json
                    setBody("""{ "brevVariant": "GENERERT" }""")
                }.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Skal hente melding om vedtak HTML`() {
        val saksbehandlerToken = gyldigSaksbehandlerToken()
        val mediator =
            mockk<MeldingOmVedtakMediator>().also {
                coEvery {
                    it.hentMeldingOmVedtakHtml(
                        oppgaveId = oppgaveId,
                        saksbehandler = TestHelper.saksbehandler,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } returns "<html><h1>Vedtak</h1></html>"
            }

        withMeldingOmVedtakApi(mediator) {
            client
                .get("oppgave/$oppgaveId/melding-om-vedtak/html") {
                    autentisert(token = saksbehandlerToken)
                }.let { response ->
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldBe "<html><h1>Vedtak</h1></html>"
                }
        }

        coVerify(exactly = 1) {
            mediator.hentMeldingOmVedtakHtml(
                oppgaveId = oppgaveId,
                saksbehandler = TestHelper.saksbehandler,
                saksbehandlerToken = saksbehandlerToken,
            )
        }
    }

    @Test
    fun `Skal returnere 500 når henting av HTML feiler`() {
        val mediator =
            mockk<MeldingOmVedtakMediator>().also {
                coEvery {
                    it.hentMeldingOmVedtakHtml(
                        oppgaveId = oppgaveId,
                        saksbehandler = any(),
                        saksbehandlerToken = any(),
                    )
                } throws KanIkkeLageMeldingOmVedtak("Feil ved henting av HTML")
            }

        withMeldingOmVedtakApi(mediator) {
            client
                .get("oppgave/$oppgaveId/melding-om-vedtak/html") { autentisert() }
                .status shouldBe HttpStatusCode.InternalServerError
        }
    }

    @Test
    fun `Skal lagre utvidet beskrivelse`() {
        val brevblokkId = "brevblokk-123"
        val saksbehandlerToken = gyldigSaksbehandlerToken()
        val mediator =
            mockk<MeldingOmVedtakMediator>().also {
                coEvery {
                    it.lagreUtvidetBeskrivelse(
                        oppgaveId = oppgaveId,
                        brevblokkId = brevblokkId,
                        tekst = "En utvidet beskrivelse",
                        saksbehandler = TestHelper.saksbehandler,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } returns """{"sistEndretTidspunkt": "2025-01-01T12:00:00"}"""
            }

        withMeldingOmVedtakApi(mediator) {
            client
                .put("oppgave/$oppgaveId/melding-om-vedtak/utvidet-beskrivelse/$brevblokkId") {
                    autentisert(token = saksbehandlerToken)
                    headers[HttpHeaders.ContentType] = "application/json"
                    //language=json
                    setBody("""{ "tekst": "En utvidet beskrivelse" }""")
                }.let { response ->
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldBe """{"sistEndretTidspunkt": "2025-01-01T12:00:00"}"""
                }
        }

        coVerify(exactly = 1) {
            mediator.lagreUtvidetBeskrivelse(
                oppgaveId = oppgaveId,
                brevblokkId = brevblokkId,
                tekst = "En utvidet beskrivelse",
                saksbehandler = TestHelper.saksbehandler,
                saksbehandlerToken = saksbehandlerToken,
            )
        }
    }

    @Test
    fun `Skal returnere 500 når lagring av utvidet beskrivelse feiler`() {
        val mediator =
            mockk<MeldingOmVedtakMediator>().also {
                coEvery {
                    it.lagreUtvidetBeskrivelse(
                        oppgaveId = oppgaveId,
                        brevblokkId = any(),
                        tekst = any(),
                        saksbehandler = any(),
                        saksbehandlerToken = any(),
                    )
                } throws KanIkkeLageMeldingOmVedtak("Feil ved lagring")
            }

        withMeldingOmVedtakApi(mediator) {
            client
                .put("oppgave/$oppgaveId/melding-om-vedtak/utvidet-beskrivelse/brevblokk-123") {
                    autentisert()
                    headers[HttpHeaders.ContentType] = "application/json"
                    //language=json
                    setBody("""{ "tekst": "En tekst" }""")
                }.status shouldBe HttpStatusCode.InternalServerError
        }
    }

    @Test
    fun `Skal lagre brevvariant`() {
        val saksbehandlerToken = gyldigSaksbehandlerToken()
        val mediator =
            mockk<MeldingOmVedtakMediator>().also {
                coEvery {
                    it.lagreBrevVariant(
                        oppgaveId = oppgaveId,
                        brevVariant = "GENERERT",
                        saksbehandler = TestHelper.saksbehandler,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } returns Unit
            }

        withMeldingOmVedtakApi(mediator) {
            client
                .put("oppgave/$oppgaveId/melding-om-vedtak/brev-variant") {
                    autentisert(token = saksbehandlerToken)
                    headers[HttpHeaders.ContentType] = "application/json"
                    //language=json
                    setBody("""{ "brevVariant": "GENERERT" }""")
                }.let { response ->
                    response.status shouldBe HttpStatusCode.NoContent
                }
        }

        coVerify(exactly = 1) {
            mediator.lagreBrevVariant(
                oppgaveId = oppgaveId,
                brevVariant = "GENERERT",
                saksbehandler = TestHelper.saksbehandler,
                saksbehandlerToken = saksbehandlerToken,
            )
        }
    }

    @Test
    fun `Skal returnere 500 når lagring av brevvariant feiler`() {
        val mediator =
            mockk<MeldingOmVedtakMediator>().also {
                coEvery {
                    it.lagreBrevVariant(
                        oppgaveId = oppgaveId,
                        brevVariant = any(),
                        saksbehandler = any(),
                        saksbehandlerToken = any(),
                    )
                } throws KanIkkeLageMeldingOmVedtak("Feil ved lagring av brevvariant")
            }

        withMeldingOmVedtakApi(mediator) {
            client
                .put("oppgave/$oppgaveId/melding-om-vedtak/brev-variant") {
                    autentisert()
                    headers[HttpHeaders.ContentType] = "application/json"
                    //language=json
                    setBody("""{ "brevVariant": "EGENDEFINERT" }""")
                }.status shouldBe HttpStatusCode.InternalServerError
        }
    }

    private fun withMeldingOmVedtakApi(
        meldingOmVedtakMediator: MeldingOmVedtakMediator,
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
                    innsendingMediator = mockk(),
                    meldingOmVedtakMediator = meldingOmVedtakMediator,
                )
            }
            test()
        }
    }
}
