package no.nav.dagpenger.saksbehandling.utsending

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.postgresql.shaded.com.ongres.scram.common.bouncycastle.base64.Base64

class ArkiverbartBrevBehovTest {
    @Test
    fun `plassholder for nå`() {
        val html = "<H1>Hugga</H1><p>bubba</p>"
        val behov = ArkiverbartBrevBehov("navn", html)
        behov.data["html"] shouldNotBe null
        behov.data["html"].let { base64Html ->
            Base64.decode(base64Html as String).toString(Charsets.UTF_8) shouldBe html
        }
    }
}
