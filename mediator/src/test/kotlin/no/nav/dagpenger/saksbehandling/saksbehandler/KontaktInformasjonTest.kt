package no.nav.dagpenger.saksbehandling.saksbehandler

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

class KontaktInformasjonTest {
    @Test
    fun `Skal kunne parse kontakt informasjon  for både postboksadresse ,stedsadresse og ingen post adresse`() {
        val objectMapper = jacksonObjectMapper()

        objectMapper
            .readValue<KontaktInformasjon>(
                // language="JSON"
                """
                {
                    "postadresse": {
                        "postnummer": "1234",
                        "poststed": "Oslo",
                        "type": "postboksadresse",
                        "postboksnummer": "5678",
                        "postboksanlegg": "Test"
                    }
                }
                """.trimIndent(),
            ).formatertPostAdresse() shouldBe "Postboks 5678, Test, 1234 Oslo"

        objectMapper
            .readValue<KontaktInformasjon>(
                // language="JSON"
                """
                {
                  "postadresse": {
                    "type": "stedsadresse",
                    "postnummer": "7713",
                    "poststed": "STEINKJER",
                    "gatenavn": "Ogndalsvegen",
                    "husnummer": "2",
                    "husbokstav": null,
                    "adresseTilleggsnavn": null
                  }
                  }
                """.trimIndent(),
            ).formatertPostAdresse() shouldBe "Ogndalsvegen 2, 7713 STEINKJER"

        objectMapper
            .readValue<KontaktInformasjon>(
                // language="JSON"
                """
                {
                  "postadresse": null
                  }
                """.trimIndent(),
            ).formatertPostAdresse() shouldBe ""
    }
}
