package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.PersonIdentifikator.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.hendelser.AldersvilkårLøsning
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse

class Person private constructor(private val ident: PersonIdentifikator) : Aktivitetskontekst by ident {
    private val behandlinger = mutableListOf<Behandling>()

    constructor(ident: String) : this(ident.tilPersonIdentfikator())

    fun håndter(søknadHendelse: SøknadHendelse) {
        søknadHendelse.kontekst(this)
        søknadHendelse.info("Har mottatt ny søknadhendelse")
        val behandling = NyRettighetsbehandling()
        behandlinger.add(behandling)
        behandling.håndter(søknadHendelse)
    }

    fun håndter(aldersvilkårLøsning: AldersvilkårLøsning) {
        behandlinger.forEach { it.håndter(aldersvilkårLøsning) }
    }

    fun harBehandlinger() = this.behandlinger.isNotEmpty()
}
