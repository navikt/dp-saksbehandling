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
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.api.models.BehandlingTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.FerdigstillInnsendingRequestDTO
import no.nav.dagpenger.saksbehandling.api.models.InnsendingDTO
import no.nav.dagpenger.saksbehandling.api.models.TynnBehandlingDTO
import no.nav.dagpenger.saksbehandling.api.models.TynnSakDTO
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
                    mediator
                        .hentInnsending(
                            innsendingId = call.behandlingId(),
                            saksbehandler = applicationCallParser.saksbehandler(call),
                        ).let {
                            call.respond(
                                HttpStatusCode.OK,
                                it.tilInnsendingDTO(
                                    mediator.hentLovligeSaker(it.person.ident),
                                ),
                            )
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

private fun Innsending.tilInnsendingDTO(lovligeSaker: List<Sak>): InnsendingDTO =
    InnsendingDTO(
        behandlingId = this.innsendingId,
        journalpostId = this.journalpostId,
        lovligeSaker =
            lovligeSaker.map {
                TynnSakDTO(
                    sakId = it.sakId,
                    opprettetDato = it.opprettet,
                )
            },
        sakId = this.valgtSakId(),
        vurdering = this.vurdering(),
        nyBehandling = this.toBehandling(),
    )

private fun Innsending.toBehandling(): TynnBehandlingDTO? =
    when (val resultat = this.innsendingResultat()) {
        is Innsending.InnsendingResultat.Klage ->
            TynnBehandlingDTO(
                behandlingId = resultat.behandlingId,
                behandlingType = BehandlingTypeDTO.KLAGE,
            )

        is Innsending.InnsendingResultat.RettTilDagpenger ->
            TynnBehandlingDTO(
                behandlingId = resultat.behandlingId,
                behandlingType = BehandlingTypeDTO.RETT_TIL_DAGPENGER,
            )

        else -> null
    }

private fun ApplicationCall.behandlingId(): UUID = this.finnUUID("behandlingId")
