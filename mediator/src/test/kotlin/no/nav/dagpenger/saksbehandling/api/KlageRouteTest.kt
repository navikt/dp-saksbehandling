package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.OpplysningerVerdi
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.models.KlageDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDTO
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KlageRouteTest {
    init {
        mockAzure()
    }

    private val klageId = UUIDv7.ny()
    private val opplysningId = UUIDv7.ny()

    @Test
    fun `Skal hente klageDTO`() {
        val klageId = UUIDv7.ny()
        val klageDTO =
            KlageDTO(
                id = klageId,
                opplysninger =
                    KlageOpplysningDTO(
                        id = klageId,
                        navn = "Testopplysning",
                        type = KlageOpplysningDTO.Type.TEKST,
                        paakrevd = false,
                        gruppe = KlageOpplysningDTO.Gruppe.KLAGEMinusANKE,
                    ),
                saksbehandler = null,
                utfall = null,
                meldingOmVedtak = null,
            )
        val mediator =
            mockk<KlageMediator>().also {
                every { it.hentKlage(klageId) } returns klageDTO
            }
        withKlageRoute(mediator) {
            client.get("/klage/$klageId").let { response ->
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldEqualJson // language=json
                    """{
                     "Id": "$klageId",
                     "opplysninger": {
                       "id": "$klageId",
                       "navn": "Testopplysning",
                       "type": "TEKST",
                       "paakrevd": false,
                       "gruppe": "KLAGE-ANKE"
                     }
                   }
                    """.trimMargin()
            }
        }
    }

    @Test
    fun `Skal kunne oppdatere en  opplysning av type flervalg `() {
        val tekstListe = OpplysningerVerdi.TekstListe("tekst1", "tekst2")
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.oppdaterKlageOpplysning(klageId, opplysningId, tekstListe)
                } returns Unit
            }
        withKlageRoute(mediator) {
            client.put("/klage/$klageId/opplysning/$opplysningId") {
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "verdi": ["tekst1","tekst2"], "opplysningType":"FLER-LISTEVALG" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
                verify(exactly = 1) {
                    mediator.oppdaterKlageOpplysning(
                        klageId = klageId,
                        opplysningId = opplysningId,
                        verdi = tekstListe,
                    )
                }
            }
        }
    }

    @Test
    fun `Skal kunne oppdatere en opplysning av type tekst `() {
        val tekst = OpplysningerVerdi.Tekst("tekst")
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.oppdaterKlageOpplysning(klageId, opplysningId, tekst)
                } returns Unit
            }
        withKlageRoute(mediator) {
            client.put("/klage/$klageId/opplysning/$opplysningId") {
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "verdi": "tekst", "opplysningType":"TEKST" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
                verify(exactly = 1) {
                    mediator.oppdaterKlageOpplysning(
                        klageId = klageId,
                        opplysningId = opplysningId,
                        verdi = tekst,
                    )
                }
            }
        }
    }

    @Test
    fun `Skal kunne oppdatere en opplysning av type boolean `() {
        val boolsk = OpplysningerVerdi.Boolsk(false)
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.oppdaterKlageOpplysning(klageId, opplysningId, boolsk)
                } returns Unit
            }
        withKlageRoute(mediator) {
            client.put("/klage/$klageId/opplysning/$opplysningId") {
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "verdi": false, "opplysningType":"BOOLSK" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
                verify(exactly = 1) {
                    mediator.oppdaterKlageOpplysning(
                        klageId = klageId,
                        opplysningId = opplysningId,
                        verdi = boolsk,
                    )
                }
            }
        }
    }

    @Test
    fun `Skal kunne oppdatere en opplysning av type dato `() {
        val dato = OpplysningerVerdi.Dato(LocalDate.of(2021, 1, 1))
        val mediator =
            mockk<KlageMediator>().also {
                every {
                    it.oppdaterKlageOpplysning(klageId, opplysningId, dato)
                } returns Unit
            }
        withKlageRoute(mediator) {
            client.put("/klage/$klageId/opplysning/$opplysningId") {
                headers[HttpHeaders.ContentType] = "application/json"
                //language=json
                setBody("""{ "verdi": "2021-01-01", "opplysningType":"DATO" }""".trimIndent())
            }.let { response ->
                response.status shouldBe HttpStatusCode.NoContent
                verify(exactly = 1) {
                    mediator.oppdaterKlageOpplysning(
                        klageId = klageId,
                        opplysningId = opplysningId,
                        verdi = dato,
                    )
                }
            }
        }
    }

    private fun withKlageRoute(
        klageMediator: KlageMediator,
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        testApplication {
            this.application {
                installerApis(mockk(), mockk(), mockk(), klageMediator)
            }
            test()
        }
    }
}
