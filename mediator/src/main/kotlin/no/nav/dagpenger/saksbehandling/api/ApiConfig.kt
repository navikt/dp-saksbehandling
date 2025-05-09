package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.document
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.routing.routing
import no.nav.dagpenger.saksbehandling.Configuration.applicationCallParser
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.auth.authConfig
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import no.nav.dagpenger.saksbehandling.statistikk.StatistikkTjeneste
import no.nav.dagpenger.saksbehandling.statistikk.statistikkApi

internal fun Application.installerApis(
    oppgaveMediator: OppgaveMediator,
    oppgaveDTOMapper: OppgaveDTOMapper,
    statistikkTjeneste: StatistikkTjeneste,
    klageMediator: KlageMediator,
    klageDTOMapper: KlageDTOMapper,
) {
    this.authConfig()
    install(CallId) {
        header("callId")
        verify { it.isNotEmpty() }
        generate { UUIDv7.ny().toString() }
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    install(CallLogging) {
        disableDefaultColors()
        filter { call ->
            !setOf(
                "isalive",
                "isready",
                "metrics",
            ).contains(call.request.document())
        }
        format { call ->
            val status = call.response.status()?.value ?: "Unhandled"
            val method = call.request.httpMethod.value
            val path = call.request.path()
            val duration = call.processingTimeMillis()
            val queryParams = call.request.queryParameters.entries()
            "$status $method $path $queryParams $duration ms"
        }
    }
    this.statusPages()

    routing {
        swaggerUI(path = "openapi", swaggerFile = "saksbehandling-api.yaml")
        this.oppgaveApi(
            oppgaveMediator,
            oppgaveDTOMapper,
            applicationCallParser,
        )
        statistikkApi(statistikkTjeneste)
        klageApi(klageMediator, klageDTOMapper)
    }
}
