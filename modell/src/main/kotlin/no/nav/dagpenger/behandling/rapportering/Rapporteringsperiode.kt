package no.nav.dagpenger.behandling.rapportering

internal class Rapporteringsperiode(dager: List<Dag> = emptyList()) {

    private val dager = dager.toMutableList()

    fun leggTilDag(dag: Dag) {
        dager.add(dag)
    }
}
