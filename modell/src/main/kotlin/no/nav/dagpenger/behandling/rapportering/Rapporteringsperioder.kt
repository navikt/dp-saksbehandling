package no.nav.dagpenger.behandling.rapportering

import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse

internal class Rapporteringsperioder(val perioder: MutableList<Rapporteringsperiode> = mutableListOf()) {
    fun håndter(rapporteringsHendelse: RapporteringsHendelse) {

        val rapporteringsperiode = Rapporteringsperiode()

        rapporteringsHendelse.populerRapporteringsperiode(rapporteringsperiode)
    }
}
