package no.nav.dagpenger.saksbehandling.api

import io.ktor.server.application.Application
import no.nav.dagpenger.saksbehandling.Configuration.applicationCallParser
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.api.auth.authConfig
import no.nav.dagpenger.saksbehandling.statistikk.StatistikkTjeneste
import no.nav.dagpenger.saksbehandling.statistikk.statistikkApi

internal fun Application.installerApis(
    oppgaveMediator: OppgaveMediator,
    oppgaveDTOMapper: OppgaveDTOMapper,
    statistikkTjeneste: StatistikkTjeneste,
) {
    this.authConfig()
    this.oppgaveApi(
        oppgaveMediator,
        oppgaveDTOMapper,
        applicationCallParser,
    )
    statistikkApi(statistikkTjeneste)
}
