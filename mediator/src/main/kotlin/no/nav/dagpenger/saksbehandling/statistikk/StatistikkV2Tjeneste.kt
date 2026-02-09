package no.nav.dagpenger.saksbehandling.statistikk

import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2GruppeDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2SerieDTO

interface StatistikkV2Tjeneste {
    fun hentStatuserForUtløstAvFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2GruppeDTO>

    fun hentUtløstAvSerier(statistikkFilter: StatistikkFilter): List<StatistikkV2SerieDTO>

    fun hentStatuserForRettighetstypeFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2GruppeDTO>

    fun hentRettighetstypeSerier(statistikkFilter: StatistikkFilter): List<StatistikkV2SerieDTO>
}