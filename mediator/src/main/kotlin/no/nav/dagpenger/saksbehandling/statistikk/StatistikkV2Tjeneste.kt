package no.nav.dagpenger.saksbehandling.statistikk

import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2GruppeDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2SerieDTO

interface StatistikkV2Tjeneste {
    fun hentTilstanderMedUtløstAvFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2GruppeDTO>

    fun hentUtløstAvMedTilstandFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2SerieDTO>

    fun hentTilstanderMedRettighetFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2GruppeDTO>

    fun hentRettigheterMedTilstandFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2SerieDTO>
}
