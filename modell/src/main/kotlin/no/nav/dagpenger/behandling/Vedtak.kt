package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.mengde.Stønadsperiode
import no.nav.dagpenger.behandling.mengde.Tid
import no.nav.dagpenger.behandling.visitor.VedtakVisitor
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

sealed class Vedtak(
    protected val vedtakId: UUID = UUID.randomUUID(),
    protected val vedtakstidspunkt: LocalDateTime = LocalDateTime.now(),
    protected val utfall: Boolean,
    protected val virkningsdato: LocalDate,
) {
    companion object {
        fun avslag(virkningsdato: LocalDate) = Rammevedtak(utfall = false, virkningsdato = virkningsdato)
        fun innvilgelse(virkningsdato: LocalDate, grunnlag: BigDecimal, dagsats: BigDecimal, stønadsperiode: Stønadsperiode) =
            Rammevedtak(utfall = true, virkningsdato = virkningsdato, grunnlag = grunnlag, dagsats = dagsats, stønadsperiode = stønadsperiode)

        fun løpendeVedtak(virkningsdato: LocalDate, forbruk: Tid) = LøpendeVedtak(
            utfall = true,
            virkningsdato = virkningsdato,
            forbruk = forbruk
        )
    }

    abstract fun accept(visitor: VedtakVisitor)
}

class Rammevedtak(
    vedtakId: UUID = UUID.randomUUID(),
    vedtakstidspunkt: LocalDateTime = LocalDateTime.now(),
    utfall: Boolean,
    virkningsdato: LocalDate,
    private val grunnlag: BigDecimal? = null,
    private val dagsats: BigDecimal? = null,
    private val stønadsperiode: Stønadsperiode? = null,
) : Vedtak(vedtakId, vedtakstidspunkt, utfall, virkningsdato) {

    override fun accept(visitor: VedtakVisitor) {
        visitor.preVisitVedtak(vedtakId, virkningsdato, vedtakstidspunkt, utfall)
        grunnlag?.let { visitor.visitVedtakGrunnlag(it) }
        dagsats?.let { visitor.visitVedtakDagsats(it) }
        stønadsperiode?.let { visitor.visitVedtakStønadsperiode(it) }
        visitor.postVisitVedtak(vedtakId, virkningsdato, vedtakstidspunkt, utfall)
    }
}

class LøpendeVedtak(
    vedtakId: UUID = UUID.randomUUID(),
    vedtakstidspunkt: LocalDateTime = LocalDateTime.now(),
    utfall: Boolean,
    virkningsdato: LocalDate,
    private val forbruk: Tid
) : Vedtak(vedtakId, vedtakstidspunkt, utfall, virkningsdato) {
    override fun accept(visitor: VedtakVisitor) {
        visitor.preVisitVedtak(vedtakId, virkningsdato, vedtakstidspunkt, utfall)
        visitor.visitForbruk(forbruk)
        visitor.postVisitVedtak(vedtakId, virkningsdato, vedtakstidspunkt, utfall)
    }
}
