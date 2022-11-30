package no.nav.dagpenger.behandling

import java.util.UUID

abstract class Hendelse(private val ident: String) {
    private val behov = mutableListOf<Behov>()
    fun behov(): List<Behov> = behov.toList()
    fun behov(behov: Behov) = this.behov.add(behov)
}

class SøknadHendelse(private val søknadUUID: UUID, ident: String) : Hendelse(ident)

sealed class Behov
object Aldersbehov : Behov()
