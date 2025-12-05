package no.nav.dagpenger.saksbehandling.api

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
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
    }
}

private fun ApplicationCall.behandlingId(): UUID =
    this.parameters["behandlingId"]?.let {
        runCatching {
            UUID.fromString(it)
        }.onFailure { t ->
            logger.error(t) { "Kunne ikke parse behandlingId: $it" }
        }.getOrThrow()
    } ?: throw IllegalArgumentException("BehandlingId mangler i path")
