package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.api.KlageDtoMapper.tilDto
import no.nav.dagpenger.saksbehandling.api.KlageDtoMapper.tilVerdi
import no.nav.dagpenger.saksbehandling.api.models.OppdaterKlageOpplysningDTO

fun Route.klageApi(mediator: KlageMediator) {
    val applicationCallParser = Configuration.applicationCallParser
    authenticate("azureAd") {
        route("oppgave/klage") {
            route("{klageId}") {
                get {
                    val klageId = call.finnUUID("klageId")
                    val klageBehandling = mediator.hentKlageBehandling(klageId)
                    val klageDTO = klageBehandling.tilDto()
                    call.respond(HttpStatusCode.OK, klageDTO)
                }
                route("trekk") {
                    put {
                        val klageId = call.finnUUID("klageId")
                        val saksbehandler = applicationCallParser.sakbehandler(call)
                        mediator.avbrytKlage(
                            klageId = klageId,
                            saksbehandler = saksbehandler,
                        )
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
                route("ferdigstill") {
                    put {
                        val klageId = call.finnUUID("klageId")
                        val saksbehandler = applicationCallParser.sakbehandler(call)
                        mediator.ferdigstill(
                            klageId = klageId,
                            saksbehandler = saksbehandler,
                        )
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
                route("opplysning") {
                    route("{opplysningId}") {
                        put {
                            val klageId = call.finnUUID("klageId")
                            val opplysningId = call.finnUUID("opplysningId")
                            val oppdaterKlageOpplysningDTO = call.receive<OppdaterKlageOpplysningDTO>()
                            val saksbehandler = applicationCallParser.sakbehandler(call)
                            mediator.oppdaterKlageOpplysning(
                                behandlingId = klageId,
                                opplysningId = opplysningId,
                                verdi = oppdaterKlageOpplysningDTO.tilVerdi(),
                                saksbehandler = saksbehandler,
                            )
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
        }
    }
}
