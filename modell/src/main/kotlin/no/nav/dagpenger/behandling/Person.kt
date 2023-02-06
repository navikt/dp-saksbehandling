package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.NyRettighetsbehandling.Companion.harSøknadUUID
import no.nav.dagpenger.behandling.PersonIdentifikator.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.hendelser.BeslutterHendelse
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.visitor.PersonVisitor

class Person private constructor(private val ident: PersonIdentifikator) : Aktivitetskontekst by ident {
    private val behandlinger = mutableListOf<NyRettighetsbehandling>()

    constructor(ident: String) : this(ident.tilPersonIdentfikator())

    companion object {
        const val kontekstType = "Person"
    }

    fun accept(visitor: PersonVisitor) {
        visitor.visitPerson(ident)
        behandlinger.forEach {
            it.accept(visitor)
        }
    }

    fun håndter(søknadHendelse: SøknadHendelse) {
        kontekst(søknadHendelse)
        if (behandlinger.harSøknadUUID(søknadHendelse.søknadUUID())) return
        søknadHendelse.info("Har mottatt ny søknadhendelse")
        val behandling = NyRettighetsbehandling(this, søknadHendelse.søknadUUID())
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

    fun håndter(beslutterHendelse: BeslutterHendelse) {
        kontekst(beslutterHendelse)
        behandlinger.forEach { it.håndter(beslutterHendelse) }
    }

    fun ident() = this.ident.identifikator()

    private fun kontekst(hendelse: Hendelse) {
        hendelse.kontekst(this)
    }
}
