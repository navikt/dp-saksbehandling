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
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.api.models.OppdaterKlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.OpprettKlageDTO
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageFerdigbehandletHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellKlageMottattHendelse
import no.nav.dagpenger.saksbehandling.jwt.ApplicationCallParser
import no.nav.dagpenger.saksbehandling.jwt.jwt

fun Route.klageApi(
    mediator: KlageMediator,
    klageDtoMapper: KlageDTOMapper,
    applicationCallParser: ApplicationCallParser,
) {
    authenticate("azureAd-maskin") {
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
        route("klage/opprett-manuelt") {
            post {
                val klage: OpprettKlageDTO = call.receive<OpprettKlageDTO>()
                val saksbehandler = applicationCallParser.saksbehandler(this.call)
                mediator.opprettManuellKlage(
                    manuellKlageMottattHendelse =
                        ManuellKlageMottattHendelse(
                            opprettet = klage.opprettet,
                            journalpostId = klage.journalpostId,
                            utførtAv = saksbehandler,
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
                    val saksbehandler = applicationCallParser.saksbehandler(call)
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
                        val saksbehandler = applicationCallParser.saksbehandler(call)
                        mediator.avbrytKlage(
                            hendelse =
                                AvbruttHendelse(
                                    behandlingId = behandlingId,
                                    utførtAv = saksbehandler,
                                ),
                        )
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
                route("ferdigstill") {
                    put {
                        val behandlingId = call.finnUUID("behandlingId")
                        val saksbehandler = applicationCallParser.saksbehandler(call)
                        mediator.ferdigstill(
                            hendelse =
                                KlageFerdigbehandletHendelse(
                                    behandlingId = behandlingId,
                                    utførtAv = saksbehandler,
                                ),
                            saksbehandlerToken = call.request.jwt(),
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
                            val saksbehandler = applicationCallParser.saksbehandler(call)
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
