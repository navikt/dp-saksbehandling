package no.nav.dagpenger.behandling.rapportering

import no.nav.dagpenger.behandling.visitor.DagVisitor

class Rapporteringsperiode(dager: List<Dag> = emptyList()) {

    private val dager = dager.toMutableList()

    fun leggTilDag(dag: Dag) {
        dager.add(dag)
    }

    fun accept(visitor: DagVisitor) {
        dager.forEach { it.accept(visitor) }
    }
}
