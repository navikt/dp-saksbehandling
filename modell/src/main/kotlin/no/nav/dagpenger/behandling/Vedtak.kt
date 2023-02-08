package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.visitor.VedtakVisitor
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Vedtak private constructor(
    private val vedtakId: UUID = UUID.randomUUID(),
    private val vedtakstidspunkt: LocalDateTime = LocalDateTime.now(),
    private val utfall: Boolean,
    private val virkningsdato: LocalDate,
    private val grunnlag: BigDecimal? = null,
    private val dagsats: BigDecimal? = null,
    private val stønadsperiode: BigDecimal? = null
) {

    companion object {
        fun avslag(virkningsdato: LocalDate) = Vedtak(utfall = false, virkningsdato = virkningsdato)
        fun innvilgelse(virkningsdato: LocalDate, grunnlag: BigDecimal, dagsats: BigDecimal, stønadsperiode: BigDecimal) =
            Vedtak(utfall = true, virkningsdato = virkningsdato, grunnlag = grunnlag, dagsats = dagsats, stønadsperiode = stønadsperiode)
    }

    fun accept(visitor: VedtakVisitor) {
        visitor.visitVedtak(utfall = utfall, grunnlag = grunnlag, dagsats = dagsats, stønadsperiode = stønadsperiode)
    }
}
