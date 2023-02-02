package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.NyRettighetsbehandling.Companion.harSøknadUUID
import no.nav.dagpenger.behandling.PersonIdentifikator.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse

class Person private constructor(private val ident: PersonIdentifikator) : Aktivitetskontekst by ident {
    private val behandlinger = mutableListOf<NyRettighetsbehandling>()

    constructor(ident: String) : this(ident.tilPersonIdentfikator())

    companion object {
        const val kontekstType = "Person"
    }

    fun håndter(søknadHendelse: SøknadHendelse) {
        kontekst(søknadHendelse)
        if (behandlinger.harSøknadUUID(søknadHendelse.søknadUUID())) return
        søknadHendelse.info("Har mottatt ny søknadhendelse")
        val behandling = NyRettighetsbehandling(søknadHendelse.søknadUUID())
        behandlinger.add(behandling)
        behandling.håndter(søknadHendelse)
    }

    fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat) {
        kontekst(paragraf423AlderResultat)
        behandlinger.forEach { it.håndter(paragraf423AlderResultat) }
    }
    fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat) {
        kontekst(grunnlagOgSatsResultat)
        behandlinger.forEach { it.håndter(grunnlagOgSatsResultat) }
    }

    // TODO vi må fikse visitor
    fun harBehandlinger() = this.behandlinger.isNotEmpty()
    fun antallBehandlinger() = this.behandlinger.size
    fun behandlinger() = this.behandlinger.toList()
    fun sisteBehandlingId() = this.behandlinger.first().behandlingsId
    fun ident() = this.ident.identifikator()

    private fun kontekst(hendelse: Hendelse) {
        hendelse.kontekst(this)
    }
}
