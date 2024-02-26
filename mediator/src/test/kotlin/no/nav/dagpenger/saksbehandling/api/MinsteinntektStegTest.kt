package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import org.junit.jupiter.api.Test

class MinsteinntektStegTest {
    @Test
    fun `Skal hente ut alle opplysninger fra en trestruktur`() {
        val behandlingDto = objectMapper.readValue(testdata, BehandlingDTO::class.java)
        val stegDto = alderskravStegFra(behandlingDto)
        requireNotNull(stegDto)
        stegDto.opplysninger.size shouldBe 10
    }
}

//language=json
private val testdata =
    """
    {
      "behandlingId": "018dc0e6-0be3-7f17-b410-08f2072ffcb1",
      "opplysning": [
        {
          "id": "018dc0e6-0d54-79b8-962a-360720ab2614",
          "opplysningstype": "Oppfyller kravet til alder",
          "verdi": "true",
          "status": "Hypotese",
          "gyldigFraOgMed": "-9999-01-01T00:00:00+02:00",
          "gyldigTilOgMed": "+999999999-12-31T23:59:59.999999999+02:00",
          "datatype": "boolean",
          "kilde": null,
          "utledetAv": {
            "regel": {
              "navn": "FørEllerLik"
            },
            "opplysninger": [
              {
                "id": "018dc0e6-0d4f-7eb1-8035-f3c666f4186b",
                "opplysningstype": "Virkningsdato",
                "verdi": "2024-02-19",
                "status": "Hypotese",
                "gyldigFraOgMed": "-9999-01-01T00:00:00+02:00",
                "gyldigTilOgMed": "+999999999-12-31T23:59:59.999999999+02:00",
                "datatype": "LocalDate",
                "kilde": null,
                "utledetAv": {
                  "regel": {
                    "navn": "SisteAv"
                  },
                  "opplysninger": [
                    {
                      "id": "018dc0e6-0d4f-7eb1-8035-f3c666f4186a",
                      "opplysningstype": "Fødselsdato",
                      "verdi": "1999-03-31",
                      "status": "Hypotese",
                      "gyldigFraOgMed": "-9999-01-01T00:00:00+02:00",
                      "gyldigTilOgMed": "+999999999-12-31T23:59:59.999999999+02:00",
                      "datatype": "LocalDate",
                      "kilde": null,
                      "utledetAv": null
                    },
                    {
                      "id": "018dc0e6-0d4b-7f11-a881-1377d9b38a2a",
                      "opplysningstype": "Søknadstidspunkt",
                      "verdi": "2024-02-19",
                      "status": "Hypotese",
                      "gyldigFraOgMed": "-9999-01-01T00:00:00+02:00",
                      "gyldigTilOgMed": "+999999999-12-31T23:59:59.999999999+02:00",
                      "datatype": "LocalDate",
                      "kilde": null,
                      "utledetAv": null
                    }
                  ],
                  "opplysningstype": null
                }
              },
              {
                "id": "018dc0e6-0d54-79b8-962a-360720ab2613",
                "opplysningstype": "Siste mulige dag bruker kan oppfylle alderskrav",
                "verdi": "2066-03-31",
                "status": "Hypotese",
                "gyldigFraOgMed": "-9999-01-01T00:00:00+02:00",
                "gyldigTilOgMed": "+999999999-12-31T23:59:59.999999999+02:00",
                "datatype": "LocalDate",
                "kilde": null,
                "utledetAv": {
                  "regel": {
                    "navn": "SisteDagIMåned"
                  },
                  "opplysninger": [
                    {
                      "id": "018dc0e6-0d54-79b8-962a-360720ab2610",
                      "opplysningstype": "Dato søker når maks alder",
                      "verdi": "2066-03-31",
                      "status": "Hypotese",
                      "gyldigFraOgMed": "-9999-01-01T00:00:00+02:00",
                      "gyldigTilOgMed": "+999999999-12-31T23:59:59.999999999+02:00",
                      "datatype": "LocalDate",
                      "kilde": null,
                      "utledetAv": {
                        "regel": {
                          "navn": "LeggTilÅr"
                        },
                        "opplysninger": [
                          {
                            "id": "018dc0e6-0d4f-7eb1-8035-f3c666f4186a",
                            "opplysningstype": "Fødselsdato",
                            "verdi": "1999-03-31",
                            "status": "Hypotese",
                            "gyldigFraOgMed": "-9999-01-01T00:00:00+02:00",
                            "gyldigTilOgMed": "+999999999-12-31T23:59:59.999999999+02:00",
                            "datatype": "LocalDate",
                            "kilde": null,
                            "utledetAv": null
                          },
                          {
                            "id": "018dc0e6-0d4f-7eb1-8035-f3c666f4186c",
                            "opplysningstype": "Aldersgrense",
                            "verdi": "67",
                            "status": "Hypotese",
                            "gyldigFraOgMed": "-9999-01-01T00:00:00+02:00",
                            "gyldigTilOgMed": "+999999999-12-31T23:59:59.999999999+02:00",
                            "datatype": "int",
                            "kilde": null,
                            "utledetAv": {
                              "regel": {
                                "navn": "Oppslag"
                              },
                              "opplysninger": [
                                {
                                  "id": "018dc0e6-0d4f-7eb1-8035-f3c666f4186b",
                                  "opplysningstype": "Virkningsdato",
                                  "verdi": "2024-02-19",
                                  "status": "Hypotese",
                                  "gyldigFraOgMed": "-9999-01-01T00:00:00+02:00",
                                  "gyldigTilOgMed": "+999999999-12-31T23:59:59.999999999+02:00",
                                  "datatype": "LocalDate",
                                  "kilde": null,
                                  "utledetAv": {
                                    "regel": {
                                      "navn": "SisteAv"
                                    },
                                    "opplysninger": [
                                      {
                                        "id": "018dc0e6-0d4f-7eb1-8035-f3c666f4186a",
                                        "opplysningstype": "Fødselsdato",
                                        "verdi": "1999-03-31",
                                        "status": "Hypotese",
                                        "gyldigFraOgMed": "-9999-01-01T00:00:00+02:00",
                                        "gyldigTilOgMed": "+999999999-12-31T23:59:59.999999999+02:00",
                                        "datatype": "LocalDate",
                                        "kilde": null,
                                        "utledetAv": null
                                      },
                                      {
                                        "id": "018dc0e6-0d4b-7f11-a881-1377d9b38a2a",
                                        "opplysningstype": "Søknadstidspunkt",
                                        "verdi": "2024-02-19",
                                        "status": "Hypotese",
                                        "gyldigFraOgMed": "-9999-01-01T00:00:00+02:00",
                                        "gyldigTilOgMed": "+999999999-12-31T23:59:59.999999999+02:00",
                                        "datatype": "LocalDate",
                                        "kilde": null,
                                        "utledetAv": null
                                      }
                                    ],
                                    "opplysningstype": null
                                  }
                                }
                              ],
                              "opplysningstype": null
                            }
                          }
                        ],
                        "opplysningstype": null
                      }
                    }
                  ],
                  "opplysningstype": null
                }
              }
            ],
            "opplysningstype": null
          }
        }
      ]
    }
    """.trimIndent()
