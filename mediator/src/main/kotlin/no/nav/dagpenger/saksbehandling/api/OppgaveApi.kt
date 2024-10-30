package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.api.models.AdressebeskyttelseGraderingDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.KjonnDTO
import no.nav.dagpenger.saksbehandling.api.models.NesteOppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.NotatDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonIdentDTO
import no.nav.dagpenger.saksbehandling.api.models.UtsettOppgaveDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdClient
import no.nav.dagpenger.saksbehandling.jwt.ApplicationCallParser
import no.nav.dagpenger.saksbehandling.jwt.jwt
import no.nav.dagpenger.saksbehandling.jwt.navIdent
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag
import java.util.UUID

private val logger = KotlinLogging.logger { }
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

internal fun Application.oppgaveApi(
    oppgaveMediator: OppgaveMediator,
    pdlKlient: PDLKlient,
    journalpostIdClient: JournalpostIdClient,
    saksbehandlerOppslag: SaksbehandlerOppslag,
    applicationCallParser: ApplicationCallParser,
) {
    suspend fun oppgaveDTO(oppgave: Oppgave): OppgaveDTO =
        coroutineScope {
            val person = async { pdlKlient.person(oppgave.behandling.person.ident).getOrThrow() }
            val journalpostIder = async { journalpostIdClient.hentJournalPostIder(oppgave.behandling) }
            val sisteSaksbehandlerDTO =
                oppgave.sisteSaksbehandler()?.let { saksbehandlerIdent ->
                    async { saksbehandlerOppslag.hentSaksbehandler(saksbehandlerIdent) }
                }
            val sisteBeslutterDTO =
                oppgave.sisteBeslutter()?.let { beslutterIdent ->
                    async { saksbehandlerOppslag.hentSaksbehandler(beslutterIdent) }
                }
            val oppgaveDTO =
                lagOppgaveDTO(
                    oppgave = oppgave,
                    person = person.await(),
                    journalpostIder = journalpostIder.await(),
                    sisteSaksbehandlerDTO = sisteSaksbehandlerDTO?.await(),
                    sisteBeslutterDTO = sisteBeslutterDTO?.await(),
                )
            oppgaveDTO
        }
    routing {
        swaggerUI(path = "openapi", swaggerFile = "saksbehandling-api.yaml")

        authenticate("azureAd") {
            route("person/oppgaver") {
                post {
                    val oppgaver =
                        oppgaveMediator.finnOppgaverFor(call.receive<PersonIdentDTO>().ident).tilOppgaverOversiktDTO()
                    call.respond(status = HttpStatusCode.OK, oppgaver)
                }
            }
            route("oppgave") {
                get {
                    val søkefilter = Søkefilter.fra(call.request.queryParameters, call.navIdent())

                    val oppgaver = oppgaveMediator.søk(søkefilter).tilOppgaverOversiktDTO()
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
                            else -> call.respond(HttpStatusCode.OK, oppgaveDTO(oppgave))
                        }
                    }
                }

                route("{oppgaveId}") {
                    get {
                        val saksbehandler = applicationCallParser.sakbehandler(call)
                        val oppgaveId = call.finnUUID("oppgaveId")
                        withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                            val oppgave = oppgaveMediator.hentOppgave(oppgaveId, saksbehandler)
                            val oppgaveDTO = oppgaveDTO(oppgave)
                            call.respond(HttpStatusCode.OK, oppgaveDTO)
                        }
                    }
                    route("notat") {
                        put {
                            val notat = call.receiveText()
                            val oppgaveId = call.finnUUID("oppgaveId")
                            val saksbehandler = applicationCallParser.sakbehandler(call)
                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                oppgaveMediator.lagreNotat(
                                    NotatHendelse(
                                        oppgaveId = oppgaveId,
                                        tekst = notat,
                                        utførtAv = saksbehandler,
                                    ),
                                )
                                call.respond(HttpStatusCode.NoContent)
                            }
                        }
                    }
                    route("tildel") {
                        put {
                            val saksbehandler = applicationCallParser.sakbehandler(call)
                            val oppgaveAnsvarHendelse = call.settOppgaveAnsvarHendelse(saksbehandler)
                            val oppgaveId = call.finnUUID("oppgaveId")

                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                val oppdatertTilstand = oppgaveMediator.tildelOppgave(oppgaveAnsvarHendelse).tilOppgaveTilstandDTO()
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
                            val klarTilKontrollHendelse = call.klarTilKontrollHendelse(saksbehandler)
                            val oppgaveId = call.finnUUID("oppgaveId")
                            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                                logger.info("Sender oppgave til kontroll: $klarTilKontrollHendelse")
                                oppgaveMediator.sendTilKontroll(klarTilKontrollHendelse)
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

private val PipelineContext<Unit, ApplicationCall>.htmlContentType: Boolean
    get() = call.request.contentType().match(ContentType.Text.Html)

private suspend fun JournalpostIdClient.hentJournalPostIder(behandling: Behandling): Set<String> {
    return when (val hendelse = behandling.hendelse) {
        is SøknadsbehandlingOpprettetHendelse -> {
            this.hentJournalpostId(hendelse.søknadId).map {
                setOf(it)
            }.getOrElse {
                emptySet()
            }
        }

        else -> emptySet()
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

private fun ApplicationCall.klarTilKontrollHendelse(saksbehandler: Saksbehandler): SendTilKontrollHendelse {
    return SendTilKontrollHendelse(
        oppgaveId = this.finnUUID("oppgaveId"),
        utførtAv = saksbehandler,
    )
}

fun lagOppgaveDTO(
    oppgave: Oppgave,
    person: PDLPersonIntern,
    journalpostIder: Set<String>,
    sisteSaksbehandlerDTO: BehandlerDTO? = null,
    sisteBeslutterDTO: BehandlerDTO? = null,
): OppgaveDTO =

    OppgaveDTO(
        oppgaveId = oppgave.oppgaveId,
        behandlingId = oppgave.behandling.behandlingId,
        person =
            PersonDTO(
                ident = person.ident,
                fornavn = person.fornavn,
                etternavn = person.etternavn,
                mellomnavn = person.mellomnavn,
                fodselsdato = person.fødselsdato,
                alder = person.alder,
                kjonn =
                    when (person.kjønn) {
                        PDLPerson.Kjonn.MANN -> KjonnDTO.MANN
                        PDLPerson.Kjonn.KVINNE -> KjonnDTO.KVINNE
                        PDLPerson.Kjonn.UKJENT -> KjonnDTO.UKJENT
                    },
                statsborgerskap = person.statsborgerskap,
                skjermesSomEgneAnsatte = oppgave.behandling.person.skjermesSomEgneAnsatte,
                adressebeskyttelseGradering =
                    when (oppgave.behandling.person.adressebeskyttelseGradering) {
                        AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG_UTLAND
                        AdressebeskyttelseGradering.STRENGT_FORTROLIG -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG
                        AdressebeskyttelseGradering.FORTROLIG -> AdressebeskyttelseGraderingDTO.FORTROLIG
                        AdressebeskyttelseGradering.UGRADERT -> AdressebeskyttelseGraderingDTO.UGRADERT
                    },
            ),
        tidspunktOpprettet = oppgave.opprettet,
        emneknagger = oppgave.emneknagger.toList(),
        tilstand = oppgave.tilstand().tilOppgaveTilstandDTO(),
        journalpostIder = journalpostIder.toList(),
        utsattTilDato = oppgave.utsattTil(),
        saksbehandler = sisteSaksbehandlerDTO,
        beslutter = sisteBeslutterDTO,
        notat =
            oppgave.tilstand().notat()?.let {
                NotatDTO(
                    tekst = it.hentTekst(),
                    sistEndretTidspunkt = it.sistEndretTidspunkt!!,
                )
            },
    )

private fun List<Oppgave>.tilOppgaverOversiktDTO(): List<OppgaveOversiktDTO> {
    return this.map { oppgave -> oppgave.tilOppgaveOversiktDTO() }
}

private fun Oppgave.Tilstand.tilOppgaveTilstandDTO(): OppgaveTilstandDTO {
    return when (this) {
        is Oppgave.Opprettet -> throw InternDataException("Ikke tillatt å eksponere oppgavetilstand Opprettet")
        is Oppgave.KlarTilBehandling -> OppgaveTilstandDTO.KLAR_TIL_BEHANDLING
        is Oppgave.UnderBehandling -> OppgaveTilstandDTO.UNDER_BEHANDLING
        is Oppgave.FerdigBehandlet -> OppgaveTilstandDTO.FERDIG_BEHANDLET
        is Oppgave.PåVent -> OppgaveTilstandDTO.PAA_VENT
        is Oppgave.KlarTilKontroll -> OppgaveTilstandDTO.KLAR_TIL_KONTROLL
        is Oppgave.UnderKontroll -> OppgaveTilstandDTO.UNDER_KONTROLL
        else -> throw InternDataException("Ukjent tilstand: $this")
    }
}

class InternDataException(message: String) : RuntimeException(message)

internal fun Oppgave.tilOppgaveOversiktDTO() =
    OppgaveOversiktDTO(
        oppgaveId = this.oppgaveId,
        behandlingId = this.behandling.behandlingId,
        personIdent = this.behandling.person.ident,
        tidspunktOpprettet = this.opprettet,
        emneknagger = this.emneknagger.toList(),
        skjermesSomEgneAnsatte = this.behandling.person.skjermesSomEgneAnsatte,
        adressebeskyttelseGradering =
            when (this.behandling.person.adressebeskyttelseGradering) {
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG_UTLAND
                AdressebeskyttelseGradering.STRENGT_FORTROLIG -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG
                AdressebeskyttelseGradering.FORTROLIG -> AdressebeskyttelseGraderingDTO.FORTROLIG
                AdressebeskyttelseGradering.UGRADERT -> AdressebeskyttelseGraderingDTO.UGRADERT
            },
        tilstand = this.tilstand().tilOppgaveTilstandDTO(),
        saksbehandlerIdent = this.behandlerIdent,
        behandlerIdent = this.behandlerIdent,
        utsattTilDato = this.utsattTil(),
    )

internal fun ApplicationCall.finnUUID(pathParam: String): UUID =
    parameters[pathParam]?.let {
        UUID.fromString(it)
    } ?: throw IllegalArgumentException("Kunne ikke finne oppgaveId i path")
