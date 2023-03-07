package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.mengde.Stønadsperiode
import no.nav.dagpenger.behandling.mengde.Tid
import no.nav.dagpenger.behandling.visitor.VedtakHistorikkVisitor
import no.nav.dagpenger.behandling.visitor.VedtakVisitor
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class VedtakHistorikk(private val vedtak: MutableList<Vedtak> = mutableListOf()) {

    internal val dagsatshistorikk = TemporalCollection<BigDecimal>()
    internal val grunnlaghistorikk = TemporalCollection<BigDecimal>()
    internal val stønadsperiodehistorikk = TemporalCollection<Stønadsperiode>()
    internal val gjensteåndeStønadsperiode = TemporalCollection<Stønadsperiode>()

    fun leggTilVedtak(vedtak: Vedtak) {
        this.vedtak.add(vedtak)
        OppdaterVedtakFakta(vedtak, this)
    }

    fun accept(visitor: VedtakHistorikkVisitor) {
        if (gjensteåndeStønadsperiode.historikk().isNotEmpty()) {
            visitor.visitGjenståendeStønadsperiode(gjensteåndeStønadsperiode.get(LocalDate.now()))
        }
        visitor.preVisitVedtak()
        vedtak.forEach { it.accept(visitor) }
        visitor.postVisitVedtak()
    }

    private class OppdaterVedtakFakta(vedtak: Vedtak, private val vedtakHistorikk: VedtakHistorikk) : VedtakVisitor {
        init {
            vedtak.accept(this)
        }

        lateinit var virkningsdato: LocalDate
        override fun preVisitVedtak(
            vedtakId: UUID,
            virkningsdato: LocalDate,
            vedtakstidspunkt: LocalDateTime,
            utfall: Boolean,
        ) {
            this.virkningsdato = virkningsdato
        }

        override fun visitVedtakDagsats(dagsats: BigDecimal) {
            vedtakHistorikk.dagsatshistorikk.put(virkningsdato, dagsats)
        }

        override fun visitVedtakStønadsperiode(stønadsperiode: Stønadsperiode) {
            vedtakHistorikk.stønadsperiodehistorikk.put(virkningsdato, stønadsperiode)
            vedtakHistorikk.gjensteåndeStønadsperiode.put(virkningsdato, stønadsperiode)
        }

        override fun visitVedtakGrunnlag(grunnlag: BigDecimal) {
            vedtakHistorikk.grunnlaghistorikk.put(virkningsdato, grunnlag)
        }

        override fun visitForbruk(forbruk: Tid) {
            val gjenstående = vedtakHistorikk.gjensteåndeStønadsperiode.get(virkningsdato)
            vedtakHistorikk.gjensteåndeStønadsperiode.put(virkningsdato, gjenstående - forbruk)
        }
    }
}
