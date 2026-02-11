package no.nav.dagpenger.saksbehandling.statistikk.db

import no.nav.dagpenger.saksbehandling.api.models.BeholdningsInfoDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkDTO
import no.nav.dagpenger.saksbehandling.statistikk.OppgaveITilstand
import java.util.UUID

interface SaksbehandlingsstatistikkRepository {
    fun hentSaksbehandlerStatistikk(navIdent: String): StatistikkDTO

    fun hentAntallVedtakGjort(): StatistikkDTO

    fun hentBeholdningsInfo(): BeholdningsInfoDTO

    fun hentAntallBrevSendt(): Int

    fun tidligereTilstandsendringerErOverført(): Boolean

    fun oppgaveTilstandsendringer(): List<OppgaveITilstand>

    fun markerTilstandsendringerSomOverført(tilstandId: UUID)
}
