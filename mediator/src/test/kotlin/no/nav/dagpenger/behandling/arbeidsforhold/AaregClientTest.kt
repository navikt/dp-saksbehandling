package no.nav.dagpenger.behandling.arbeidsforhold

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AaregClientTest {
    private val testTokenProvider: (token: String, audience: String) -> String = { _, _ -> "testToken" }
    private val baseUrl = "http://baseUrl"
    val subjectToken = "gylidg_token"
    private val aaregScope = "aaregScope"

    @Test
    fun `aareg svarer med 200 og en tom liste av arbeidsforhold`() {
        val mockEngine =
            MockEngine {
                respond(
                    content = ByteReadChannel("[]"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }

        val aaregClient =
            AaregClient(
                baseUrl = baseUrl,
                aaregScope = aaregScope,
                tokenProvider = testTokenProvider,
                engine = mockEngine,
            )

        val arbeidsforhold = aaregClient.hentArbeidsforhold(fnr = "12345678903", subjectToken = subjectToken)
        assertTrue { arbeidsforhold.isEmpty() }
        assertEquals(arbeidsforhold.toString(), "[]")
    }

    @Test
    fun `aareg svarer med 200 og liste med to arbeidsforhold`() {
        val mockEngine =
            MockEngine {
                respond(
                    content = ByteReadChannel(mockArbeidsforhold()),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }

        val aaregClient =
            AaregClient(
                baseUrl = baseUrl,
                aaregScope = aaregScope,
                tokenProvider = testTokenProvider,
                engine = mockEngine,
            )

        val arbeidsforhold = aaregClient.hentArbeidsforhold(fnr = "12345678903", subjectToken = subjectToken)
        assertNotEquals(arbeidsforhold.toString(), "[]")
        assertEquals(arbeidsforhold.size, 2)

        assertEquals("V911050676R16054L0001", arbeidsforhold[0].id)
        assertEquals("910825518", arbeidsforhold[0].orgnr)
        assertEquals("Fast ansettelse", arbeidsforhold[0].ansettelsestype)
        assertEquals("2014-01-01", arbeidsforhold[0].startdato.toString())
        assertEquals("KONTORLEDER", arbeidsforhold[0].yrke)
        assertEquals("2015-01-01", arbeidsforhold[0].sluttdato.toString())
        assertEquals("Arbeidsgiver har sagt opp arbeidstaker", arbeidsforhold[0].sluttaarsak)

        assertEquals("b2561ea4-4b40-4633-8370-51152b3bc22e", arbeidsforhold[1].id)
        assertEquals("972674818", arbeidsforhold[1].orgnr)
        assertEquals("Midlertidig ansettelse", arbeidsforhold[1].ansettelsestype)
        assertEquals("2020-01-01", arbeidsforhold[1].startdato.toString())
        assertEquals("ABONNEMENTSJEF", arbeidsforhold[1].yrke)
        assertNull(arbeidsforhold[1].sluttdato)
        assertNull(arbeidsforhold[1].sluttaarsak)
    }

    fun mockArbeidsforhold(): String {
        //language=JSON
        return """
            [
              {
                "id": "V911050676R16054L0001",
                "type": {
                  "kode": "ordinaertArbeidsforhold",
                  "beskrivelse": "Ordinært arbeidsforhold"
                },
                "arbeidstaker": {
                  "identer": [
                    {
                      "type": "AKTORID",
                      "ident": "2175141353812",
                      "gjeldende": true
                    },
                    {
                      "type": "FOLKEREGISTERIDENT",
                      "ident": "30063000562",
                      "gjeldende": true
                    }
                  ]
                },
                "arbeidssted": {
                  "type": "Underenhet",
                  "identer": [
                    {
                      "type": "ORGANISASJONSNUMMER",
                      "ident": "910825518"
                    }
                  ]
                },
                "opplysningspliktig": {
                  "type": "Hovedenhet",
                  "identer": [
                    {
                      "type": "ORGANISASJONSNUMMER",
                      "ident": "810825472"
                    }
                  ]
                },
                "ansettelsesperiode": {
                  "startdato": "2014-01-01",
                  "sluttdato": "2015-01-01", 
                  "sluttaarsak": {
                    "kode": "Oppsagt",
                    "beskrivelse": "Arbeidsgiver har sagt opp arbeidstaker"
                  }
                },
                "ansettelsesdetaljer": [
                  {
                    "type": "Ordinaer",
                    "arbeidstidsordning": {
                      "kode": "ikkeSkift",
                      "beskrivelse": "Ikke skift"
                    },
                    "ansettelsesform": {
                      "kode": "fast", 
                      "beskrivelse": "Fast ansettelse"
                    },
                    "yrke": {
                      "kode": "1231119",
                      "beskrivelse": "KONTORLEDER"
                    },
                    "antallTimerPrUke": 37.5,
                    "avtaltStillingsprosent": 100,
                    "sisteStillingsprosentendring": "2014-01-01",
                    "sisteLoennsendring": "2014-01-01",
                    "rapporteringsmaaneder": {
                      "fra": "2019-11",
                      "til": null
                    }
                  },
                  {
                    "type": "Ordinaer",
                    "arbeidstidsordning": {
                      "kode": "ikkeSkift",
                      "beskrivelse": "Ikke skift"
                    },
                    "ansettelsesform": {
                      "kode": "fast",
                      "beskrivelse": "Fast ansettelse"
                    },
                    "yrke": {
                      "kode": "1231119",
                      "beskrivelse": "KONTORLEDER"
                    },
                    "antallTimerPrUke": 37.5,
                    "avtaltStillingsprosent": 100,
                    "sisteStillingsprosentendring": "2016-01-01",
                    "sisteLoennsendring": "2016-01-01",
                    "rapporteringsmaaneder": {
                      "fra": "2016-01",
                      "til": "2019-10"
                    }
                  }
                ],
                "permisjoner": [
                  {
                    "id": "68796",
                    "type": {
                      "kode": "permisjonMedForeldrepenger",
                      "beskrivelse": "Permisjon med foreldrepenger"
                    },
                    "startdato": "2021-01-29",
                    "prosent": 50
                  }
                ],
                "permitteringer": [
                  {
                    "id": "54232",
                    "type": {
                      "kode": "permittering",
                      "beskrivelse": "Permittering"
                    },
                    "startdato": "2020-10-30",
                    "prosent": 50
                  }
                ],
                "innrapportertEtterAOrdningen": true,
                "rapporteringsordning": {
                  "kode": "a-ordningen",
                  "beskrivelse": "Rapportert via a-ordningen (2015-d.d.)"
                },
                "navArbeidsforholdId": 12345,
                "navVersjon": 5,
                "navUuid": "28199f29-29e3-42fc-8784-049772bc72fe",
                "opprettet": "2020-05-28T08:52:01.793",
                "sistBekreftet": "2020-09-15T08:19:53",
                "sistEndret": "2020-07-03T14:13:00",
                "bruksperiode": {
                  "fom": "2020-07-03T14:06:00.286",
                  "tom": null
                }
              },
              {
                "id": "b2561ea4-4b40-4633-8370-51152b3bc22e",
                "type": {
                  "kode": "ordinaertArbeidsforhold",
                  "beskrivelse": "Ordinært arbeidsforhold"
                },
                "arbeidstaker": {
                  "identer": [
                    {
                      "type": "AKTORID",
                      "ident": "2175141353812",
                      "gjeldende": true
                    },
                    {
                      "type": "FOLKEREGISTERIDENT",
                      "ident": "30063000562",
                      "gjeldende": true
                    }
                  ]
                },
                "arbeidssted": {
                  "type": "Underenhet",
                  "identer": [
                    {
                      "type": "ORGANISASJONSNUMMER",
                      "ident": "972674818"
                    }
                  ]
                },
                "opplysningspliktig": {
                  "type": "Hovedenhet",
                  "identer": [
                    {
                      "type": "ORGANISASJONSNUMMER",
                      "ident": "928497704"
                    }
                  ]
                },
                "ansettelsesperiode": {
                  "startdato": "2020-01-01"
                },
                "ansettelsesdetaljer": [
                  {
                    "type": "Ordinaer",
                    "arbeidstidsordning": {
                      "kode": "ikkeSkift",
                      "beskrivelse": "Ikke skift"
                    },
                    "ansettelsesform": {
                      "kode": "midlertidig",
                      "beskrivelse": "Midlertidig ansettelse"
                    },
                    "yrke": {
                      "kode": "1233101",
                      "beskrivelse": "ABONNEMENTSJEF"
                    },
                    "antallTimerPrUke": 40,
                    "avtaltStillingsprosent": 100,
                    "sisteStillingsprosentendring": "2020-01-01",
                    "sisteLoennsendring": "2020-01-01",
                    "rapporteringsmaaneder": {
                      "fra": "2020-01",
                      "til": null
                    }
                  }
                ],
                "idHistorikk": [
                  {
                    "id": "2b84b5cb-2eeb-4c50-8f40-51145b7cf852",
                    "bruksperiode": {
                      "fom": "2020-09-09T17:31:45.481",
                      "tom": null
                    }
                  }
                ],
                "varsler": [
                  {
                    "entitet": "Arbeidsforhold",
                    "varslingskode": {
                      "kode": "AFIDHI",
                      "beskrivelse": "Arbeidsforholdet har id-historikk"
                    }
                  }
                ],
                "innrapportertEtterAOrdningen": true,
                "rapporteringsordning": {
                  "kode": "a-ordningen",
                  "beskrivelse": "Rapportert via a-ordningen (2015-d.d.)"
                },
                "navArbeidsforholdId": 45678,
                "navVersjon": 6,
                "navUuid": "6c9cb6aa-528a-4f83-ab10-c511fe1fb2fd",
                "opprettet": "2020-06-16T20:50:07.81",
                "sistBekreftet": "2020-07-28T09:10:19",
                "sistEndret": "2020-09-09T17:32:15",
                "bruksperiode": {
                  "fom": "2020-07-03T14:11:07.021",
                  "tom": null
                }
              }
              
            ]
            """.trimIndent()
    }
}
