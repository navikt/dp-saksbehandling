package no.nav.dagpenger.saksbehandling.api

import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
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
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.autentisert
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.innsending.Aksjon
import no.nav.dagpenger.saksbehandling.innsending.Innsending
import no.nav.dagpenger.saksbehandling.innsending.InnsendingMediator
import org.junit.jupiter.api.Test

class InnsendingApiTest {
    init {
        mockAzure()
    }

    private val innsendingId = UUIDv7.ny()

    @Test
    fun `Skal kaste feil når det mangler autentisering`() {
        val mediator = mockk<InnsendingMediator>()
        withInnsendingApi(mediator) {
            client.get("innsending/$innsendingId").status shouldBe HttpStatusCode.Unauthorized
            client
                .put("innsending/$innsendingId/ferdigstill") {
                    headers[HttpHeaders.ContentType] = "application/json"
                    //language=json
                    setBody("""{ "tullebody": "tull" }""".trimIndent())
                }.let { response ->
                    response.status shouldBe HttpStatusCode.Unauthorized
                }
        }
    }

    @Test
    fun `Skal kunne hente en innsending`() {
        val innsendingResultat = Innsending.InnsendingResultat.RettTilDagpenger(UUIDv7.ny())
        val sak =
            Sak(
                sakId = UUIDv7.ny(),
                søknadId = UUIDv7.ny(),
                opprettet = TestHelper.opprettetNå,
            )
        val innsending =
            TestHelper.lagInnsending(
                vurdering = "hubba",
                innsendingResultat = innsendingResultat,
                valgtSakId = sak.sakId,
            )
        val mediator =
            mockk<InnsendingMediator>().also {
                every { it.hentInnsending(innsendingId, any()) } returns innsending
                every { it.hentLovligeSaker(TestHelper.personIdent) } returns
                    listOf(
                        sak,
                    )
            }
        withInnsendingApi(mediator) {
            client
                .get("innsending/$innsendingId") {
                    autentisert()
                    this.header(HttpHeaders.Accept, "application/json")
                }.bodyAsText() shouldEqualSpecifiedJson
                """
                {
                  "behandlingId": "${innsending.innsendingId}",
                  "journalpostId": "journalpostId",
                  "sakId": "${sak.sakId}",
                  "vurdering": "hubba",
                  "nyBehandling": {
                      "behandlingId": "${innsendingResultat.behandlingId}",
                      "behandlingType": "RETT_TIL_DAGPENGER"
                    },
                  "lovligeSaker": [
                    {
                      "sakId": "${sak.sakId}",
                      "opprettetDato": "${sak.opprettet}"
                    }
                  ]
                }
                """.trimIndent()
        }
    }

    @Test
    fun `Skal kunne ferdigstille en innsending`() {
        val sakId = UUIDv7.ny()
        val slot = slot<FerdigstillInnsendingHendelse>()
        val mediator =
            mockk<InnsendingMediator>().also {
                every { it.ferdigstill(capture(slot)) } returns Unit
            }
        withInnsendingApi(mediator) {
            client
                .put("innsending/$innsendingId/ferdigstill") {
                    autentisert()
                    this.header(HttpHeaders.ContentType, "application/json")
                    //language=json
                    setBody(
                        """
                        {
                            "sakId": "$sakId",
                            "vurdering": "Hubba Bubba",
                            "behandlingType": "RETT_TIL_DAGPENGER"
                        }
                        """.trimIndent(),
                    )
                }.let { response ->
                    response.status shouldBe HttpStatusCode.NoContent
                    slot.captured.let {
                        it.innsendingId shouldBe innsendingId
                        it.valgtSakId() shouldBe sakId
                        it.vurdering shouldBe "Hubba Bubba"
                        it.aksjon.type shouldBe Aksjon.Type.OPPRETT_MANUELL_BEHANDLING
                    }
                }
        }
    }

    private fun withInnsendingApi(
        innsendingMediator: InnsendingMediator,
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
                    innsendingMediator = innsendingMediator,
                )
            }
            test()
        }
    }
}
