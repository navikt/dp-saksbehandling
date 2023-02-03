package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.visitor.VedtakVisitor

class Vedtak(private val utfall: Boolean) {

    fun accept(visitor: VedtakVisitor) {
        visitor.visitVedtak(utfall)
    }
}
