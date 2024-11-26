package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.api.models.LagreNotatResponseDTO
import no.nav.dagpenger.saksbehandling.api.models.NesteOppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonIdentDTO
import no.nav.dagpenger.saksbehandling.api.models.UtsettOppgaveDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
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
            route("oppgave") {
                get {
                    val søkefilter = Søkefilter.fra(call.request.queryParameters, call.navIdent())

                    val oppgaver = oppgaveMediator.søk(søkefilter).tilOppgaveOversiktDTOListe()
                    call.respond(status = HttpStatusCode.OK, oppgaver)
                }
                route("neste") {
                    put {
                        val dto = call.receive<NesteOppgaveDTO>()
                        val saksbehandler = applicationCallParser.sakbehandler(call = call)
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
                            logger.debug {
                                "Oppgave med tilstand ${oppgave.tilstand().type} " +
                                    "har journalpostIder: ${oppgaveDTO.journalpostIder}"
                            }
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
                                    HttpStatusCode.OK,
                                    LagreNotatResponseDTO(sistEndretTidspunkt = sistEndretTidspunkt),
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
                                val oppdatertTilstand =
                                    oppgaveMediator.tildelOppgave(oppgaveAnsvarHendelse).tilOppgaveTilstandDTO()
                                call.respondText(
                                    contentType = ContentType.Text.Plain,
                                    status = HttpStatusCode.OK,
                                    text = oppdatertTilstand.toString(),
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
                            val returnerTilSaksbehandlingHendelse =
                                call.returnerTilSaksbehandlingHendelse(saksbehandler)

                            val oppgaveId = call.finnUUID("oppgaveId")

                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                logger.info("Sender oppgave tilbake til saksbehandler: $returnerTilSaksbehandlingHendelse")
                                oppgaveMediator.returnerTilSaksbehandling(returnerTilSaksbehandlingHendelse)
                                call.respond(HttpStatusCode.NoContent)
                            }
                        }
                    }

                    route("ferdigstill/melding-om-vedtak") {
                        put {
                            val meldingOmVedtak = call.receiveText()
                            val oppgaveId = call.finnUUID("oppgaveId")
                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                try {
                                    if (!htmlContentType) throw UgyldigContentType("Kun støtte for HTML")
                                    sikkerlogger.info { "Motatt melding om vedtak for oppgave $oppgaveId: $meldingOmVedtak" }
                                    val saksbehandler = applicationCallParser.sakbehandler(call)
                                    val saksbehandlerToken = call.request.jwt()
                                    oppgaveMediator.ferdigstillOppgave(
                                        GodkjentBehandlingHendelse(
                                            meldingOmVedtak = meldingOmVedtak,
                                            oppgaveId = oppgaveId,
                                            utførtAv = saksbehandler,
                                        ),
                                        saksbehandlerToken,
                                    )
                                    call.respond(HttpStatusCode.NoContent)
                                } catch (e: UgyldigContentType) {
                                    val feilmelding = "Feil ved mottak av melding om vedtak: ${e.message}"
                                    logger.error(e) { feilmelding }
                                    sikkerlogger.error(e) { "$feilmelding for $meldingOmVedtak" }
                                    call.respond(HttpStatusCode.UnsupportedMediaType)
                                }
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

class UgyldigContentType(message: String) : RuntimeException(message)

private val RoutingContext.htmlContentType: Boolean
    get() = call.request.contentType().match(ContentType.Text.Html)

private suspend fun ApplicationCall.utsettOppgaveHendelse(saksbehandler: Saksbehandler): UtsettOppgaveHendelse {
    val utsettOppgaveDto = this.receive<UtsettOppgaveDTO>()
    return UtsettOppgaveHendelse(
        oppgaveId = this.finnUUID("oppgaveId"),
        navIdent = saksbehandler.navIdent,
        utsattTil = utsettOppgaveDto.utsettTilDato,
        beholdOppgave = utsettOppgaveDto.beholdOppgave,
        utførtAv = saksbehandler,
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
