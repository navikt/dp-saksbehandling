package no.nav.dagpenger.saksbehandling.utsending

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.postgresql.shaded.com.ongres.scram.common.bouncycastle.base64.Base64

class ArkiverbartBrevBehovTest {
    @Test
    fun `data skal inneholde base64 enkodet html`() {
        val html = "<H1>Hugga</H1><p>bubba</p>"
        val behov = ArkiverbartBrevBehov(html)
        behov.data["html"].let { base64Html ->
            Base64.decode(base64Html as String).toString(Charsets.UTF_8) shouldBe html
        }
    }

    @Test
    fun `html kan ikke være tom`() {
        shouldThrow<IllegalArgumentException> {
            ArkiverbartBrevBehov("")
        }
    }
}
