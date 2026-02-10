package no.nav.dagpenger.saksbehandling.statistikk

import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2GruppeDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2SerieDTO
import no.nav.dagpenger.saksbehandling.api.models.V2StatusNavnDTO

interface StatistikkV2Tjeneste {
    fun hentTilstanderMedUtløstAvFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2GruppeDTO>

    fun hentUtløstAvMedTilstandFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2SerieDTO>

    fun hentTilstanderMedRettighetFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2GruppeDTO>

    fun hentRettigheterMedTilstandFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2SerieDTO>

    fun hentResultatGrupper(statistikkFilter: StatistikkFilter): List<V2StatusNavnDTO>

    fun hentResultatSerierForUtløstAv(statistikkFilter: StatistikkFilter): List<AntallOppgaverForTilstandOgUtløstAv>

    fun hentResultatSerierForRettigheter(statistikkFilter: StatistikkFilter): List<AntallOppgaverForTilstandOgRettighet>
}
