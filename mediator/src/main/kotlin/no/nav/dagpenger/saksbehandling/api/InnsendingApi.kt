package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.api.models.BehandlingTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.FerdigstillInnsendingRequestDTO
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.innsending.Aksjon
import no.nav.dagpenger.saksbehandling.innsending.Aksjon.Avslutt
import no.nav.dagpenger.saksbehandling.innsending.InnsendingMediator
import no.nav.dagpenger.saksbehandling.jwt.ApplicationCallParser
import no.nav.dagpenger.saksbehandling.jwt.jwt
import java.util.UUID

fun Route.innsendingApi(
    mediator: InnsendingMediator,
    applicationCallParser: ApplicationCallParser,
) {
    route("innsending") {
        authenticate("azureAd") {
            route("{behandlingId}") {
                get {
                    mediator.hentInnsending(
                        innsendingId = call.behandlingId(),
                        saksbehandler = applicationCallParser.saksbehandler(call),
                    )
                }
                route("ferdigstill") {
                    put {
                        val behandlingId = call.behandlingId()

                        call.receive<FerdigstillInnsendingRequestDTO>().let { requestDTO ->
                            val saksBehandlerToken = call.request.jwt()
                            val aksjon =
                                when (requestDTO.behandlingType) {
                                    null -> Avslutt
                                    BehandlingTypeDTO.RETT_TIL_DAGPENGER ->
                                        Aksjon.OpprettManuellBehandling(
                                            saksbehandlerToken = saksBehandlerToken,
                                        )

                                    BehandlingTypeDTO.KLAGE -> Aksjon.OpprettKlage(requestDTO.sakId)
                                    else -> throw IllegalArgumentException("Ugyldig behandling type")
                                }

                            mediator.ferdigstill(
                                hendelse =
                                    FerdigstillInnsendingHendelse(
                                        innsendingId = behandlingId,
                                        aksjon = aksjon,
                                        utførtAv = applicationCallParser.saksbehandler(call),
                                    ),
                            )
                            call.respond(status = HttpStatusCode.NoContent, message = "Innsending ferdigstilt")
                        }
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.behandlingId(): UUID {
    return this.finnUUID("behandlingId")
}
