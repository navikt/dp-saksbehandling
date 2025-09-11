package no.nav.dagpenger.saksbehandling.api

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.saksbehandling.Oppgave.AlleredeTildeltException
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.ManglendeTilgang
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UgyldigTilstandException
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UlovligTilstandsendringException
import no.nav.dagpenger.saksbehandling.api.models.HttpProblemDTO
import no.nav.dagpenger.saksbehandling.behandling.BehandlingException
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKreverIkkeTotrinnskontrollException
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import java.net.URI
import java.time.format.DateTimeParseException

private val sikkerLogger = KotlinLogging.logger("tjenestekall")
private val logger = KotlinLogging.logger {}
private val apiFeilCounter: Counter =
    Counter.builder()
        .name("dp_saksbehandling_oppgave_api_feil")
        .help("Antall feil kastet i oppgave APIet")
        .labelNames("feiltype")
        .register(PrometheusRegistry.defaultRegistry)

fun Application.statusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            apiFeilCounter.labelValues(cause.javaClass.simpleName).inc()
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

                is AlleredeTildeltException -> {
                    val problem =
                        HttpProblemDTO(
                            title = "Oppgaven eies av en annen. Operasjon kan ikke utføres.",
                            detail = cause.message,
                            status = HttpStatusCode.Conflict.value,
                            instance = call.request.path(),
                            type =
                                URI.create("dagpenger.nav.no/saksbehandling:problem:oppgave-eies-av-annen-behandler")
                                    .toString(),
                        )
                    call.respond(HttpStatusCode.Conflict, problem)
                }

                is BehandlingException -> {
                    val behandlingException: BehandlingException = cause
                    val problem =
                        behandlingException.text?.let {
                            try {
                                objectMapper.readValue<HttpProblemDTO>(it)
                            } catch (e: Exception) {
                                logger.warn { "Fikk ikke parset til HttpProblemDTO: $it" }
                                null
                            }
                        } ?: HttpProblemDTO(
                            title = "Feil ved kall mot dp-behandling",
                            detail = behandlingException.text,
                            status = behandlingException.status,
                            instance = call.request.path(),
                            type =
                                URI.create("dagpenger.nav.no/saksbehandling:problem:behandling-feil")
                                    .toString(),
                        )
                    when (behandlingException.status) {
                        in 400 until 499 -> {
                            logger.warn { "Behandling feilet med klientfeil: ${behandlingException.text} med kode ${behandlingException.status}" }
                        }
                        in 500 until 599 -> {
                            logger.error { "Behandling feilet med serverfeil: ${behandlingException.text} med kode ${behandlingException.status}" }
                        }
                        !in HttpStatusCode.allStatusCodes.map { it.value } -> {
                            logger.error { "Behandling feilet med ukjent statuskode: ${behandlingException.text} med kode ${behandlingException.status}" }
                        }
                    }
                    call.respond(HttpStatusCode.fromValue(behandlingException.status), problem)
                }

                is ManglendeTilgang -> {
                    val problem =
                        HttpProblemDTO(
                            title = cause.javaClass.simpleName,
                            detail = cause.message,
                            status = HttpStatusCode.Forbidden.value,
                            instance = call.request.path(),
                            type =
                                URI.create("dagpenger.nav.no/saksbehandling:problem:manglende-tilgang")
                                    .toString(),
                        )
                    call.respond(HttpStatusCode.Forbidden, problem)
                }

                is BehandlingKreverIkkeTotrinnskontrollException -> {
                    val problem =
                        HttpProblemDTO(
                            title = "Behandling krever ikke totrinnskontroll",
                            detail = cause.message,
                            status = HttpStatusCode.Conflict.value,
                            instance = call.request.path(),
                            type =
                                URI.create("dagpenger.nav.no/saksbehandling:problem:behandling-krever-ikke-totrinnskontroll")
                                    .toString(),
                        )
                    call.respond(HttpStatusCode.Conflict, problem)
                }

                is MeldingOmVedtakKlient.KanIkkeLageMeldingOmVedtak -> {
                    val problem =
                        HttpProblemDTO(
                            title = "Feil ved laging av melding om vedtak",
                            detail = cause.message,
                            status = HttpStatusCode.InternalServerError.value,
                            instance = call.request.path(),
                            type =
                                URI.create("dagpenger.nav.no/saksbehandling:problem:feil-lag-melding-om-vedtak")
                                    .toString(),
                        )
                    call.respond(HttpStatusCode.InternalServerError, problem)
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
