package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.models.KlageDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDTO
import org.junit.jupiter.api.Test
import java.util.UUID

class KlageRouteTest {
    init {
        mockAzure()
    }

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
            object : KlageMediator {
                override fun hentKlage(klageId: UUID): KlageDTO {
                    return klageDTO
                }
            }

        testApplication {
            this.application {
                installerApis(mockk(), mockk(), mockk())
            }
            routing {
                klageApi(mediator)
            }
            client.get("/klage/$klageId") {
                headers[HttpHeaders.ContentType] = "application/json"
            }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldEqualJson
                    // language=json
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
}
