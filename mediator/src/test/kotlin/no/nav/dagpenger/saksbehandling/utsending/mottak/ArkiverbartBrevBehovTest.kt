package no.nav.dagpenger.saksbehandling.utsending.mottak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.utsending.ArkiverbartBrevBehov
import org.junit.jupiter.api.Test
import java.util.Base64

class ArkiverbartBrevBehovTest {
    private val oppgaveId = UUIDv7.ny()
    private val ident = "12345678901"
    private val utsendingSak = UtsendingSak(id = "consul", kontekst = "doctus")

    @Test
    fun `data skal inneholde base64 enkodet html`() {
        val html = "<H1>Hugga</H1><p>bubba</p>"
        val behov = ArkiverbartBrevBehov(oppgaveId, html, ident, utsendingSak)
        behov.data()["htmlBase64"].let { base64Html ->
            Base64.getDecoder().decode(base64Html as String).toString(Charsets.UTF_8) shouldBe html
        }
    }

    @Test
    fun `html kan ikke være tom`() {
        shouldThrow<IllegalArgumentException> {
            ArkiverbartBrevBehov(oppgaveId, "", ident, utsendingSak)
        }
    }
}
