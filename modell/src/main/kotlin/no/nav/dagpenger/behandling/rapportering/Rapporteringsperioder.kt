package no.nav.dagpenger.behandling.rapportering

import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.visitor.DagVisitor

class Rapporteringsperioder(val perioder: MutableList<Rapporteringsperiode> = mutableListOf()) {
    fun håndter(rapporteringsHendelse: RapporteringsHendelse) {

        val rapporteringsperiode = Rapporteringsperiode()

        rapporteringsHendelse.populerRapporteringsperiode(rapporteringsperiode)
    }

    fun accept(visitor: DagVisitor) {
        perioder.forEach { it.accept(visitor) }
    }
}
