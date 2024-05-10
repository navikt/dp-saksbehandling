package no.nav.dagpenger.saksbehandling.api.config

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.document
import io.ktor.server.request.path
import io.ktor.server.response.respond
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UkjentTilstandException
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UlovligTilstandsendringException
import no.nav.dagpenger.saksbehandling.api.config.auth.jwt
import no.nav.dagpenger.saksbehandling.api.models.HttpProblemDTO
import no.nav.dagpenger.saksbehandling.db.DataNotFoundException
import java.net.URI
import java.time.format.DateTimeParseException

private val sikkerLogger = KotlinLogging.logger("tjenestekall")

fun Application.apiConfig() {
    install(CallLogging) {
        disableDefaultColors()
        filter { call ->
            !setOf(
                "isalive",
                "isready",
                "metrics",
            ).contains(call.request.document())
        }
    }

    install(ContentNegotiation) {
        jackson {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
    }

    install(Authentication) {
        jwt("azureAd")
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is DataNotFoundException -> {
                    val problem =
                        HttpProblemDTO(
                            title = "Ressurs ikke funnet",
                            detail = cause.message,
                            status = HttpStatusCode.NotFound.value,
                            instance = call.request.path(),
                            type = URI.create("dagpenger.nav.no/saksbehandling:problem:ressurs-ikke-funnet").toString(),
                        )
                    call.respond(HttpStatusCode.NotFound, problem)
                }

                is IllegalArgumentException -> call.respond(HttpStatusCode.BadRequest)
                is DateTimeParseException -> call.respond(HttpStatusCode.BadRequest) { cause.message.toString() }
                is UkjentTilstandException -> call.respond(HttpStatusCode.InternalServerError) { cause.message.toString() }
                is UlovligTilstandsendringException -> call.respond(HttpStatusCode.Conflict) { cause.message.toString() }
                else -> {
                    sikkerLogger.error(cause) { "Uhåndtert feil: ${cause.message}" }
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }
    }
}
