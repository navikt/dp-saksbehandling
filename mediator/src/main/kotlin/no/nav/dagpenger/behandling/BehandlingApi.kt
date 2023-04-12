package no.nav.dagpenger.behandling

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.behandling.dto.BehandlingDTO
import no.nav.dagpenger.behandling.dto.FnrDTO
import no.nav.dagpenger.behandling.dto.SvarDTO
import no.nav.dagpenger.behandling.dto.SvartypeDTO
import no.nav.dagpenger.behandling.dto.toBehandlingDTO
import no.nav.dagpenger.behandling.dto.toBehandlingerDTO
import no.nav.dagpenger.behandling.hendelser.BehandlingSvar
import java.time.LocalDate
import java.util.*
import kotlin.NoSuchElementException

fun Application.behandlingApi(mediator: Mediator) {
    install(CallLogging) { }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            enable(SerializationFeature.INDENT_OUTPUT)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }

    routing {
        route("behandlinger") {
            get {
                val behandlinger = mediator.hentBehandlinger().toBehandlingerDTO()
                call.respond(HttpStatusCode.OK, behandlinger)
            }

            route("sok") {
                post {
                    val fnrDTO = call.receive<FnrDTO>()

                    try {
                        val behandling = mediator.hentBehandlingerFor(fnrDTO.fnr).toBehandlingerDTO()
                        call.respond(HttpStatusCode.OK, behandling)
                    } catch (e: NoSuchElementException) {
                        call.respond(
                            status = HttpStatusCode.NotFound,
                            message = "Fant ingen behandlinger for gitt fnr.",
                        )
                    }
                }
            }

            route("{behandlingId}") {
                get() {
                    val behandlingId = call.finnUUID("behandlingId")

                    try {
                        val behandling: BehandlingDTO =
                            mediator.hentBehandling(behandlingId).toBehandlingDTO()

                        call.respond(
                            HttpStatusCode.OK,
                            behandling,
                        )
                    } catch (e: NoSuchElementException) {
                        call.respond(
                            status = HttpStatusCode.NotFound,
                            message = "Fant ingen behandling med UUID $behandlingId",
                        )
                    }
                }

                route("steg") {
                    put("{stegId}") {
                        val behandlingId = call.finnUUID("behandlingId")
                        val stegId = call.finnUUID("stegId")
                        val svar: SvarDTO = call.receive()

                        require(svar.svar != null)

                        when (svar.type) {
                            SvartypeDTO.String -> mediator.behandle(
                                BehandlingSvar(
                                    "123",
                                    behandlingId,
                                    stegId,
                                    svar.svar,
                                ),
                            )

                            SvartypeDTO.LocalDate -> mediator.behandle(
                                BehandlingSvar(
                                    "123",
                                    behandlingId,
                                    stegId,
                                    LocalDate.parse(svar.svar),
                                ),
                            )

                            SvartypeDTO.Int -> mediator.behandle(
                                BehandlingSvar(
                                    "123",
                                    behandlingId,
                                    stegId,
                                    svar.svar.toInt(),
                                ),
                            )

                            SvartypeDTO.Boolean -> mediator.behandle(
                                BehandlingSvar(
                                    "123",
                                    behandlingId,
                                    stegId,
                                    svar.svar.toBoolean(),
                                ),
                            )
                        }
                        call.respond(status = HttpStatusCode.OK, message = "")
                    }
                }
            }
        }
    }
}

internal fun ApplicationCall.finnUUID(pathParam: String): UUID = parameters[pathParam]?.let {
    UUID.fromString(it)
} ?: throw IllegalArgumentException("Kunne ikke finne behandlingId i path")
