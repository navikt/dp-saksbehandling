package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.visitor.VedtakVisitor

class VedtakHistorikk(private val vedtak: MutableList<Vedtak> = mutableListOf()) {

    fun leggTilVedtak(vedtak: Vedtak) {
        this.vedtak.add(vedtak)
    }

    fun accept(visitor: VedtakVisitor) {
        vedtak.forEach { it.accept(visitor) }
    }
}
