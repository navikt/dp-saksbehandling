package no.nav.dagpenger.saksbehandling.api.config

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.callloging.processingTimeMillis
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.document
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UgyldigTilstandException
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UlovligTilstandsendringException
import no.nav.dagpenger.saksbehandling.api.config.auth.jwt
import no.nav.dagpenger.saksbehandling.api.models.HttpProblemDTO
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.IngenTilgangTilOppgaveException
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import java.net.URI
import java.time.format.DateTimeParseException

private val sikkerLogger = KotlinLogging.logger("tjenestekall")
private val logger = KotlinLogging.logger {}

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
        format { call ->
            val status = call.response.status()?.value ?: "Unhandled"
            val method = call.request.httpMethod.value
            val path = call.request.path()
            val duration = call.processingTimeMillis()
            val queryParams = call.request.queryParameters.entries()
            "$status $method $path $queryParams $duration ms"
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

                is UgyldigTilstandException -> {
                    val problem =
                        HttpProblemDTO(
                            title = "Ugyldig oppgavetilstand",
                            detail = cause.message,
                            status = HttpStatusCode.InternalServerError.value,
                            instance = call.request.path(),
                            type =
                                URI.create("dagpenger.nav.no/saksbehandling:problem:ugyldig-oppgavetilstand")
                                    .toString(),
                        )
                    call.respond(HttpStatusCode.InternalServerError, problem)
                }

                is UlovligTilstandsendringException -> {
                    val problem =
                        HttpProblemDTO(
                            title = "Ulovlig tilstandsendring på oppgave",
                            detail = cause.message,
                            status = HttpStatusCode.Conflict.value,
                            instance = call.request.path(),
                            type =
                                URI.create("dagpenger.nav.no/saksbehandling:problem:oppgave-ulovlig-tilstandsendring")
                                    .toString(),
                        )
                    call.respond(HttpStatusCode.Conflict, problem)
                }

                is IllegalArgumentException -> {
                    val problem =
                        HttpProblemDTO(
                            title = "Ugyldig verdi",
                            detail = cause.message,
                            status = HttpStatusCode.BadRequest.value,
                            instance = call.request.path(),
                            type =
                                URI.create("dagpenger.nav.no/saksbehandling:problem:ugyldig-verdi")
                                    .toString(),
                        )
                    call.respond(HttpStatusCode.BadRequest, problem)
                }

                is DateTimeParseException -> {
                    val problem =
                        HttpProblemDTO(
                            title = "Dato/tid feil",
                            detail = cause.message,
                            status = HttpStatusCode.BadRequest.value,
                            instance = call.request.path(),
                            type =
                                URI.create("dagpenger.nav.no/saksbehandling:problem:dato-tid-feil")
                                    .toString(),
                        )
                    call.respond(HttpStatusCode.BadRequest, problem)
                }

                is IngenTilgangTilOppgaveException -> {
                    val problem =
                        HttpProblemDTO(
                            title = "Ingen tilgang til oppgave",
                            detail = cause.message,
                            status = HttpStatusCode.Forbidden.value,
                            instance = call.request.path(),
                            type =
                                URI.create("dagpenger.nav.no/saksbehandling:problem:ingen-tilgang-til-oppgave:${cause.type}")
                                    .toString(),
                        )
                    call.respond(HttpStatusCode.Forbidden, problem)
                }

                else -> {
                    logger.error(cause) { "Uhåndtert feil: Se sikkerlogg for detaljer" }
                    sikkerLogger.error(cause) { "Uhåndtert feil: ${cause.message}" }
                    val problem =
                        HttpProblemDTO(
                            title = "Uhåndtert feil",
                            detail = cause.message,
                            status = HttpStatusCode.InternalServerError.value,
                            instance = call.request.path(),
                            type =
                                URI.create("dagpenger.nav.no/saksbehandling:problem:uhåndtert-feil")
                                    .toString(),
                        )
                    call.respond(HttpStatusCode.InternalServerError, problem)
                }
            }
        }
    }
}
