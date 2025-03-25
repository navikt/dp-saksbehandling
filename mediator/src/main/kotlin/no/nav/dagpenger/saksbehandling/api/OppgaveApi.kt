package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.Emneknagg.PåVent
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.api.models.LagreNotatResponseDTO
import no.nav.dagpenger.saksbehandling.api.models.NesteOppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppdatertTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonIdentDTO
import no.nav.dagpenger.saksbehandling.api.models.SoknadDTO
import no.nav.dagpenger.saksbehandling.api.models.UtsettOppgaveAarsakDTO
import no.nav.dagpenger.saksbehandling.api.models.UtsettOppgaveDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SlettNotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.jwt.ApplicationCallParser
import no.nav.dagpenger.saksbehandling.jwt.jwt
import no.nav.dagpenger.saksbehandling.jwt.navIdent
import java.util.UUID

private val logger = KotlinLogging.logger { }
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

internal fun Application.oppgaveApi(
    oppgaveMediator: OppgaveMediator,
    oppgaveDTOMapper: OppgaveDTOMapper,
    applicationCallParser: ApplicationCallParser,
) {
    routing {
        swaggerUI(path = "openapi", swaggerFile = "saksbehandling-api.yaml")
        authenticate("azureAd") {
            route("person/oppgaver") {
                post {
                    val oppgaver =
                        oppgaveMediator.finnOppgaverFor(call.receive<PersonIdentDTO>().ident)
                            .tilOppgaveOversiktDTOListe()
                    call.respond(status = HttpStatusCode.OK, oppgaver)
                }
            }
            route("person/finnes-soknad-til-behandling") {
                post {
                    val soknad: SoknadDTO = call.receive<SoknadDTO>()
                    // todo finn oppgave og sjekk tilstand
                    val finnesSoknadTilBehandling = true
                    call.respond(status = HttpStatusCode.OK, finnesSoknadTilBehandling)
                }
            }
            route("oppgave") {
                get {
                    val søkefilter = Søkefilter.fra(call.request.queryParameters, call.navIdent())
                    sikkerlogger.info {
                        "Henter alle oppgaver med følgende søkefilter: $søkefilter"
                    }
                    val oppgaver = oppgaveMediator.søk(søkefilter).tilOppgaverOversiktResultatDTO()
                    call.respond(status = HttpStatusCode.OK, oppgaver)
                }
                route("neste") {
                    put {
                        val dto = call.receive<NesteOppgaveDTO>()
                        val saksbehandler = applicationCallParser.sakbehandler(call = call)
                        sikkerlogger.info {
                            "Henter neste oppgave for saksbehandler ${saksbehandler.navIdent} med " +
                                "queryparams: ${dto.queryParams}"
                        }
                        val oppgave =
                            oppgaveMediator.tildelOgHentNesteOppgave(
                                nesteOppgaveHendelse =
                                    NesteOppgaveHendelse(
                                        ansvarligIdent = saksbehandler.navIdent,
                                        utførtAv = saksbehandler,
                                    ),
                                queryString = dto.queryParams,
                            )
                        when (oppgave) {
                            null -> call.respond(HttpStatusCode.NotFound)
                            else -> call.respond(HttpStatusCode.OK, oppgaveDTOMapper.lagOppgaveDTO(oppgave))
                        }
                    }
                }

                route("{oppgaveId}") {
                    get {
                        val saksbehandler = applicationCallParser.sakbehandler(call)
                        val oppgaveId = call.finnUUID("oppgaveId")
                        withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                            val oppgave = oppgaveMediator.hentOppgave(oppgaveId, saksbehandler)
                            val oppgaveDTO = oppgaveDTOMapper.lagOppgaveDTO(oppgave)
                            call.respond(HttpStatusCode.OK, oppgaveDTO)
                        }
                    }
                    route("notat") {
                        put {
                            val notat = call.receiveText()
                            val oppgaveId = call.finnUUID("oppgaveId")
                            val saksbehandler = applicationCallParser.sakbehandler(call)
                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                val sistEndretTidspunkt =
                                    oppgaveMediator.lagreNotat(
                                        NotatHendelse(
                                            oppgaveId = oppgaveId,
                                            tekst = notat,
                                            utførtAv = saksbehandler,
                                        ),
                                    )
                                call.respond(
                                    status = HttpStatusCode.OK,
                                    message = LagreNotatResponseDTO(sistEndretTidspunkt = sistEndretTidspunkt),
                                )
                            }
                        }
                        delete {
                            val oppgaveId = call.finnUUID("oppgaveId")
                            val saksbehandler = applicationCallParser.sakbehandler(call)

                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                val sistEndretTidspunkt =
                                    oppgaveMediator.slettNotat(
                                        SlettNotatHendelse(
                                            oppgaveId = oppgaveId,
                                            utførtAv = saksbehandler,
                                        ),
                                    )

                                call.respond(
                                    status = HttpStatusCode.OK,
                                    message = LagreNotatResponseDTO(sistEndretTidspunkt = sistEndretTidspunkt),
                                )
                            }
                        }
                    }
                    route("tildel") {
                        put {
                            val saksbehandler = applicationCallParser.sakbehandler(call)
                            val oppgaveAnsvarHendelse = call.settOppgaveAnsvarHendelse(saksbehandler)
                            val oppgaveId = call.finnUUID("oppgaveId")
                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                val nyTilstand: OppdatertTilstandDTO =
                                    OppdatertTilstandDTO(
                                        oppgaveMediator.tildelOppgave(oppgaveAnsvarHendelse).tilOppgaveTilstandDTO(),
                                    )
                                call.respond(
                                    status = HttpStatusCode.OK,
                                    message = nyTilstand,
                                )
                            }
                        }
                    }
                    route("utsett") {
                        put {
                            val saksbehandler = applicationCallParser.sakbehandler(call)
                            val utsettOppgaveHendelse = call.utsettOppgaveHendelse(saksbehandler)
                            val oppgaveId = call.finnUUID("oppgaveId")
                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                logger.info("Utsetter oppgave: $utsettOppgaveHendelse")
                                oppgaveMediator.utsettOppgave(utsettOppgaveHendelse)
                                call.respond(HttpStatusCode.NoContent)
                            }
                        }
                    }
                    route("legg-tilbake") {
                        put {
                            val saksbehandler = applicationCallParser.sakbehandler(call)
                            val oppgaveAnsvarHendelse = call.fjernOppgaveAnsvarHendelse(saksbehandler)
                            val oppgaveId = call.finnUUID("oppgaveId")
                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                oppgaveMediator.fristillOppgave(oppgaveAnsvarHendelse)
                                call.respond(HttpStatusCode.NoContent)
                            }
                        }
                    }
                    route("send-til-kontroll") {
                        put {
                            val saksbehandler = applicationCallParser.sakbehandler(call)
                            val saksbehandlerToken = call.request.jwt()
                            val sendTilKontrollHendelse = call.sendTilKontrollHendelse(saksbehandler)
                            val oppgaveId = call.finnUUID("oppgaveId")
                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                logger.info("Sender oppgave til kontroll: $sendTilKontrollHendelse")
                                oppgaveMediator.sendTilKontroll(sendTilKontrollHendelse, saksbehandlerToken)
                                call.respond(HttpStatusCode.NoContent)
                            }
                        }
                    }
                    route("returner-til-saksbehandler") {
                        put {
                            val saksbehandler = applicationCallParser.sakbehandler(call)
                            val saksbehandlerToken = call.request.jwt()
                            val returnerTilSaksbehandlingHendelse =
                                call.returnerTilSaksbehandlingHendelse(saksbehandler)
                            val oppgaveId = call.finnUUID("oppgaveId")
                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                logger.info("Sender oppgave tilbake til saksbehandler: $returnerTilSaksbehandlingHendelse")
                                oppgaveMediator.returnerTilSaksbehandling(
                                    returnerTilSaksbehandlingHendelse,
                                    saksbehandlerToken,
                                )
                                call.respond(HttpStatusCode.NoContent)
                            }
                        }
                    }

                    route("ferdigstill/melding-om-vedtak") {
                        put {
                            val oppgaveId = call.finnUUID("oppgaveId")
                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                val saksbehandler = applicationCallParser.sakbehandler(call)
                                val saksbehandlerToken = call.request.jwt()
                                oppgaveMediator.ferdigstillOppgave(
                                    oppgaveId = oppgaveId,
                                    saksBehandler = saksbehandler,
                                    saksbehandlerToken = saksbehandlerToken,
                                )
                                call.respond(HttpStatusCode.NoContent)
                            }
                        }
                    }

                    route("ferdigstill/melding-om-vedtak-arena") {
                        put {
                            val saksbehandler = applicationCallParser.sakbehandler(call)
                            val oppgaveId = call.finnUUID("oppgaveId")
                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                val saksbehandlerToken = call.request.jwt()
                                oppgaveMediator.ferdigstillOppgave(
                                    GodkjennBehandlingMedBrevIArena(
                                        oppgaveId = oppgaveId,
                                        utførtAv = saksbehandler,
                                    ),
                                    saksbehandlerToken,
                                )
                                call.respond(HttpStatusCode.NoContent)
                            }
                        }
                    }
                }
            }
            route("behandling/{behandlingId}/oppgaveId") {
                get {
                    val behandlingId = call.finnUUID("behandlingId")
                    when (val oppgaveId: UUID? = oppgaveMediator.hentOppgaveIdFor(behandlingId = behandlingId)) {
                        null -> call.respond(HttpStatusCode.NotFound)
                        else ->
                            call.respondText(
                                contentType = ContentType.Text.Plain,
                                status = HttpStatusCode.OK,
                                text = oppgaveId.toString(),
                            )
                    }
                }
            }
        }
    }
}

private suspend fun ApplicationCall.utsettOppgaveHendelse(saksbehandler: Saksbehandler): UtsettOppgaveHendelse {
    val utsettOppgaveDto = this.receive<UtsettOppgaveDTO>()
    return UtsettOppgaveHendelse(
        oppgaveId = this.finnUUID("oppgaveId"),
        navIdent = saksbehandler.navIdent,
        utsattTil = utsettOppgaveDto.utsettTilDato,
        beholdOppgave = utsettOppgaveDto.beholdOppgave,
        utførtAv = saksbehandler,
        årsak =
            when (utsettOppgaveDto.aarsak) {
                UtsettOppgaveAarsakDTO.AVVENT_SVAR -> PåVent.AVVENT_SVAR
                UtsettOppgaveAarsakDTO.AVVENT_MELDEKORT -> PåVent.AVVENT_MELDEKORT
                UtsettOppgaveAarsakDTO.AVVENT_DOKUMENTASJON -> PåVent.AVVENT_DOKUMENTASJON
                UtsettOppgaveAarsakDTO.AVVENT_RAPPORTERINGSFRIST -> PåVent.AVVENT_RAPPORTERINGSFRIST
                UtsettOppgaveAarsakDTO.AVVENT_SVAR_PÅ_FORESPØRSEL -> PåVent.AVVENT_SVAR_PÅ_FORESPØRSEL
                UtsettOppgaveAarsakDTO.ANNET -> PåVent.ANNET
                null -> PåVent.ANNET
            },
    )
}

private fun ApplicationCall.settOppgaveAnsvarHendelse(saksbehandler: Saksbehandler): SettOppgaveAnsvarHendelse {
    return SettOppgaveAnsvarHendelse(
        oppgaveId = this.finnUUID("oppgaveId"),
        ansvarligIdent = saksbehandler.navIdent,
        utførtAv = saksbehandler,
    )
}

private fun ApplicationCall.fjernOppgaveAnsvarHendelse(saksbehandler: Saksbehandler): FjernOppgaveAnsvarHendelse {
    return FjernOppgaveAnsvarHendelse(
        oppgaveId = this.finnUUID("oppgaveId"),
        utførtAv = saksbehandler,
    )
}

private fun ApplicationCall.sendTilKontrollHendelse(saksbehandler: Saksbehandler): SendTilKontrollHendelse {
    return SendTilKontrollHendelse(
        oppgaveId = this.finnUUID("oppgaveId"),
        utførtAv = saksbehandler,
    )
}

private fun ApplicationCall.returnerTilSaksbehandlingHendelse(saksbehandler: Saksbehandler): ReturnerTilSaksbehandlingHendelse {
    return ReturnerTilSaksbehandlingHendelse(
        oppgaveId = this.finnUUID("oppgaveId"),
        utførtAv = saksbehandler,
    )
}

class InternDataException(message: String) : RuntimeException(message)

internal fun ApplicationCall.finnUUID(pathParam: String): UUID =
    parameters[pathParam]?.let {
        UUID.fromString(it)
    } ?: throw IllegalArgumentException("Kunne ikke finne oppgaveId i path")
