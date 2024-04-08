package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.api.config.apiConfig
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.SokDTO
import java.util.UUID

internal fun Application.oppgaveApi(mediator: Mediator) {
    val sikkerLogger = KotlinLogging.logger("tjenestekall")

    apiConfig()

    routing {
        swaggerUI(path = "openapi", swaggerFile = "saksbehandling-api.yaml")

        authenticate("azureAd") {
            route("oppgave") {
                get {
                    val oppgaver = mediator.hentOppgaverKlarTilBehandling().tilOppgaverOversiktDTO()
                    sikkerLogger.info { "Alle oppgaver hentes: $oppgaver" }
                    call.respond(status = HttpStatusCode.OK, oppgaver)
                }

                route("sok") {
                    post {
                        val oppgaver = mediator.finnOppgaverFor(call.receive<SokDTO>().fnr).tilOppgaverOversiktDTO()
                        call.respond(status = HttpStatusCode.OK, oppgaver)
                    }
                }

                route("{oppgaveId}") {
                    get {
                        val oppgaveId = call.finnUUID("oppgaveId")
                        val saksbehandlerSignatur = call.request.jwt()
                        val oppdaterOppgaveHendelse = OppdaterOppgaveHendelse(oppgaveId, saksbehandlerSignatur)
                        val oppgaveMedBehandlingResponse = mediator.lagOppgaveDTO(oppdaterOppgaveHendelse)
                        when (oppgaveMedBehandlingResponse) {
                            null -> call.respond(
                                status = HttpStatusCode.NotFound,
                                message = "Fant ingen oppgave med UUID $oppgaveId",
                            )

                            else -> {
                                val message = oppgaveMedBehandlingResponse
                                sikkerLogger.info { "Oppgave $oppgaveId skal gjøres om til OppgaveDTO: $oppgaveMedBehandlingResponse" }
                                sikkerLogger.info { "OppgaveDTO $oppgaveId hentes: $message" }
                                call.respond(HttpStatusCode.OK, message)
                            }
                        }
                    }

                    route("avslag") {
                        put {
                            val oppgaveId = call.finnUUID("oppgaveId")
                            val saksbehandlerSignatur = call.request.jwt()
                            val godkjennBehandlingHendelse =
                                GodkjennBehandlingHendelse(oppgaveId = oppgaveId, saksbehandlerSignatur)

                            mediator.godkjennBehandling(godkjennBehandlingHendelse)
                                .onSuccess { httpStatusCode: Int ->
                                    call.respond(status = HttpStatusCode.fromValue(httpStatusCode), message = "")
                                }
                                .onFailure { e ->
                                    call.respond(
                                        status = HttpStatusCode.NotFound,
                                        message = e.message.toString(),
                                    )
                                }
                        }
                    }

                    route("lukk") {
                        put {
                            val oppgaveId = call.finnUUID("oppgaveId")
                            val saksbehandlerSignatur = call.request.jwt()
                            val avbrytBehandlingHendelse = AvbrytBehandlingHendelse(oppgaveId, saksbehandlerSignatur)
                            mediator.avbrytBehandling(avbrytBehandlingHendelse)
                                .onSuccess { call.respond(HttpStatusCode.NoContent) }
                                .onFailure { e ->
                                    call.respond(
                                        status = HttpStatusCode.NotFound,
                                        message = e.message.toString(),
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}

private fun List<Oppgave>.tilOppgaverOversiktDTO(): List<OppgaveOversiktDTO> {
    return this.map { oppgave -> oppgave.tilOppgaveOvresiktDTO() }
}

private fun Oppgave.Tilstand.Type.tilOppgaveTilstandDTO() =
    when (this) {
        Oppgave.Tilstand.Type.OPPRETTET -> OppgaveTilstandDTO.OPPRETTET
        Oppgave.Tilstand.Type.FERDIG_BEHANDLET -> OppgaveTilstandDTO.FERDIG_BEHANDLET
        Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING -> OppgaveTilstandDTO.KLAR_TIL_BEHANDLING
    }

internal fun Oppgave.tilOppgaveOvresiktDTO(): OppgaveOversiktDTO {
    return OppgaveOversiktDTO(
        oppgaveId = this.oppgaveId,
        personIdent = this.ident,
        behandlingId = this.behandlingId,
        tidspunktOpprettet = this.opprettet,
        emneknagger = this.emneknagger.toList(),
        tilstand = this.tilstand.tilOppgaveTilstandDTO(),
    )
}

internal fun ApplicationCall.finnUUID(pathParam: String): UUID =
    parameters[pathParam]?.let {
        UUID.fromString(it)
    } ?: throw IllegalArgumentException("Kunne ikke finne oppgaveId i path")

internal fun ApplicationRequest.jwt(): String =
    this.parseAuthorizationHeader().let { authHeader ->
        (authHeader as? HttpAuthHeader.Single)?.blob ?: throw IllegalArgumentException("JWT not found")
    }
