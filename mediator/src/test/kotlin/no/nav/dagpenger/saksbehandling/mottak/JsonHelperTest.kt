package no.nav.dagpenger.saksbehandling.mottak

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.helper.fileAsText
import no.nav.dagpenger.saksbehandling.mottak.ForslagTilVedtakMottak.Companion.AVKLARINGER
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import org.junit.jupiter.api.Test

class JsonHelperTest {
    @Test
    fun `Parse avklaringstyper`() {
        lagJsonMedFilter(
            avklaringsMap,
        ).emneknagger() shouldBe setOf("EØSArbeid", "HarRapportertInntektNesteMåned", "SykepengerSiste36Måneder")

        lagJsonMedFilter(mapOf("avklaringer" to emptyList<String>())).emneknagger() shouldBe emptySet()

        lagJsonMedFilter(emptyMap()).emneknagger() shouldBe emptySet()
    }

    @Test
    fun `Skal kunne parse sak fra et vedtak fattet hendelse json`() {
        val vedtakFattetJson =
            "/vedtak_fattet.json".fileAsText().let {
                objectMapper.readTree(it)
            }

        vedtakFattetJson["opplysninger"].sak() shouldBe Sak("14952361", "Arena")
    }

    private fun lagJsonMedFilter(json: Map<String, Any>): JsonMessage {
        return JsonMessage.newMessage(json).also { it.interestedIn(AVKLARINGER) }
    }

    private val avklaringsMap =
        mapOf(
            "avklaringer" to
                listOf(
                    mapOf("type" to "EØSArbeid"),
                    mapOf("type" to "HarRapportertInntektNesteMåned"),
                    mapOf("type" to "SykepengerSiste36Måneder"),
                ),
        )
}
