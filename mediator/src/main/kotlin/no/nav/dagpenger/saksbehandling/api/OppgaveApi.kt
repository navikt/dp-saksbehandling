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
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Steg
import no.nav.dagpenger.saksbehandling.api.config.apiConfig
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.StegDTO
import no.nav.dagpenger.saksbehandling.api.models.StegTilstandDTO
import no.nav.dagpenger.saksbehandling.maskinell.BehandlingKlient
import java.util.UUID

internal fun Application.oppgaveApi(
    mediator: Mediator,
    behandlingKlient: BehandlingKlient,
) {
    apiConfig()

    val sikkerLogger = KotlinLogging.logger("tjenestekall")

    routing {
        swaggerUI(path = "openapi", swaggerFile = "saksbehandling-api.yaml")

        authenticate("azureAd") {
            route("oppgave") {
                get {
                    val oppgaver = mediator.hentAlleOppgaver().tilOppgaverDTO() + oppgaveDtos
                    call.respond(status = HttpStatusCode.OK, oppgaver)
                }

                route("sok") {
                    post {
                        val oppgaver = oppgaveDtos
                        call.respond(status = HttpStatusCode.OK, oppgaver)
                    }
                }

                route("{oppgaveId}") {
                    get {
                        val oppgaveId = call.finnUUID("oppgaveId")
                        val saksbehandlerSignatur = call.request.jwt()
                        val oppdaterOppgaveHendelse = OppdaterOppgaveHendelse(oppgaveId, saksbehandlerSignatur)
                        val oppgave = mediator.oppdaterOppgaveMedSteg(oppdaterOppgaveHendelse)
                        when (oppgave) {
                            null ->
                                call.respond(
                                    status = HttpStatusCode.NotFound,
                                    message = "Fant ingen oppgave med UUID $oppgaveId",
                                )

                            else -> call.respond(HttpStatusCode.OK, oppgave.tilOppgaveDTO())
                        }
                    }

                    put {
                        call.respond(HttpStatusCode.NoContent)
                    }

                    route("avslag") {
                        put {
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }

                    route("lukk") {
                        put {
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
        }
    }
}

private fun List<Oppgave>.tilOppgaverDTO(): List<OppgaveDTO> {
    return this.map { oppgave -> oppgave.tilOppgaveDTO() }
}

internal fun Oppgave.tilOppgaveDTO(): OppgaveDTO {
    return OppgaveDTO(
        oppgaveId = this.oppgaveId,
        personIdent = this.ident,
        behandlingId = this.behandlingId,
        datoOpprettet = this.opprettet.toLocalDate(),
        journalpostIder = emptyList(),
        emneknagger = this.emneknagger.toList(),
        // @TODO: Hent tilstand fra oppgave? (FerdigBehandlet, TilBehandling)
        tilstand = OppgaveTilstandDTO.TilBehandling,
        steg = this.steg.map { steg -> steg.tilStegDTO() },
    )
}

private fun Steg.tilStegDTO(): StegDTO {
    return StegDTO(
        stegNavn = this.navn,
        opplysninger = emptyList(),
        // @TODO: Hent stegtilstand fra steg?
        tilstand = StegTilstandDTO.Groenn,
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
