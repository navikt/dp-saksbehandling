package no.nav.dagpenger.saksbehandling.mottak

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import org.junit.jupiter.api.Test

class JsonHelperTest {
    @Test
    fun `Parse avklaringstyper`() {
        val newNode: JsonNode = objectMapper.readTree(avklaringsJson)

        newNode["avklaringer"].avklaringstyper() shouldBe setOf("EØSArbeid", "HarRapportertInntektNesteMåned", "SykepengerSiste36Måneder")

        objectMapper.readTree(tommeAvklaringerJson)["avklaringer"].avklaringstyper() shouldBe emptySet()

        objectMapper.readTree("""{}""")["avklaringer"].avklaringstyper() shouldBe emptySet()
    }

    //language=JSON
    private val tommeAvklaringerJson = """{"avklaringer":[]}"""

    //language=JSON
    private val avklaringsJson = """{
      "avklaringer": [
        {
          "type": "EØSArbeid",
          "utfall": "Manuell",
          "begrunnelse": "Personen har oppgitt arbeid fra EØS"
        },
        {
          "type": "HarRapportertInntektNesteMåned",
          "utfall": "Manuell",
          "begrunnelse": "Personen har inntekter som tilhører neste inntektsperiode"
        },
        {
          "type": "SykepengerSiste36Måneder",
          "utfall": "Manuell",
          "begrunnelse": "Personen har sykepenger som kan være svangerskapsrelaterte"
        }
      ]
  }"""
}
