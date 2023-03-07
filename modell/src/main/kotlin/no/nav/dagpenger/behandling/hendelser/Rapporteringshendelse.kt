package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.entitet.Periode
import no.nav.dagpenger.behandling.entitet.Timer.Companion.timer
import no.nav.dagpenger.behandling.rapportering.Dag
import no.nav.dagpenger.behandling.rapportering.Rapporteringsperiode
import java.util.UUID

class Rapporteringshendelse(
    ident: String,
    internal val rapporteringsId: UUID,
    rapporteringsdager: List<Rapporteringsdag>,
) : Hendelse(ident) {
    private val rapporteringsdager = rapporteringsdager.sorted()
    internal fun populerRapporteringsperiode(rapporteringsperiode: Rapporteringsperiode) {
        rapporteringsdager.forEach {
            val dag = when (it.fravær) {
                true -> Dag.fraværsdag(it.dato)
                false -> Dag.arbeidsdag(it.dato, it.timer.timer)
            }
            rapporteringsperiode.leggTilDag(dag)
        }
    }

    internal fun somPeriode() = Periode(rapporteringsdager.first().dato, rapporteringsdager.last().dato)
}
