package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.NyRettighetsbehandling.Companion.harSøknadUUID
import no.nav.dagpenger.behandling.PersonIdentifikator.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.hendelser.AldersvilkårLøsning
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse

class Person private constructor(private val ident: PersonIdentifikator) : Aktivitetskontekst by ident {
    private val behandlinger = mutableListOf<Behandling>()

    constructor(ident: String) : this(ident.tilPersonIdentfikator())

    fun håndter(søknadHendelse: SøknadHendelse) {
        kontekst(søknadHendelse)
        if(behandlinger.harSøknadUUID(søknadHendelse.søknadUUID)) return
        søknadHendelse.info("Har mottatt ny søknadhendelse")
        val behandling = NyRettighetsbehandling(søknadHendelse.søknadUUID)
        behandlinger.add(behandling)
        behandling.håndter(søknadHendelse)
    }

    fun håndter(aldersvilkårLøsning: AldersvilkårLøsning) {
        kontekst(aldersvilkårLøsning)
        behandlinger.forEach { it.håndter(aldersvilkårLøsning) }
    }

    fun harBehandlinger() = this.behandlinger.isNotEmpty()
    fun antallBehandlinger() = this.behandlinger.size

    private fun kontekst(hendelse: Hendelse) {
        hendelse.kontekst(this)
    }
}
