package no.nav.dagpenger.saksbehandling.statistikk.db

import no.nav.dagpenger.saksbehandling.api.models.BeholdningsInfoDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkDTO
import no.nav.dagpenger.saksbehandling.statistikk.ProduksjonsstatistikkFilter

interface ProduksjonsstatistikkRepository {
    // Gamle metoder for statistikk
    fun hentSaksbehandlerStatistikk(navIdent: String): StatistikkDTO

    fun hentAntallVedtakGjort(): StatistikkDTO

    fun hentBeholdningsInfo(): BeholdningsInfoDTO

    fun hentAntallBrevSendt(): Int

    // Nye metoder for statistikk med filter
    fun hentTilstanderMedUtløstAvFilter(produksjonsstatistikkFilter: ProduksjonsstatistikkFilter): List<TilstandStatistikk>

    fun hentUtløstAvMedTilstandFilter(produksjonsstatistikkFilter: ProduksjonsstatistikkFilter): List<AntallOppgaverForUtløstAv>

    fun hentTilstanderMedRettighetFilter(produksjonsstatistikkFilter: ProduksjonsstatistikkFilter): List<TilstandStatistikk>

    fun hentRettigheterMedTilstandFilter(produksjonsstatistikkFilter: ProduksjonsstatistikkFilter): List<AntallOppgaverForRettighet>

    fun hentResultatSerierForUtløstAv(produksjonsstatistikkFilter: ProduksjonsstatistikkFilter): List<AntallOppgaverForTilstandOgUtløstAv>

    fun hentResultatSerierForRettigheter(
        produksjonsstatistikkFilter: ProduksjonsstatistikkFilter,
    ): List<AntallOppgaverForTilstandOgRettighet>
}
