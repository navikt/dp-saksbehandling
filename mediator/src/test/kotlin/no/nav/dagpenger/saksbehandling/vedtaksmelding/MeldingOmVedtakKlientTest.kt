package no.nav.dagpenger.saksbehandling.vedtaksmelding

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Sikkerhetstiltak
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerEnhetDTO
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient.Companion.lagMeldingOmVedtakKlient
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient.KanIkkeLageMeldingOmVedtak
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class MeldingOmVedtakKlientTest {
    val saksbehandlerToken = "saksbehandlerToken"
    private val tokenProvider = { _: String -> saksbehandlerToken }
    private val ident = "testIdent"
    private val behandlingIdSuksess = UUID.fromString("0194b1e0-e59d-71b0-ad40-54bd49c894af")

    private var requestData: HttpRequestData? = null
    private val expectedHtmlResponse = "<html><h1>Hei</h1></html>"

    private val meldingOmVedtakKlient =
        MeldingOmVedtakKlient(
            dpMeldingOmVedtakUrl = "http://localhost",
            tokenProvider = tokenProvider,
            httpClient =
                lagMeldingOmVedtakKlient(
                    engine =
                        MockEngine { request: HttpRequestData ->
                            requestData = request
                            when (request.url.encodedPath.contains(behandlingIdSuksess.toString())) {
                                true ->
                                    respond(
                                        content = expectedHtmlResponse,
                                        status = HttpStatusCode.OK,
                                    )

                                else ->
                                    respond(
                                        content = "Error",
                                        status = HttpStatusCode.InternalServerError,
                                    )
                            }
                        },
                    registry = PrometheusRegistry(),
                ),
        )
    private val person =
        PDLPersonIntern(
            ident = ident,
            fornavn = "Test",
            etternavn = "Person",
            mellomnavn = null,
            fødselsdato = LocalDate.of(2000, 1, 1),
            alder = 21,
            statsborgerskap = "NOR",
            kjønn = PDLPerson.Kjonn.MANN,
            adresseBeskyttelseGradering = UGRADERT,
            sikkerhetstiltak =
                listOf(
                    Sikkerhetstiltak(
                        type = "Tiltakstype",
                        beskrivelse = "To ansatte i samtale",
                        gyldigFom = LocalDate.now(),
                        gyldigTom = LocalDate.now().plusDays(1),
                    ),
                ),
        )
    private val saksbehandler =
        BehandlerDTO(
            ident = "saksbehandlerIdent",
            fornavn = "Saks",
            etternavn = "Behandler",
            enhet =
                BehandlerEnhetDTO(
                    navn = "Enhet",
                    enhetNr = "1234",
                    postadresse = "Postadresse",
                ),
        )

    @Test
    fun `kall mot dp-melding-om-vedtak ved feil `() {
        runBlocking {
            val behandlingIdSomFeiler = UUIDv7.ny()
            shouldThrow<KanIkkeLageMeldingOmVedtak> {
                meldingOmVedtakKlient.lagOgHentMeldingOmVedtak(
                    person = person,
                    saksbehandler = saksbehandler,
                    beslutter = saksbehandler,
                    behandlingId = behandlingIdSomFeiler,
                    saksbehandlerToken = saksbehandlerToken,
                )
            }
        }
    }

    @Test
    fun `kall mot dp-melding-om-vedtak happy path `() {
        runBlocking {
            val result =
                meldingOmVedtakKlient.lagOgHentMeldingOmVedtak(
                    person = person,
                    saksbehandler = saksbehandler,
                    beslutter = saksbehandler,
                    behandlingId = behandlingIdSuksess,
                    saksbehandlerToken = saksbehandlerToken,
                )
            result.getOrThrow() shouldBe expectedHtmlResponse
            requireNotNull(requestData).let {
                it.headers["Authorization"] shouldBe "Bearer $saksbehandlerToken"
                it.body.contentType.toString() shouldBe "application/json"
                it.body.toByteArray().decodeToString() shouldEqualJson
                    """
                                       {
                      "fornavn": "Test",
                      "etternavn": "Person",
                      "fodselsnummer": "testIdent",
                      "saksbehandler": {
                        "ident": "saksbehandlerIdent",
                        "fornavn": "Saks",
                        "etternavn": "Behandler",
                        "enhet": {
                          "navn": "Enhet",
                          "enhetNr": "1234",
                          "postadresse": "Postadresse"
                        }
                      },
                      "beslutter": {
                        "ident": "saksbehandlerIdent",
                        "fornavn": "Saks",
                        "etternavn": "Behandler",
                        "enhet": {
                          "navn": "Enhet",
                          "enhetNr": "1234",
                          "postadresse": "Postadresse"
                        }
                      }
                    } 
                    """.trimIndent()
            }
        }
    }
}
