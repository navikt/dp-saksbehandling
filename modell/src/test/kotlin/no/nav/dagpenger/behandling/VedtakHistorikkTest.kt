package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.mengde.Enhet.Companion.arbeidsuker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakHistorikkTest {

    private val vedtakHistorikk = VedtakHistorikk()
    @Test
    fun `Skal lage ta vare på alle vedtakfakta gitt virkningsdato`() {
        val vedtak1 = Vedtak.innvilgelse(
            virkningsdato = LocalDate.now().minusDays(1),
            grunnlag = 450000.toBigDecimal(), dagsats = 700.toBigDecimal(),
            stønadsperiode = 52.arbeidsuker,
        )

        val vedtak2 = Vedtak.innvilgelse(
            virkningsdato = LocalDate.now().plusDays(1),
            grunnlag = 450000.toBigDecimal(), dagsats = 755.toBigDecimal(),
            stønadsperiode = 104.arbeidsuker,
        )

        vedtakHistorikk.leggTilVedtak(vedtak1)
        vedtakHistorikk.leggTilVedtak(vedtak2)

        assertEquals(52.arbeidsuker, vedtakHistorikk.stønadsperiodehistorikk.get(LocalDate.now()))
        assertEquals(700.toBigDecimal(), vedtakHistorikk.dagsatshistorikk.get(LocalDate.now()))
        assertEquals(104.arbeidsuker, vedtakHistorikk.stønadsperiodehistorikk.get(LocalDate.now().plusDays(2)))
        assertEquals(755.toBigDecimal(), vedtakHistorikk.dagsatshistorikk.get(LocalDate.now().plusDays(2)))
    }
}
