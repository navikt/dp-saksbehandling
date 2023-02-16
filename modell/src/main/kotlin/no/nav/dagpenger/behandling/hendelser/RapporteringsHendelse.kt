package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.entitet.Periode
import no.nav.dagpenger.behandling.rapportering.Dag
import no.nav.dagpenger.behandling.rapportering.Rapporteringsperiode
import java.util.UUID

class RapporteringsHendelse(
    ident: String,
    internal val rapporteringsId: UUID,
    rapporteringsdager: List<Rapporteringsdag>,
) : Hendelse(ident) {
    private val rapporteringsdager = rapporteringsdager.sorted()
    internal fun populerRapporteringsperiode(rapporteringsperiode: Rapporteringsperiode) {
        rapporteringsdager.forEach {
            val dag = when (it.fravær) {
                true -> Dag.fraværsdag(it.dato)
                false -> Dag.arbeidsdag(it.dato)
            }
            rapporteringsperiode.leggTilDag(dag)
        }
    }

    internal fun tilPeriode() = Periode(rapporteringsdager.first().dato, rapporteringsdager.last().dato)
}
