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
import no.nav.dagpenger.saksbehandling.UtsendingMediator
import no.nav.dagpenger.saksbehandling.api.config.apiConfig
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse

internal fun Application.utsendingApi(utsendingMediator: UtsendingMediator) {
    apiConfig()

    routing {
        authenticate("azureAd") {
            route("/utsending/{oppgaveId}/send-brev") {
                post {
                    if (!htmlContentType) throw UgyldigContentType("Kun støtte for HTML")

                    val oppgaveId = call.finnUUID("oppgaveId")

                    val brevHtml = call.receiveText()
                    val vedtaksbrevHendelse = VedtaksbrevHendelse(oppgaveId, brevHtml)
                    utsendingMediator.mottaBrev(vedtaksbrevHendelse)
                    call.respond(HttpStatusCode.Accepted)
                }
            }
        }
    }
}

class UgyldigContentType(message: String) : RuntimeException(message)

private val PipelineContext<Unit, ApplicationCall>.htmlContentType: Boolean
    get() = call.request.contentType().match(ContentType.Text.Html)
