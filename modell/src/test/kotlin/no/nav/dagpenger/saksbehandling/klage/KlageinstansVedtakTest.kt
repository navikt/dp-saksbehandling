package no.nav.dagpenger.saksbehandling.klage

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class KlageinstansVedtakTest {
    @Test
    fun `utfall som skal starte revurdering`() {
        listOf(
            KlageinstansVedtak.Klage.Utfall.MEDHOLD,
            KlageinstansVedtak.Klage.Utfall.DELVIS_MEDHOLD,
            KlageinstansVedtak.Klage.Utfall.OPPHEVET,
            KlageinstansVedtak.Klage.Utfall.RETUR,
            KlageinstansVedtak.Klage.Utfall.UGUNST,
        ).forEach { utfall ->
            utfall.skalStarteRevurdering() shouldBe true
        }
    }

    @Test
    fun `utfall som ikke skal starte revurdering`() {
        listOf(
            KlageinstansVedtak.Klage.Utfall.STADFESTELSE,
            KlageinstansVedtak.Klage.Utfall.TRUKKET,
            KlageinstansVedtak.Klage.Utfall.AVVIST,
            KlageinstansVedtak.Klage.Utfall.HENLAGT,
        ).forEach { utfall ->
            utfall.skalStarteRevurdering() shouldBe false
        }
    }
}
