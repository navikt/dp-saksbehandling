package no.nav.dagpenger.behandling.hendelser

import java.time.LocalDate

class Rapporteringsdag(val dato: LocalDate, val fravær: Boolean) : Comparable<Rapporteringsdag> {
    override fun compareTo(other: Rapporteringsdag) = this.dato.compareTo(other.dato)
}
