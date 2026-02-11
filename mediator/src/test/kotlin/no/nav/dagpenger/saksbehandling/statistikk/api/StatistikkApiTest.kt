package no.nav.dagpenger.saksbehandling.statistikk.api

import io.kotest.assertions.json.shouldEqualJson
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
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.api.MockAzure
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.autentisert
import no.nav.dagpenger.saksbehandling.api.installerApis
import no.nav.dagpenger.saksbehandling.api.mockAzure
import no.nav.dagpenger.saksbehandling.api.models.BeholdningsInfoDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkGruppeDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkSerieDTO
import no.nav.dagpenger.saksbehandling.statistikk.db.AntallOppgaverForTilstandOgRettighet
import no.nav.dagpenger.saksbehandling.statistikk.db.AntallOppgaverForTilstandOgUtløstAv
import no.nav.dagpenger.saksbehandling.statistikk.db.ProduksjonsstatistikkRepository
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class StatistikkApiTest {
    init {
        mockAzure()
    }

    @Test
    fun `test public statistikk html response`() {
        val mockProduksjonsstatistikkRepository =
            mockk<ProduksjonsstatistikkRepository>().also {
                every { it.hentAntallBrevSendt() } returns 3
            }
        testApplication {
            application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    saksbehandlingsstatistikkRepository = mockk(),
                    produksjonsstatistikkRepository = mockProduksjonsstatistikkRepository,
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
        val mockProduksjonsstatistikkRepository =
            mockk<ProduksjonsstatistikkRepository>().also {
                every { it.hentSaksbehandlerStatistikk(any()) } returns StatistikkDTO(1, 2, 3)
                every { it.hentAntallVedtakGjort() } returns StatistikkDTO(4, 5, 6)
                every { it.hentBeholdningsInfo() } returns BeholdningsInfoDTO(2, 3, null)
            }

        testApplication {
            application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    saksbehandlingsstatistikkRepository = mockk(),
                    produksjonsstatistikkRepository = mockProduksjonsstatistikkRepository,
                    klageMediator = mockk(),
                    klageDTOMapper = mockk(),
                    personMediator = mockk(),
                    sakMediator = mockk(),
                    innsendingMediator = mockk(),
                )
            }

            client
                .get("statistikk") {
                    autentisert(token = MockAzure.Companion.gyldigSaksbehandlerToken())
                }.let { httpResponse ->
                    httpResponse.status.value shouldBe 200
                    val json = httpResponse.bodyAsText()
                    val jsonElement = Json.Default.parseToJsonElement(json).jsonObject
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
    fun `API skal svare med status 400 og http problem ved ugyldig tilstand i queryparam`() {
        testApplication {
            application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    saksbehandlingsstatistikkRepository = mockk(),
                    produksjonsstatistikkRepository = mockk(),
                    klageMediator = mockk(),
                    klageDTOMapper = mockk(),
                    personMediator = mockk(),
                    sakMediator = mockk(),
                    innsendingMediator = mockk(),
                )
            }

            client
                .get("produksjonsstatistikk?tilstand=FEIL") {
                    autentisert(token = MockAzure.Companion.gyldigSaksbehandlerToken())
                }.let { httpResponse ->
                    httpResponse.status.value shouldBe 400
                    val json = httpResponse.bodyAsText()
                    json shouldEqualJson
                        //language=json
                        """
                        {
                          "type" : "dagpenger.nav.no/saksbehandling:problem:ugyldig-verdi",
                          "title" : "Ugyldig verdi",
                          "status" : 400,
                          "detail" : "No enum constant no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FEIL",
                          "instance" : "/produksjonsstatistikk"
                        }
                        """.trimIndent()
                }
        }
    }

    @Test
    fun `API skal svare med status 400 og http problem ved ugyldig utlostAv i queryparam`() {
        testApplication {
            application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    saksbehandlingsstatistikkRepository = mockk(),
                    produksjonsstatistikkRepository = mockk(),
                    klageMediator = mockk(),
                    klageDTOMapper = mockk(),
                    personMediator = mockk(),
                    sakMediator = mockk(),
                    innsendingMediator = mockk(),
                )
            }

            client
                .get("produksjonsstatistikk?utlostAv=FEIL") {
                    autentisert(token = MockAzure.Companion.gyldigSaksbehandlerToken())
                }.let { httpResponse ->
                    httpResponse.status.value shouldBe 400
                    val json = httpResponse.bodyAsText()
                    json shouldEqualJson
                        //language=json
                        """
                        {
                          "type" : "dagpenger.nav.no/saksbehandling:problem:ugyldig-verdi",
                          "title" : "Ugyldig verdi",
                          "status" : 400,
                          "detail" : "No enum constant no.nav.dagpenger.saksbehandling.UtløstAvType.FEIL",
                          "instance" : "/produksjonsstatistikk"
                        }
                        """.trimIndent()
                }
        }
    }

    @Test
    fun `Hent produksjonsstatistikk gruppert på utlostAv når bruker er autentisert og ingen gruppering er valgt`() {
        val iGår = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS)
        val mockProduksjonsstatistikkRepository =
            mockk<ProduksjonsstatistikkRepository>().also {
                every { it.hentTilstanderMedUtløstAvFilter(any()) } returns
                    listOf(
                        StatistikkGruppeDTO(
                            navn = "KLAR_TIL_BEHANDLING",
                            total = 1,
                            eldsteOppgave = iGår,
                        ),
                    )
                every { it.hentUtløstAvMedTilstandFilter(any()) } returns
                    listOf(
                        StatistikkSerieDTO(
                            navn = "SØKNAD",
                            total = 1,
                        ),
                    )
                every { it.hentResultatSerierForUtløstAv(any()) } returns
                    listOf(
                        AntallOppgaverForTilstandOgUtløstAv(
                            tilstand = KLAR_TIL_BEHANDLING,
                            utløstAv = UtløstAvType.SØKNAD,
                            antall = 1,
                        ),
                    )
            }

        testApplication {
            application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    saksbehandlingsstatistikkRepository = mockk(),
                    produksjonsstatistikkRepository = mockProduksjonsstatistikkRepository,
                    klageMediator = mockk(),
                    klageDTOMapper = mockk(),
                    personMediator = mockk(),
                    sakMediator = mockk(),
                    innsendingMediator = mockk(),
                )
            }

            client
                .get("produksjonsstatistikk?tilstand=KLAR_TIL_BEHANDLING") {
                    autentisert(token = MockAzure.Companion.gyldigSaksbehandlerToken())
                }.let { httpResponse ->
                    httpResponse.status.value shouldBe 200
                    val json = httpResponse.bodyAsText().trimIndent()

                    // verify through string matching
                    json shouldEqualJson
                        //language=json
                        """
                        {
                          "grupper" : [ {
                            "navn" : "KLAR_TIL_BEHANDLING",
                            "total" : 1,
                            "eldsteOppgave" : "$iGår"
                          } ],
                          "serier" : [ {
                            "navn" : "SØKNAD",
                            "total" : 1
                          } ],
                          "resultat" : {
                            "grupper" : [ 
                              {
                                "navn" : "Klar til behandling"
                              }
                            ],
                            "serier" : [ 
                              {
                                "navn" : "Søknad",
                                "verdier" : [ 
                                  {
                                    "gruppe" : "Klar til behandling",
                                    "antall" : 1
                                  } 
                                ]
                              } 
                            ]
                          }
                        }
                        """.trimIndent()
                }
        }
    }

    @Test
    fun `Hent produksjonsstatistikk gruppert på rettighet når bruker er autentisert og rettighetgruppering er valgt`() {
        val iDag = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        val iGår = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS)
        val mockProduksjonsstatistikkRepository =
            mockk<ProduksjonsstatistikkRepository>().also {
                every { it.hentTilstanderMedRettighetFilter(any()) } returns
                    listOf(
                        StatistikkGruppeDTO(
                            navn = "KLAR_TIL_BEHANDLING",
                            total = 8,
                            eldsteOppgave = iDag,
                        ),
                        StatistikkGruppeDTO(
                            navn = "UNDER_BEHANDLING",
                            total = 14,
                            eldsteOppgave = iGår,
                        ),
                    )
                every { it.hentRettigheterMedTilstandFilter(any()) } returns
                    listOf(
                        StatistikkSerieDTO(
                            navn = "Ordinær",
                            total = 20,
                        ),
                        StatistikkSerieDTO(
                            navn = "Verneplikt",
                            total = 2,
                        ),
                    )
                every { it.hentResultatSerierForRettigheter(any()) } returns
                    listOf(
                        AntallOppgaverForTilstandOgRettighet(
                            tilstand = KLAR_TIL_BEHANDLING,
                            rettighet = "Ordinær",
                            antall = 6,
                        ),
                        AntallOppgaverForTilstandOgRettighet(
                            tilstand = KLAR_TIL_BEHANDLING,
                            rettighet = "Verneplikt",
                            antall = 2,
                        ),
                        AntallOppgaverForTilstandOgRettighet(
                            tilstand = UNDER_BEHANDLING,
                            rettighet = "Ordinær",
                            antall = 14,
                        ),
                        AntallOppgaverForTilstandOgRettighet(
                            tilstand = UNDER_BEHANDLING,
                            rettighet = "Verneplikt",
                            antall = 0,
                        ),
                    )
            }

        testApplication {
            application {
                installerApis(
                    oppgaveMediator = mockk(),
                    oppgaveDTOMapper = mockk(),
                    saksbehandlingsstatistikkRepository = mockk(),
                    produksjonsstatistikkRepository = mockProduksjonsstatistikkRepository,
                    klageMediator = mockk(),
                    klageDTOMapper = mockk(),
                    personMediator = mockk(),
                    sakMediator = mockk(),
                    innsendingMediator = mockk(),
                )
            }

            client
                .get("produksjonsstatistikk?tilstand=KLAR_TIL_BEHANDLING&tilstand=UNDER_BEHANDLING&grupperEtter=RETTIGHETSTYPE") {
                    autentisert(token = MockAzure.Companion.gyldigSaksbehandlerToken())
                }.let { httpResponse ->
                    httpResponse.status.value shouldBe 200
                    val json = httpResponse.bodyAsText().trimIndent()

                    // verify through string matching
                    json shouldEqualJson
                        //language=json
                        """
                        {
                          "grupper" : [ {
                            "navn" : "KLAR_TIL_BEHANDLING",
                            "total" : 8,
                            "eldsteOppgave" : "$iDag"
                          },{
                            "navn" : "UNDER_BEHANDLING",
                            "total" : 14,
                            "eldsteOppgave" : "$iGår"
                          } ],
                          "serier" : [ {
                            "navn" : "Ordinær",
                            "total" : 20
                          }, {
                            "navn" : "Verneplikt",
                            "total" : 2
                          } ],
                          "resultat" : {
                            "grupper" : [ 
                              {
                                "navn" : "Klar til behandling"
                              }, {
                                "navn" : "Under behandling"
                              }
                            ],
                            "serier" : [ 
                              {
                                "navn" : "Ordinær",
                                "verdier" : [ 
                                  {
                                    "gruppe" : "Klar til behandling",
                                    "antall" : 6
                                  } ,
                                  {
                                    "gruppe" : "Under behandling",
                                    "antall" : 14
                                  } 
                                ]
                              }, 
                              {
                                "navn" : "Verneplikt",
                                "verdier" : [ 
                                  {
                                    "gruppe" : "Klar til behandling",
                                    "antall" : 2
                                  } ,
                                  {
                                    "gruppe" : "Under behandling",
                                    "antall" : 0
                                  } 
                                ]
                              } 
                            ]
                          }
                        }
                        """.trimIndent()
                }
        }
    }
}
