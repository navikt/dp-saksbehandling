package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.PersonIdentifikator.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse

class Person private constructor(ident: PersonIdentifikator) {
    private val behandlinger = mutableListOf<Behandling>()

    constructor(ident: String) : this(ident.tilPersonIdentfikator())

    fun håndter(søknadHendelse: SøknadHendelse) {
        val behandling = NyRettighetsbehandling()
        behandlinger.add(behandling)
        behandling.håndter(søknadHendelse)
    }

    fun harBehandlinger() = this.behandlinger.isNotEmpty()
}
