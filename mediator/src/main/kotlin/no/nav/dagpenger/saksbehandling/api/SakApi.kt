package no.nav.dagpenger.saksbehandling.api

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.api.models.PersonIdentDTO
import no.nav.dagpenger.saksbehandling.api.models.SakIdDTO
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

private val logger = KotlinLogging.logger { }

fun Route.sakApi(mediator: SakMediator) {
    authenticate("azureAd-maskin") {
        route("behandling/{behandlingId}/sakId") {
            get {
                val behandlingId = call.behandlingId()
                mediator.hentDagpengerSakIdForBehandlingId(behandlingId)
                call.respondText { mediator.hentDagpengerSakIdForBehandlingId(behandlingId).toString() }
            }
        }
        route("sak/siste-sak-id/for-ident") {
            post {
                val personIdentDTO: PersonIdentDTO = call.receive<PersonIdentDTO>()
                mediator.finnSisteSakId(personIdentDTO.ident).let { sakId ->
                    when (sakId) {
                        null -> call.respond(message = "", status = HttpStatusCode.NoContent)
                        else -> call.respond(SakIdDTO(id = sakId))
                    }
                }
            }
        }

        route("sak/sak-id-for-soknad/{soknadId}") {
            get {
                val søknadId: UUID = call.søknadId()
                mediator.finnSakIdForSøknad(søknadId).let { sakId ->
                    when (sakId) {
                        null -> call.respond(message = "", status = HttpStatusCode.NoContent)
                        else -> call.respond(SakIdDTO(id = sakId))
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.søknadId(): UUID =
    this.parameters["soknadId"]?.let {
        runCatching {
            UUID.fromString(it)
        }.onFailure { t ->
            logger.error(t) { "Kunne ikke parse soknadId: $it" }
        }.getOrThrow()
    } ?: throw IllegalArgumentException("SøknadId mangler i path")

private fun ApplicationCall.behandlingId(): UUID {
    return this.parameters["behandlingId"]?.let {
        runCatching {
            UUID.fromString(it)
        }.onFailure { t ->
            logger.error(t) { "Kunne ikke parse behandlingId: $it" }
        }.getOrThrow()
    } ?: throw IllegalArgumentException("BehandlingId mangler i path")
}
