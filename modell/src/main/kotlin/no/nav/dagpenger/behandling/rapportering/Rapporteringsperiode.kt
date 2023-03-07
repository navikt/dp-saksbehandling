package no.nav.dagpenger.behandling.rapportering

import no.nav.dagpenger.behandling.visitor.RapporteringsperiodeVisitor

class Rapporteringsperiode(dager: List<Dag> = emptyList()) {

    private val dager = dager.toMutableList()

    fun leggTilDag(dag: Dag) {
        dager.add(dag)
    }

    fun accept(visitor: RapporteringsperiodeVisitor) {
        visitor.preVisitRapporteringPeriode(this)
        dager.forEach { it.accept(visitor) }
        visitor.postVisitRapporteringPeriode(this)
    }
}
