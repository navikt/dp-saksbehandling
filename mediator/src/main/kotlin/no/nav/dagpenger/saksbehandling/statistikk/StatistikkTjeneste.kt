package no.nav.dagpenger.saksbehandling.statistikk

import no.nav.dagpenger.saksbehandling.api.models.BeholdningsInfoDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkDTO
import java.util.UUID

interface StatistikkTjeneste {
    fun hentSaksbehandlerStatistikk(navIdent: String): StatistikkDTO

    fun hentAntallVedtakGjort(): StatistikkDTO

    fun hentBeholdningsInfo(): BeholdningsInfoDTO

    fun hentAntallBrevSendt(): Int

    fun tidligereTilstandsendringErOverført(): Boolean

    fun oppgaveTilstandsendringer(): List<OppgaveTilstandsendring>

    fun markerTilstandsendringerSomOverført(tilstandsId: UUID)
}
