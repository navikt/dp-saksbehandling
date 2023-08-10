package no.nav.dagpenger.behandling.api

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
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.behandling.ManuellSporing
import no.nav.dagpenger.behandling.Mediator
import no.nav.dagpenger.behandling.api.auth.AzureAd
import no.nav.dagpenger.behandling.api.auth.verifier
import no.nav.dagpenger.behandling.api.models.NyTilstandDTO
import no.nav.dagpenger.behandling.api.models.NyttSvarDTO
import no.nav.dagpenger.behandling.api.models.SokDTO
import no.nav.dagpenger.behandling.api.models.SvartypeDTO
import no.nav.dagpenger.behandling.dto.toOppgaveDTO
import no.nav.dagpenger.behandling.dto.toOppgaverDTO
import no.nav.dagpenger.behandling.hendelser.StegUtført
import no.nav.dagpenger.behandling.oppgave.Saksbehandler
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun Application.oppgaveApi(mediator: Mediator) {
    install(CallLogging) {
        disableDefaultColors()
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            enable(SerializationFeature.INDENT_OUTPUT)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }

    install(Authentication) {
        jwt("azureAd") {
            verifier(AzureAd)
            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }
    }

    routing {
        swaggerUI(path = "openapi", swaggerFile = "behandling-api.yaml")

        authenticate("azureAd") {
            route("oppgave") {
                get {
                    val oppgaver = mediator.hentOppgaver().toOppgaverDTO()
                    call.respond(HttpStatusCode.OK, oppgaver)
                }

                route("sok") {
                    post {
                        val fnrDTO = call.receive<SokDTO>()
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
                            val svar: NyttSvarDTO = call.receive()

                            val sporing = ManuellSporing(
                                LocalDateTime.now(),
                                Saksbehandler(call.saksbehandlerId()),
                                svar.begrunnelse.tekst,
                            )

                            mediator.behandle(StegUtført("123", oppgaveId)) {
                                when (svar.type) {
                                    SvartypeDTO.String -> besvar(stegId, svar.svar, sporing)
                                    SvartypeDTO.LocalDate -> besvar(stegId, LocalDate.parse(svar.svar), sporing)
                                    SvartypeDTO.Int -> besvar(stegId, svar.svar.toInt(), sporing)
                                    SvartypeDTO.Boolean -> besvar(stegId, svar.svar.toBoolean(), sporing)
                                    SvartypeDTO.Double -> besvar(stegId, svar.svar.toDouble(), sporing)
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
}

internal fun ApplicationCall.finnUUID(pathParam: String): UUID = parameters[pathParam]?.let {
    UUID.fromString(it)
} ?: throw IllegalArgumentException("Kunne ikke finne oppgaveId i path")

internal fun ApplicationCall.saksbehandlerId() =
    this.authentication.principal<JWTPrincipal>()?.payload?.claims?.get("NAVident")?.asString()
        ?: throw IllegalArgumentException("Ikke autentisert")
