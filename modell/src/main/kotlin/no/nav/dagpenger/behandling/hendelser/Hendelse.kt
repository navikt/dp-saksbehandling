package no.nav.dagpenger.behandling

import java.math.BigDecimal

abstract class Hendelse(private val ident: String) {
    private val behov = mutableListOf<Behov>()
    fun behov(): List<Behov> = behov.toList()
    fun behov(behov: Behov) = this.behov.add(behov)
}

sealed class Behov
object Aldersbehov : Behov()
class VedtakInnvilgetBehov(private val sats: BigDecimal) : Behov()
class VedtakAvslåttBehov(private val begrunnelse: String) : Behov() // TODO: Begrunnelse = liste med ikke oppfylte vilkår
