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
import no.nav.dagpenger.behandling.dto.FnrDTO
import no.nav.dagpenger.behandling.dto.NyTilstandDTO
import no.nav.dagpenger.behandling.dto.SvarDTO
import no.nav.dagpenger.behandling.dto.SvartypeDTO
import no.nav.dagpenger.behandling.dto.toOppgaveDTO
import no.nav.dagpenger.behandling.dto.toOppgaverDTO
import no.nav.dagpenger.behandling.hendelser.StegUtført
import java.time.LocalDate
import java.util.UUID

fun Application.oppgaveApi(mediator: Mediator) {
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
        route("oppgave") {
            get {
                val oppgaver = mediator.hentOppgaver().toOppgaverDTO()
                call.respond(HttpStatusCode.OK, oppgaver)
            }

            route("sok") {
                post {
                    val fnrDTO = call.receive<FnrDTO>()
                    val oppgave = mediator.hentOppgaverFor(fnrDTO.fnr).toOppgaverDTO()
                    call.respond(HttpStatusCode.OK, oppgave)
                }
            }

            route("{oppgaveId}") {
                get {
                    val oppgaveId = call.finnUUID("oppgaveId")

                    try {
                        val oppgave = mediator.hentOppgave(oppgaveId).toOppgaveDTO()

                        call.respond(HttpStatusCode.OK, oppgave)
                    } catch (e: NoSuchElementException) {
                        call.respond(
                            status = HttpStatusCode.NotFound,
                            message = "Fant ingen oppgave med UUID $oppgaveId",
                        )
                    }
                }

                route("steg") {
                    put("{stegId}") {
                        val oppgaveId = call.finnUUID("oppgaveId")
                        val stegId = call.finnUUID("stegId")
                        val svar: SvarDTO = call.receive()

                        require(svar.svar != null)

                        mediator.behandle(StegUtført("123", oppgaveId)) {
                            when (svar.type) {
                                SvartypeDTO.String -> besvar(stegId, svar.svar)
                                SvartypeDTO.LocalDate -> besvar(stegId, LocalDate.parse(svar.svar))
                                SvartypeDTO.Int -> besvar(stegId, svar.svar.toInt())
                                SvartypeDTO.Boolean -> besvar(stegId, svar.svar.toBoolean())
                                SvartypeDTO.Double -> besvar(stegId, svar.svar.toDouble())
                            }
                        }
                        call.respond(status = HttpStatusCode.OK, message = "")
                    }
                }

                route("tilstand") {
                    post {
                        val oppgaveId = call.finnUUID("oppgaveId")
                        val nyTilstandDTO = call.receive<NyTilstandDTO>()

                        try {
                            mediator.hentOppgave(oppgaveId).gåTil(nyTilstandDTO.nyTilstand)
                            call.respond(HttpStatusCode.OK, "")
                        } catch (e: NoSuchElementException) {
                            call.respond(
                                status = HttpStatusCode.NotFound,
                                message = "Fant ingen oppgave med UUID $oppgaveId",
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun ApplicationCall.finnUUID(pathParam: String): UUID = parameters[pathParam]?.let {
    UUID.fromString(it)
} ?: throw IllegalArgumentException("Kunne ikke finne oppgaveId i path")
