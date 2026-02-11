package no.nav.dagpenger.saksbehandling.statistikk.db

import no.nav.dagpenger.saksbehandling.api.models.BeholdningsInfoDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkGruppeDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkSerieDTO
import no.nav.dagpenger.saksbehandling.api.models.TilstandNavnDTO
import no.nav.dagpenger.saksbehandling.statistikk.StatistikkFilter

interface ProduksjonsstatistikkRepository {
    // Gamle metoder for statistikk
    fun hentSaksbehandlerStatistikk(navIdent: String): StatistikkDTO

    fun hentAntallVedtakGjort(): StatistikkDTO

    fun hentBeholdningsInfo(): BeholdningsInfoDTO

    fun hentAntallBrevSendt(): Int

    // Nye metoder for statistikk med filter
    fun hentTilstanderMedUtløstAvFilter(statistikkFilter: StatistikkFilter): List<StatistikkGruppeDTO>

    fun hentUtløstAvMedTilstandFilter(statistikkFilter: StatistikkFilter): List<StatistikkSerieDTO>

    fun hentTilstanderMedRettighetFilter(statistikkFilter: StatistikkFilter): List<StatistikkGruppeDTO>

    fun hentRettigheterMedTilstandFilter(statistikkFilter: StatistikkFilter): List<StatistikkSerieDTO>

    fun hentResultatGrupper(statistikkFilter: StatistikkFilter): List<TilstandNavnDTO>

    fun hentResultatSerierForUtløstAv(statistikkFilter: StatistikkFilter): List<AntallOppgaverForTilstandOgUtløstAv>

    fun hentResultatSerierForRettigheter(statistikkFilter: StatistikkFilter): List<AntallOppgaverForTilstandOgRettighet>
}
