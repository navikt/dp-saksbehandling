package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.rapportering.Dag
import no.nav.dagpenger.behandling.rapportering.Rapporteringsperiode
import java.util.UUID

class RapporteringsHendelse(
    private val ident: String,
    internal val rapporteringsId: UUID,
    private val rapporteringsdager: List<Rapporteringsdag>
) :
    Hendelse(ident) {
    internal fun populerRapporteringsperiode(rapporteringsperiode: Rapporteringsperiode) {
        rapporteringsdager.forEach {
            val dag = when (it.fravær) {
                true -> Dag.fraværsdag(it.dato)
                false -> Dag.arbeidsdag(it.dato)
            }
            rapporteringsperiode.leggTilDag(dag)
        }
    }
}
