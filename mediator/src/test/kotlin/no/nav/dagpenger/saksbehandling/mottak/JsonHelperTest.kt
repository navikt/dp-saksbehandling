package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.mottak.ForslagTilVedtakMottak.Companion.AVKLARINGER
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
