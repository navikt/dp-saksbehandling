package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.autentisert
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.gyldigSaksbehandlerToken
import no.nav.dagpenger.saksbehandling.api.installerApis
import no.nav.dagpenger.saksbehandling.api.mockAzure
import no.nav.dagpenger.saksbehandling.api.models.BeholdningsInfoDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2GruppeDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2SerieDTO
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class StatistikkTestApiTest {
    init {
        mockAzure()
    }

    @Test
    fun `test public statistikk html response`() {
        val mockStatistikkTjeneste =
            mockk<StatistikkTjeneste>().also {
                every { it.hentAntallBrevSendt() } returns 3
            }
        testApplication {
            application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    statistikkTjeneste = mockStatistikkTjeneste,
                    statistikkV2Tjeneste = mockk(),
                    klageMediator = mockk(),
                    klageDTOMapper = mockk(),
                    personMediator = mockk(),
                    sakMediator = mockk(),
                    innsendingMediator = mockk(),
                )
            }

            client.get("public/statistikk").let { httpResponse ->
                httpResponse.status.value shouldBe 200
                httpResponse.bodyAsText() shouldContain """Antall brev sendt: 3"""
            }
        }
    }

    @Test
    fun `test authenticated statistikk api response`() {
        val mockStatistikkTjeneste =
            mockk<StatistikkTjeneste>().also {
                every { it.hentSaksbehandlerStatistikk(any()) } returns StatistikkDTO(1, 2, 3)
                every { it.hentAntallVedtakGjort() } returns StatistikkDTO(4, 5, 6)
                every { it.hentBeholdningsInfo() } returns BeholdningsInfoDTO(2, 3, null)
            }

        testApplication {
            application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    statistikkTjeneste = mockStatistikkTjeneste,
                    statistikkV2Tjeneste = mockk(),
                    klageMediator = mockk(),
                    klageDTOMapper = mockk(),
                    personMediator = mockk(),
                    sakMediator = mockk(),
                    innsendingMediator = mockk(),
                )
            }

            client
                .get("statistikk") {
                    autentisert(token = gyldigSaksbehandlerToken())
                }.let { httpResponse ->
                    httpResponse.status.value shouldBe 200
                    val json = httpResponse.bodyAsText()
                    val jsonElement = Json.parseToJsonElement(json).jsonObject
                    val individuellStatistikk = jsonElement["individuellStatistikk"]!!.jsonObject
                    val generellStatistikk = jsonElement["generellStatistikk"]!!.jsonObject
                    val beholdningsinfo = jsonElement["beholdningsinfo"]!!.jsonObject

                    individuellStatistikk["dag"]!!.jsonPrimitive.int shouldBe 1
                    individuellStatistikk["uke"]!!.jsonPrimitive.int shouldBe 2
                    individuellStatistikk["totalt"]!!.jsonPrimitive.int shouldBe 3

                    generellStatistikk["dag"]!!.jsonPrimitive.int shouldBe 4
                    generellStatistikk["uke"]!!.jsonPrimitive.int shouldBe 5
                    generellStatistikk["totalt"]!!.jsonPrimitive.int shouldBe 6

                    beholdningsinfo["antallOppgaverKlarTilBehandling"]!!.jsonPrimitive.int shouldBe 2
                    beholdningsinfo["antallOppgaverKlarTilKontroll"]!!.jsonPrimitive.int shouldBe 3
                }
        }
    }

    @Test
    fun `test autentisert statistikk v2 apirespons`() {
        val date = LocalDateTime.now().minusDays(1)
        val mockStatistikkV2Tjeneste =
            mockk<StatistikkV2Tjeneste>().also {
                every { it.hentStatuserForUtløstAvFilter(any()) } returns
                    listOf(
                        StatistikkV2GruppeDTO(
                            navn = "test",
                            total = 1,
                            eldsteOppgave = date,
                        ),
                    )
                every { it.hentUtløstAvSerier(any()) } returns
                    listOf(
                        StatistikkV2SerieDTO(
                            navn = "test",
                            verdier = listOf(1),
                        ),
                    )
                every { it.hentStatuserForRettighetstypeFilter(any()) } returns
                    listOf(
                        StatistikkV2GruppeDTO(
                            navn = "test",
                            total = 1,
                            eldsteOppgave = date,
                        ),
                    )
                every { it.hentRettighetstypeSerier(any()) } returns
                    listOf(
                        StatistikkV2SerieDTO(
                            navn = "test",
                            verdier = listOf(1),
                        ),
                    )
            }

        testApplication {
            application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    statistikkTjeneste = mockk(),
                    statistikkV2Tjeneste = mockStatistikkV2Tjeneste,
                    klageMediator = mockk(),
                    klageDTOMapper = mockk(),
                    personMediator = mockk(),
                    sakMediator = mockk(),
                    innsendingMediator = mockk(),
                )
            }

            client
                .get("v2/statistikk") {
                    autentisert(token = gyldigSaksbehandlerToken())
                }.let { httpResponse ->
                    httpResponse.status.value shouldBe 200
                    val json = httpResponse.bodyAsText().trimIndent()

                    // verify through string matching
                    json shouldBe
                        """
                        {
                          "grupper" : [ {
                            "navn" : "test",
                            "total" : 1,
                            "eldsteOppgave" : "$date"
                          } ],
                          "serier" : [ {
                            "navn" : "test",
                            "verdier" : [ 1 ]
                          } ]
                        }
                        """.trimIndent()
                }
        }
    }
}
