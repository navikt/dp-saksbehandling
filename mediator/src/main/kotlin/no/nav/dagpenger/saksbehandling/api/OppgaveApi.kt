package no.nav.dagpenger.saksbehandling.api

import PersonMediator
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.Emneknagg.AvbrytBehandling
import no.nav.dagpenger.saksbehandling.Emneknagg.PåVent
import no.nav.dagpenger.saksbehandling.Oppgave.KontrollertBrev.IKKE_RELEVANT
import no.nav.dagpenger.saksbehandling.Oppgave.KontrollertBrev.JA
import no.nav.dagpenger.saksbehandling.Oppgave.KontrollertBrev.NEI
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.GOSYS
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.INGEN
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.api.models.AvbrytOppgaveAarsakDTO
import no.nav.dagpenger.saksbehandling.api.models.AvbrytOppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.HttpProblemDTO
import no.nav.dagpenger.saksbehandling.api.models.KontrollertBrevDTO
import no.nav.dagpenger.saksbehandling.api.models.KontrollertBrevRequestDTO
import no.nav.dagpenger.saksbehandling.api.models.LagreNotatResponseDTO
import no.nav.dagpenger.saksbehandling.api.models.MeldingOmVedtakKildeDTO
import no.nav.dagpenger.saksbehandling.api.models.MeldingOmVedtakKildeRequestDTO
import no.nav.dagpenger.saksbehandling.api.models.NesteOppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.NotatRequestDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonIdDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonIdentDTO
import no.nav.dagpenger.saksbehandling.api.models.SakIdDTO
import no.nav.dagpenger.saksbehandling.api.models.SoknadDTO
import no.nav.dagpenger.saksbehandling.api.models.TildeltOppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.UtsettOppgaveAarsakDTO
import no.nav.dagpenger.saksbehandling.api.models.UtsettOppgaveDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.hendelser.AvbrytOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
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
import java.net.URI
import java.util.UUID

private val logger = KotlinLogging.logger { }
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

internal fun Route.oppgaveApi(
    oppgaveMediator: OppgaveMediator,
    personMediator: PersonMediator,
    oppgaveDTOMapper: OppgaveDTOMapper,
    applicationCallParser: ApplicationCallParser,
) {
    authenticate("azureAd-maskin") {
        route("person/skal-varsle-om-ettersending") {
            post {
                val soknad: SoknadDTO = call.receive<SoknadDTO>()
                val skalVarsle =
                    oppgaveMediator.skalEttersendingTilSøknadVarsles(
                        søknadId = soknad.soknadId,
                        ident = soknad.ident,
                    )
                call.respond(status = HttpStatusCode.OK, skalVarsle)
            }
        }
        route("person/siste-sak") {
            post {
                val personIdentDTO: PersonIdentDTO = call.receive<PersonIdentDTO>()
                val sisteSakId = oppgaveDTOMapper.hentSisteSakId(ident = personIdentDTO.ident)
                call.respond(
                    status = HttpStatusCode.OK,
                    message = SakIdDTO(id = sisteSakId),
                )
            }
        }
    }

    authenticate("azureAd") {
        route("person/personId") {
            post {
                val personIdentDTO: PersonIdentDTO = call.receive<PersonIdentDTO>()
                val personIdDTO = PersonIdDTO(id = personMediator.hentPerson(personIdentDTO.ident).id)
                call.respond(status = HttpStatusCode.OK, personIdDTO)
            }
        }
        route("person/{personId}") {
            get {
                val personId: UUID = call.finnUUID("personId")
                sikkerlogger.info { "Søker etter person med UUID i url: $personId" }
                val person = personMediator.hentPerson(personId)
                val oppgaver =
                    oppgaveMediator.finnOppgaverFor(person.ident)
                        .tilOppgaveOversiktDTOListe()
                val personOversiktDTO = oppgaveDTOMapper.lagPersonOversiktDTO(person, oppgaver)
                call.respond(status = HttpStatusCode.OK, personOversiktDTO)
            }
        }
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
                sikkerlogger.info {
                    "Henter alle oppgaver med følgende søkefilter: $søkefilter"
                }
                val oppgaver = oppgaveMediator.søk(søkefilter).tilOppgaverOversiktResultatDTO()
                call.respond(status = HttpStatusCode.OK, oppgaver)
            }
            route("neste") {
                put {
                    val dto = call.receive<NesteOppgaveDTO>()
                    val saksbehandler = applicationCallParser.saksbehandler(call = call)
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
                        null ->
                            call.respond(
                                status = HttpStatusCode.NotFound,
                                message =
                                    HttpProblemDTO(
                                        title = "Ingen oppgave funnet",
                                        status = 404,
                                        instance = call.request.path(),
                                        detail = "Ingen oppgave funnet for søket",
                                        type =
                                            URI.create("dagpenger.nav.no/saksbehandling:problem:ingen-oppgave-funnet")
                                                .toString(),
                                    ),
                            )

                        else -> call.respond(HttpStatusCode.OK, oppgaveDTOMapper.lagOppgaveDTO(oppgave))
                    }
                }
            }

            route("{oppgaveId}") {
                get {
                    val saksbehandler = applicationCallParser.saksbehandler(call)
                    val oppgaveId = call.finnUUID("oppgaveId")
                    withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                        val oppgave = oppgaveMediator.hentOppgave(oppgaveId, saksbehandler)
                        val oppgaveDTO = oppgaveDTOMapper.lagOppgaveDTO(oppgave)
                        call.respond(HttpStatusCode.OK, oppgaveDTO)
                    }
                }
                route("kontrollert-brev") {
                    put {
                        val oppgaveId = call.finnUUID("oppgaveId")
                        withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                            val saksbehandler = applicationCallParser.saksbehandler(call)

                            val kontrollert =
                                try {
                                    call.receive<KontrollertBrevRequestDTO>().kontrollert
                                } catch (t: Throwable) {
                                    logger.warn { "Kunne ikke lese om beslutter har kontrollert fra request body" }
                                    sikkerlogger.warn {
                                        "Kunne ikke lese om beslutter har kontrollert fra request body. Feilmelding: ${t.message}"
                                    }
                                    throw IllegalArgumentException(t)
                                }
                            val kontrollertBrev =
                                when (kontrollert) {
                                    KontrollertBrevDTO.JA -> JA
                                    KontrollertBrevDTO.NEI -> NEI
                                    KontrollertBrevDTO.IKKE_RELEVANT -> IKKE_RELEVANT
                                }
                            oppgaveMediator.lagreKontrollertBrev(
                                oppgaveId = oppgaveId,
                                kontrollertBrev = kontrollertBrev,
                                saksbehandler = saksbehandler,
                            )
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
                route("melding-om-vedtak-kilde") {
                    put {
                        val oppgaveId = call.finnUUID("oppgaveId")
                        withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                            val saksbehandler = applicationCallParser.saksbehandler(call)

                            val meldingOmVedtakKildeRequestDTO =
                                try {
                                    call.receive<MeldingOmVedtakKildeRequestDTO>().meldingOmVedtakKilde
                                } catch (t: Throwable) {
                                    logger.warn {
                                        "Kunne ikke lese kilde for melding om vedtak fra request body, " +
                                            "bruker DP_SAK som default"
                                    }
                                    sikkerlogger.warn {
                                        "Kunne ikke lese kilde for melding om vedtak fra request body, " +
                                            "bruker DP_SAK som default. Feilmelding: ${t.message}"
                                    }
                                    MeldingOmVedtakKildeDTO.DP_SAK
                                }
                            val meldingOmVedtakKilde =
                                when (meldingOmVedtakKildeRequestDTO) {
                                    MeldingOmVedtakKildeDTO.DP_SAK -> DP_SAK
                                    MeldingOmVedtakKildeDTO.GOSYS -> GOSYS
                                    MeldingOmVedtakKildeDTO.INGEN -> INGEN
                                    null -> DP_SAK
                                }
                            oppgaveMediator.endreMeldingOmVedtakKilde(
                                oppgaveId = oppgaveId,
                                meldingOmVedtakKilde = meldingOmVedtakKilde,
                                saksbehandler = saksbehandler,
                            )
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
                route("notat") {
                    put {
                        val notat = call.receive<NotatRequestDTO>()
                        val oppgaveId = call.finnUUID("oppgaveId")
                        val saksbehandler = applicationCallParser.saksbehandler(call)
                        withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                            val sistEndretTidspunkt =
                                oppgaveMediator.lagreNotat(
                                    NotatHendelse(
                                        oppgaveId = oppgaveId,
                                        tekst = notat.tekst,
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
                        val saksbehandler = applicationCallParser.saksbehandler(call)

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
                        val saksbehandler = applicationCallParser.saksbehandler(call)
                        val oppgaveAnsvarHendelse = call.settOppgaveAnsvarHendelse(saksbehandler)
                        val oppgaveId = call.finnUUID("oppgaveId")
                        withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                            val tildeltOppgave: TildeltOppgaveDTO =
                                oppgaveMediator.tildelOppgave(
                                    oppgaveAnsvarHendelse,
                                ).tilTildeltOppgaveDTO()
                            call.respond(
                                status = HttpStatusCode.OK,
                                message = tildeltOppgave,
                            )
                        }
                    }
                }
                route("utsett") {
                    put {
                        val saksbehandler = applicationCallParser.saksbehandler(call)
                        val utsettOppgaveHendelse = call.utsettOppgaveHendelse(saksbehandler)
                        val oppgaveId = call.finnUUID("oppgaveId")
                        withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                            logger.info { "Utsetter oppgave: $utsettOppgaveHendelse" }
                            oppgaveMediator.utsettOppgave(utsettOppgaveHendelse)
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
                route("legg-tilbake") {
                    put {
                        val saksbehandler = applicationCallParser.saksbehandler(call)
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
                        val saksbehandler = applicationCallParser.saksbehandler(call)
                        val saksbehandlerToken = call.request.jwt()
                        val sendTilKontrollHendelse = call.sendTilKontrollHendelse(saksbehandler)
                        val oppgaveId = call.finnUUID("oppgaveId")
                        withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                            logger.info { "Sender oppgave til kontroll: $sendTilKontrollHendelse" }
                            oppgaveMediator.sendTilKontroll(sendTilKontrollHendelse, saksbehandlerToken)
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
                route("returner-til-saksbehandler") {
                    put {
                        val saksbehandler = applicationCallParser.saksbehandler(call)
                        val saksbehandlerToken = call.request.jwt()
                        val returnerTilSaksbehandlingHendelse =
                            call.returnerTilSaksbehandlingHendelse(saksbehandler)
                        val oppgaveId = call.finnUUID("oppgaveId")
                        withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                            logger.info { "Sender oppgave tilbake til saksbehandler: $returnerTilSaksbehandlingHendelse" }
                            oppgaveMediator.returnerTilSaksbehandling(
                                returnerTilSaksbehandlingHendelse,
                                saksbehandlerToken,
                            )
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }

                route("avbryt") {
                    put {
                        val oppgaveId = call.finnUUID("oppgaveId")
                        withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                            val saksbehandler = applicationCallParser.saksbehandler(call)
                            val avbrytOppgaveHendelse = call.avbrytOppgaveHendelse(saksbehandler = saksbehandler)
                            val saksbehandlerToken = call.request.jwt()
                            oppgaveMediator.avbryt(
                                avbrytOppgaveHendelse = avbrytOppgaveHendelse,
                                saksbehandlerToken = saksbehandlerToken,
                            )
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }

                route("ferdigstill") {
                    put {
                        val oppgaveId = call.finnUUID("oppgaveId")
                        withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                            val saksbehandler = applicationCallParser.saksbehandler(call)
                            val saksbehandlerToken = call.request.jwt()
                            oppgaveMediator.ferdigstillOppgave(
                                oppgaveId = oppgaveId,
                                saksbehandler = saksbehandler,
                                saksbehandlerToken = saksbehandlerToken,
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

private suspend fun ApplicationCall.avbrytOppgaveHendelse(saksbehandler: Saksbehandler): AvbrytOppgaveHendelse {
    val avbrytOppgaveDTO = this.receive<AvbrytOppgaveDTO>()
    return AvbrytOppgaveHendelse(
        oppgaveId = this.finnUUID("oppgaveId"),
        navIdent = saksbehandler.navIdent,
        utførtAv = saksbehandler,
        årsak =
            when (avbrytOppgaveDTO.aarsak) {
                AvbrytOppgaveAarsakDTO.BEHANDLES_I_ARENA -> AvbrytBehandling.AVBRUTT_BEHANDLES_I_ARENA
                AvbrytOppgaveAarsakDTO.FLERE_SØKNADER -> AvbrytBehandling.AVBRUTT_FLERE_SØKNADER
                AvbrytOppgaveAarsakDTO.TRUKKET_SØKNAD -> AvbrytBehandling.AVBRUTT_TRUKKET_SØKNAD
                AvbrytOppgaveAarsakDTO.ANNET -> AvbrytBehandling.AVBRUTT_ANNET
            },
    )
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
                UtsettOppgaveAarsakDTO.AVVENT_PERMITTERINGSÅRSAK -> PåVent.AVVENT_PERMITTERINGSÅRSAK
                UtsettOppgaveAarsakDTO.AVVENT_RAPPORTERINGSFRIST -> PåVent.AVVENT_RAPPORTERINGSFRIST
                UtsettOppgaveAarsakDTO.AVVENT_SVAR_PÅ_FORESPØRSEL -> PåVent.AVVENT_SVAR_PÅ_FORESPØRSEL
                UtsettOppgaveAarsakDTO.ANNET -> PåVent.AVVENT_ANNET
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
