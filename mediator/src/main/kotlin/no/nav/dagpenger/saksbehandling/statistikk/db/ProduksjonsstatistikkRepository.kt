package no.nav.dagpenger.saksbehandling.statistikk.db

import no.nav.dagpenger.saksbehandling.statistikk.ProduksjonsstatistikkFilter

interface ProduksjonsstatistikkRepository {
    fun hentAntallBrevSendt(): Int

    fun hentTilstanderMedUtløstAvFilter(produksjonsstatistikkFilter: ProduksjonsstatistikkFilter): List<TilstandStatistikk>

    fun hentUtløstAvMedTilstandFilter(produksjonsstatistikkFilter: ProduksjonsstatistikkFilter): List<AntallOppgaverForUtløstAv>

    fun hentTilstanderMedRettighetFilter(produksjonsstatistikkFilter: ProduksjonsstatistikkFilter): List<TilstandStatistikk>

    fun hentRettigheterMedTilstandFilter(produksjonsstatistikkFilter: ProduksjonsstatistikkFilter): List<AntallOppgaverForRettighet>

    fun hentResultatSerierForUtløstAv(produksjonsstatistikkFilter: ProduksjonsstatistikkFilter): List<AntallOppgaverForTilstandOgUtløstAv>

    fun hentResultatSerierForRettigheter(
        produksjonsstatistikkFilter: ProduksjonsstatistikkFilter,
    ): List<AntallOppgaverForTilstandOgRettighet>
}
