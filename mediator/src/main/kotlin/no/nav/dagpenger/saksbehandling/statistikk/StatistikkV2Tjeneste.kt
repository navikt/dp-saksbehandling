package no.nav.dagpenger.saksbehandling.statistikk

import no.nav.dagpenger.saksbehandling.api.models.StatistikkGruppeDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkSerieDTO
import no.nav.dagpenger.saksbehandling.api.models.TilstandNavnDTO

interface StatistikkV2Tjeneste {
    fun hentTilstanderMedUtløstAvFilter(statistikkFilter: StatistikkFilter): List<StatistikkGruppeDTO>

    fun hentUtløstAvMedTilstandFilter(statistikkFilter: StatistikkFilter): List<StatistikkSerieDTO>

    fun hentTilstanderMedRettighetFilter(statistikkFilter: StatistikkFilter): List<StatistikkGruppeDTO>

    fun hentRettigheterMedTilstandFilter(statistikkFilter: StatistikkFilter): List<StatistikkSerieDTO>

    fun hentResultatGrupper(statistikkFilter: StatistikkFilter): List<TilstandNavnDTO>

    fun hentResultatSerierForUtløstAv(statistikkFilter: StatistikkFilter): List<AntallOppgaverForTilstandOgUtløstAv>

    fun hentResultatSerierForRettigheter(statistikkFilter: StatistikkFilter): List<AntallOppgaverForTilstandOgRettighet>
}
