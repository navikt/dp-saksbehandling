package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.visitor.VedtakVisitor
import java.math.BigDecimal

class Vedtak(private val utfall: Boolean, private val grunnlag: BigDecimal? = null, private val sats: BigDecimal? = null) {

    fun accept(visitor: VedtakVisitor) {
        visitor.visitVedtak(utfall)
    }
}
