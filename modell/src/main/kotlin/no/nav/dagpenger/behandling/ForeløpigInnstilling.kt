package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.visitor.ForeløpigInnstillingVisitor
import java.math.BigDecimal

data class ForeløpigInnstilling(private val utfall: Boolean) {

    internal var grunnlag: BigDecimal? = null
    internal var sats: BigDecimal? = null
    fun accept(visitor: ForeløpigInnstillingVisitor) {
        visitor.visitForeløpigInnstilling(utfall)
    }
}
