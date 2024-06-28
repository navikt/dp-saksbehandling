package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

internal fun Application.utsendingApi(utsendingMediator: UtsendingMediator) {
    routing {
        authenticate("azureAd") {
            route("/utsending/{oppgaveId}/send-brev") {
                post {
                    val brevBody = call.receiveText()
                    try {
                        if (!htmlContentType) throw UgyldigContentType("Kun støtte for HTML")

                        val oppgaveId = call.finnUUID("oppgaveId")

                        val vedtaksbrevHendelse = VedtaksbrevHendelse(oppgaveId, brevBody)
                        utsendingMediator.mottaBrev(vedtaksbrevHendelse)
                        call.respond(HttpStatusCode.Accepted)
                    } catch (e: Exception) {
                        val feilmelding = "Feil ved mottak av brev: ${e.message}"
                        logger.error(e) { feilmelding }
                        sikkerlogger.error(e) { "$feilmelding for $brevBody" }
                    }
                }
            }
        }
    }
}

class UgyldigContentType(message: String) : RuntimeException(message)

private val PipelineContext<Unit, ApplicationCall>.htmlContentType: Boolean
    get() = call.request.contentType().match(ContentType.Text.Html)
