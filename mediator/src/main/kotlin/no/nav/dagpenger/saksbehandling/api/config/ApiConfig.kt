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
import io.ktor.server.response.respond
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.api.config.auth.jwt
import no.nav.dagpenger.saksbehandling.db.DataNotFoundException
import java.time.format.DateTimeParseException

private val logger = KotlinLogging.logger { }
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
                is DataNotFoundException -> call.respond(HttpStatusCode.NotFound) { cause.message.toString() }
                is IllegalArgumentException -> call.respond(HttpStatusCode.BadRequest)
                is DateTimeParseException -> call.respond(HttpStatusCode.BadRequest) { cause.message.toString() }
                else -> {
                    sikkerLogger.error(cause) { "Uhåndtert feil: ${cause.message}" }
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }
    }
}
