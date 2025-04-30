package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.models.OppdaterKlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonIdentDTO
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import java.time.LocalDateTime

fun Route.klageApi(
    mediator: KlageMediator,
    klageDtoMapper: KlageDTOMapper,
) {
    val applicationCallParser = Configuration.applicationCallParser

    route("klage/opprett") {
        post {
            mediator.opprettKlage(
                klageMottattHendelse =
                    KlageMottattHendelse(
                        opprettet = LocalDateTime.now(),
                        journalpostId = UUIDv7.ny().toString(),
                        utførtAv = Applikasjon("dp-mottak"),
                        ident = call.receive<PersonIdentDTO>().ident,
                    ),
            ).let {
                call.respond(HttpStatusCode.Created, it)
            }
        }
    }

    authenticate("azureAd") {
        route("klage") {
            route("{klageId}") {
                get {
                    val klageId = call.finnUUID("klageId")
                    val saksbehandler = applicationCallParser.sakbehandler(call)
                    val klageBehandling =
                        mediator.hentKlageBehandling(
                            behandlingId = klageId,
                            saksbehandler = saksbehandler,
                        )
                    val klageDTO =
                        klageDtoMapper.tilDto(
                            klageBehandling = klageBehandling,
                            saksbehandler = saksbehandler,
                        )
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
                                verdi = klageDtoMapper.tilVerdi(oppdaterKlageOpplysningDTO),
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
