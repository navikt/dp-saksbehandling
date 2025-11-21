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
import no.nav.dagpenger.saksbehandling.api.models.BehandlingDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlingTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.FerdigstillInnsendingRequestDTO
import no.nav.dagpenger.saksbehandling.api.models.InnsendingDTO
import no.nav.dagpenger.saksbehandling.api.models.UtlostAvTypeDTO
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.innsending.Aksjon
import no.nav.dagpenger.saksbehandling.innsending.Aksjon.Avslutt
import no.nav.dagpenger.saksbehandling.innsending.Innsending
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
                    ).let {
                        call.respond(HttpStatusCode.OK, it.tilInnsendingDTO())
                    }
                }
                route("ferdigstill") {
                    put {
                        val behandlingId = call.behandlingId()

                        call.receive<FerdigstillInnsendingRequestDTO>().let { requestDTO ->
                            val saksBehandlerToken = call.request.jwt()
                            val aksjon =
                                when (requestDTO.behandlingType) {
                                    null -> Avslutt(requestDTO.sakId)
                                    BehandlingTypeDTO.RETT_TIL_DAGPENGER -> {
                                        val valgtSakId = requestDTO.sakId
                                        requireNotNull(valgtSakId)
                                        Aksjon.OpprettManuellBehandling(
                                            saksbehandlerToken = saksBehandlerToken,
                                            valgtSakId = valgtSakId,
                                        )
                                    }
                                    BehandlingTypeDTO.KLAGE -> {
                                        val valgtSakId = requestDTO.sakId
                                        requireNotNull(valgtSakId)
                                        Aksjon.OpprettKlage(valgtSakId)
                                    }

                                    else -> throw IllegalArgumentException("Ugyldig behandling type")
                                }
                            mediator.ferdigstill(
                                hendelse =
                                    FerdigstillInnsendingHendelse(
                                        innsendingId = behandlingId,
                                        aksjon = aksjon,
                                        vurdering = requestDTO.vurdering,
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

private fun Innsending.tilInnsendingDTO(): InnsendingDTO {
    return InnsendingDTO(
        behandlingId = this.innsendingId,
        journalpostId = this.journalpostId,
        // Hent alle saker for en person
        lovligeSaker = emptyList(),
        // sakbehandler har valgt denne saken og burde hentes fra InnsendingResultat
        sakId = null,
        vurdering = this.vurdering(),
        nyBehandling = this.toBehandling(),
    )
}

private fun Innsending.toBehandling(): BehandlingDTO? {
    return when (val resultat = this.innsendingResultat()) {
        is Innsending.InnsendingResultat.Klage ->
            BehandlingDTO(
                id = resultat.behandlingId,
                behandlingType = BehandlingTypeDTO.KLAGE,
                utlostAv = UtlostAvTypeDTO.INNSENDING,
                opprettet = this.mottatt,
                // kan gi på klage
                oppgaveId = null,
            )

        is Innsending.InnsendingResultat.RettTilDagpenger ->
            BehandlingDTO(
                id = resultat.behandlingId,
                behandlingType = BehandlingTypeDTO.RETT_TIL_DAGPENGER,
                utlostAv = UtlostAvTypeDTO.INNSENDING,
                opprettet = this.mottatt,
                oppgaveId = null,
            )

        else -> null
    }
}

private fun ApplicationCall.behandlingId(): UUID {
    return this.finnUUID("behandlingId")
}
