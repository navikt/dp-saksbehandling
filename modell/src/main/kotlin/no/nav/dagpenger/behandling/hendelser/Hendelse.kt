package no.nav.dagpenger.behandling

abstract class Hendelse(private val ident: String) {
    private val behov = mutableListOf<Behov>()
    fun behov(): List<Behov> = behov.toList()
    fun behov(behov: Behov) = this.behov.add(behov)
}

sealed class Behov
object Aldersbehov : Behov()
