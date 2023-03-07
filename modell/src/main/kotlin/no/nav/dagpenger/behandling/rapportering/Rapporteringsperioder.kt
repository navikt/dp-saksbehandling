package no.nav.dagpenger.behandling.rapportering

import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.visitor.PersonVisitor

class Rapporteringsperioder(val perioder: MutableList<Rapporteringsperiode> = mutableListOf()) {
    fun håndter(rapporteringsHendelse: RapporteringsHendelse) {
        val rapporteringsperiode = Rapporteringsperiode()
        rapporteringsHendelse.populerRapporteringsperiode(rapporteringsperiode)
        perioder.add(rapporteringsperiode)
    }

    fun accept(visitor: PersonVisitor) {
        visitor.preVisitRapporteringsperioder(this)
        perioder.forEach { it.accept(visitor) }
        visitor.postVisitRapporteringsperioder(this)
    }
}
