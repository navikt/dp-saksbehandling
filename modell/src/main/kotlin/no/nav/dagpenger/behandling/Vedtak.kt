package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.entitet.Dagpengerettighet
import no.nav.dagpenger.behandling.entitet.Timer
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
        fun avslag(virkningsdato: LocalDate) = Avslag(virkningsdato = virkningsdato)

        fun innvilgelse(
            virkningsdato: LocalDate,
            grunnlag: BigDecimal,
            dagsats: BigDecimal,
            stønadsperiode: Stønadsperiode,
            dagpengerettighet: Dagpengerettighet,
            fastsattArbeidstidPerDag: Timer,
            tom: LocalDate?,
        ) = Rammevedtak(
            virkningsdato = virkningsdato,
            grunnlag = grunnlag,
            dagsats = dagsats,
            stønadsperiode = stønadsperiode,
            dagpengerettighet = dagpengerettighet, // TODO: Skal rettighetslista bare inneholde innvilgede rettigheter? Hva med avslag på utdanning f.eks.?
            fastsattArbeidstidPerDag = fastsattArbeidstidPerDag,
            tom = tom, // TODO: Noe mer fornuftig
        )

        fun løpendeVedtak(virkningsdato: LocalDate, forbruk: Tid, utfall: Boolean) = LøpendeVedtak(
            utfall = utfall,
            virkningsdato = virkningsdato,
            forbruk = forbruk,
        )
    }

    abstract fun accept(visitor: VedtakVisitor)
}

class Avslag(
    vedtakId: UUID = UUID.randomUUID(),
    vedtakstidspunkt: LocalDateTime = LocalDateTime.now(),
    virkningsdato: LocalDate,
) : Vedtak(vedtakId, vedtakstidspunkt, utfall = false, virkningsdato) {
    override fun accept(visitor: VedtakVisitor) {
        visitor.preVisitVedtak(vedtakId, virkningsdato, vedtakstidspunkt, utfall)
        visitor.postVisitVedtak(vedtakId, virkningsdato, vedtakstidspunkt, utfall)
    }
}

class Rammevedtak(
    vedtakId: UUID = UUID.randomUUID(),
    vedtakstidspunkt: LocalDateTime = LocalDateTime.now(),
    virkningsdato: LocalDate,
    private val fastsattArbeidstidPerDag: Timer,
    private val grunnlag: BigDecimal,
    private val dagsats: BigDecimal,
    private val stønadsperiode: Stønadsperiode,
    private val dagpengerettighet: Dagpengerettighet,
    private val tom: LocalDate?,
) : Vedtak(vedtakId, vedtakstidspunkt, utfall = true, virkningsdato) {

    override fun accept(visitor: VedtakVisitor) {
        visitor.preVisitVedtak(vedtakId, virkningsdato, vedtakstidspunkt, utfall)
        grunnlag.let { visitor.visitVedtakGrunnlag(it) }
        dagsats.let { visitor.visitVedtakDagsats(it) }
        stønadsperiode.let { visitor.visitVedtakStønadsperiode(it) }
        visitor.visitFastsattArbeidstidPerDag(fastsattArbeidstidPerDag)
        visitor.visitVedtakDagpengerettighet(dagpengerettighet)
        visitor.postVisitVedtak(vedtakId, virkningsdato, vedtakstidspunkt, utfall)
    }
}

class LøpendeVedtak(
    vedtakId: UUID = UUID.randomUUID(),
    vedtakstidspunkt: LocalDateTime = LocalDateTime.now(),
    utfall: Boolean,
    virkningsdato: LocalDate,
    private val forbruk: Tid,
) : Vedtak(vedtakId, vedtakstidspunkt, utfall, virkningsdato) {
    override fun accept(visitor: VedtakVisitor) {
        visitor.preVisitVedtak(vedtakId, virkningsdato, vedtakstidspunkt, utfall)
        visitor.visitForbruk(forbruk)
        visitor.postVisitVedtak(vedtakId, virkningsdato, vedtakstidspunkt, utfall)
    }
}
