package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.entitet.Rettighet
import no.nav.dagpenger.behandling.entitet.Rettighetstype
import no.nav.dagpenger.behandling.mengde.Enhet.Companion.arbeidsuker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakHistorikkTest {

    private val vedtakHistorikk = VedtakHistorikk()
    @Test
    fun `Skal ta vare på alle vedtaksfakta gitt virkningsdato`() {

        val rettigheter = mutableListOf<Rettighet>()
        rettigheter.add(Rettighet(rettighetstype = Rettighetstype.OrdinæreDagpenger, utfall = true, fomDato = LocalDate.now(), tomDato = null))

        val vedtak1 = Vedtak.innvilgelse(
            virkningsdato = LocalDate.now().minusDays(1),
            grunnlag = 450000.toBigDecimal(),
            dagsats = 700.toBigDecimal(),
            stønadsperiode = 52.arbeidsuker,
            rettigheter = rettigheter,
        )

        val vedtak2 = Vedtak.innvilgelse(
            virkningsdato = LocalDate.now().plusDays(1),
            grunnlag = 450000.toBigDecimal(),
            dagsats = 755.toBigDecimal(),
            stønadsperiode = 104.arbeidsuker,
            rettigheter = rettigheter,
        )

        vedtakHistorikk.leggTilVedtak(vedtak1)
        vedtakHistorikk.leggTilVedtak(vedtak2)

        assertEquals(52.arbeidsuker, vedtakHistorikk.stønadsperiodehistorikk.get(LocalDate.now()))
        assertEquals(700.toBigDecimal(), vedtakHistorikk.dagsatshistorikk.get(LocalDate.now()))
        assertEquals(104.arbeidsuker, vedtakHistorikk.stønadsperiodehistorikk.get(LocalDate.now().plusDays(2)))
        assertEquals(755.toBigDecimal(), vedtakHistorikk.dagsatshistorikk.get(LocalDate.now().plusDays(2)))
    }
}
