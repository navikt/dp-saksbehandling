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
import no.nav.dagpenger.saksbehandling.api.models.OppdaterKlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.OpprettKlageDTO
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse

fun Route.klageApi(
    mediator: KlageMediator,
    klageDtoMapper: KlageDTOMapper,
) {
    val applicationCallParser = Configuration.applicationCallParser
    authenticate("azureAd", "azureAd-maskin") {
        route("klage/opprett") {
            post {
                val klage: OpprettKlageDTO = call.receive<OpprettKlageDTO>()
                mediator.opprettKlage(
                    klageMottattHendelse =
                        KlageMottattHendelse(
                            opprettet = klage.opprettet,
                            journalpostId = klage.journalpostId,
                            utførtAv = Applikasjon("dp-mottak"),
                            ident = klage.personIdent.ident,
                        ),
                ).let { oppgave ->

                    call.respond(HttpStatusCode.Created, oppgave.tilOppgaveOversiktDTO())
                }
            }
        }
    }

    authenticate("azureAd") {
        route("klage") {
            route("{behandlingId}") {
                get {
                    val behandlingId = call.finnUUID("behandlingId")
                    val saksbehandler = applicationCallParser.sakbehandler(call)
                    val klageBehandling =
                        mediator.hentKlageBehandling(
                            behandlingId = behandlingId,
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
                        val behandlingId = call.finnUUID("behandlingId")
                        val saksbehandler = applicationCallParser.sakbehandler(call)
                        mediator.avbrytKlage(
                            behandlingId = behandlingId,
                            saksbehandler = saksbehandler,
                        )
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
                route("ferdigstill") {
                    put {
                        val behandlingId = call.finnUUID("behandlingId")
                        val saksbehandler = applicationCallParser.sakbehandler(call)
                        mediator.ferdigstill(
                            behandlingId = behandlingId,
                            saksbehandler = saksbehandler,
                        )
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
                route("opplysning") {
                    route("{opplysningId}") {
                        put {
                            val behandlingId = call.finnUUID("behandlingId")
                            val opplysningId = call.finnUUID("opplysningId")
                            val oppdaterKlageOpplysningDTO = call.receive<OppdaterKlageOpplysningDTO>()
                            val saksbehandler = applicationCallParser.sakbehandler(call)
                            mediator.oppdaterKlageOpplysning(
                                behandlingId = behandlingId,
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
