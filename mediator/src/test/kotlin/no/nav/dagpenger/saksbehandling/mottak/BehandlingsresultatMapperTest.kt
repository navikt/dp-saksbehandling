package no.nav.dagpenger.saksbehandling.mottak

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal fun String.readFile(): String =
    object {}.javaClass.getResource(this)?.readText()
        ?: throw RuntimeException("Fant ikke ressurs $this")

class BehandlingsresultatMapperTest {
    @Test
    fun `skal kunne finne periodisert verdi for boolsk opplysning`() {
        "/gjenopptak_permittering.json".readFile().also {
            val mapper = BehandlingsresultatMapper(it)
            mapper.prøvingsdato() shouldBe LocalDate.of(2026, 11, 9)
            mapper.finnesOpplysningMedVerdiTrue(
                OpplysningTyper.RETTIGHET_DAGPEGNER_UNDER_PERMITTERING_SKAL_VURDERES.opplysningTypeId,
            ) shouldBe
                true
            mapper.finnesOpplysningMedVerdiTrue(OpplysningTyper.RETTIGHET_ORDINÆRE_DAGPENGER.opplysningTypeId) shouldBe false
            mapper.finnesOpplysningMedVerdiTrue(UUID.randomUUID()) shouldBe false
        }
    }

    @Test
    fun `skal håndtere perioder uten gyldig fom eller tom`() {
        //language=json
        val mapper =
            BehandlingsresultatMapper(
                """
                {
                  "behandlingId": "01a1c01b-d51a-74f9-8885-fa461b5506e1",
                  "behandletHendelse": {
                    "datatype": "UUID",
                    "id": "ec657d35-6af4-4f3d-9ba5-bf55e71fd3b7",
                    "type": "Søknad",
                    "skjedde": "2026-11-09"
                  },
                  "rettighetsperioder": [
                    {
                      "fraOgMed": "2026-11-06",
                      "tilOgMed": "2026-11-07",
                      "harRett": true,
                      "opprinnelse": "Arvet"
                    }
                  ],
                  "opprettet": "2026-11-09T12:40:11.96251",
                  "sistEndret": "2026-11-09T15:06:42.37972",
                  "opplysninger": [
                    {
                      "opplysningTypeId": "0194881f-91d1-7df2-ba1d-4533f37fcc76",
                      "navn": "Prøvingsdato",
                      "datatype": "dato",
                      "perioder": [
                        {
                          "id": "01a1c01b-dbf0-70d6-b9a5-f1d89c87a4ea",
                          "opprettet": "2026-11-09T12:40:13.680622",
                          "type": "Faktum",
                          "opprinnelse": "Ny",
                          "verdi": {
                            "verdi": "2026-10-09",
                            "datatype": "dato"
                          },
                          "utledetAv": {
                            "regel": {
                              "navn": "Prøvingsdato"
                            },
                            "opplysninger": [
                              "01a1c01b-dbee-71ad-a83f-e215c9bd79b2"
                            ],
                            "versjon": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-behandling:2026.11.09-11.26-47083b0@sha256:25f1116c6ed7f65393d7b976793e7df366d9370d2e1b780bb6431f6033f27e18"
                          }
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent(),
            )

        mapper.prøvingsdato() shouldBe LocalDate.of(2026, 10, 9)
    }

    @Test
    fun `skal kunne finne periodisert verdi for boolsk opplysning som er false`() {
        "/gjenopptak_permittering.json".readFile().also {
            val mapper = BehandlingsresultatMapper(it)
            mapper.prøvingsdato() shouldBe LocalDate.of(2026, 11, 9)
            mapper.finnesOpplysningMedVerdiFalse(
                OpplysningTyper.RETTIGHET_DAGPEGNER_UNDER_PERMITTERING_SKAL_VURDERES.opplysningTypeId,
            ) shouldBe
                false
            mapper.finnesOpplysningMedVerdiFalse(OpplysningTyper.RETTIGHET_ORDINÆRE_DAGPENGER.opplysningTypeId) shouldBe true
            mapper.finnesOpplysningMedVerdiFalse(UUID.randomUUID()) shouldBe false
        }
    }
}
