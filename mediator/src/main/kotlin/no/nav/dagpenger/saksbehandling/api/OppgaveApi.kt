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
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Aktør
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.Configuration.egneAnsatteADGruppe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.api.models.AdressebeskyttelseGraderingDTO
import no.nav.dagpenger.saksbehandling.api.models.KjonnDTO
import no.nav.dagpenger.saksbehandling.api.models.NesteOppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonIdentDTO
import no.nav.dagpenger.saksbehandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.UtsettOppgaveDTO
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.AdressebeskyttelseTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.EgneAnsatteTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.oppgaveTilgangskontroll
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdClient
import no.nav.dagpenger.saksbehandling.jwt.navIdent
import no.nav.dagpenger.saksbehandling.jwt.saksbehandler
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
) {
    suspend fun oppgaveDTO(oppgave: Oppgave): OppgaveDTO =
        coroutineScope {
            val person = async { pdlKlient.person(oppgave.ident).getOrThrow() }
            val journalpostIder = async { journalpostIdClient.hentJournalPostIder(oppgave.behandling) }
            val saksbehandlerDTO =
                oppgave.saksbehandlerIdent?.let { saksbehandlerIdent ->
                    async { saksbehandlerOppslag.hentSaksbehandler(saksbehandlerIdent) }
                }
            val oppgaveDTO = lagOppgaveDTO(oppgave, person.await(), journalpostIder.await(), saksbehandlerDTO?.await())
            oppgaveDTO
        }
    routing {
        val adressebeskyttelseTilgangskontroll =
            AdressebeskyttelseTilgangskontroll(
                strengtFortroligGruppe = Configuration.strengtFortroligADGruppe,
                strengtFortroligUtlandGruppe = Configuration.strengtFortroligUtlandADGruppe,
                fortroligGruppe = Configuration.fortroligADGruppe,
                adressebeskyttelseGraderingFun = oppgaveMediator::adresseGraderingForPerson,
            )

        val egneAnsatteTilgangskontroll =
            EgneAnsatteTilgangskontroll(
                tillatteGrupper = setOf(egneAnsatteADGruppe),
                skjermesSomEgneAnsatteFun = oppgaveMediator::personSkjermesSomEgneAnsatte,
            )
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
                        val saksbehandler = call.saksbehandler()
                        val dto = call.receive<NesteOppgaveDTO>()

                        val søkefilter =
                            TildelNesteOppgaveFilter.fra(
                                queryString = dto.queryParams,
                                saksbehandlerTilgangEgneAnsatte = egneAnsatteTilgangskontroll.harTilgang(saksbehandler),
                                adresseBeskyttelseGradering = adressebeskyttelseTilgangskontroll.tilganger(saksbehandler),
                            )

                        val oppgave =
                            oppgaveMediator.tildelNesteOppgaveTil(
                                saksbehandlerIdent = call.navIdent(),
                                filter = søkefilter,
                            )
                        when (oppgave) {
                            null -> call.respond(HttpStatusCode.NotFound)
                            else -> call.respond(HttpStatusCode.OK, oppgaveDTO(oppgave))
                        }
                    }
                }

                route("{oppgaveId}") {
                    val tilgangskontroller = setOf(adressebeskyttelseTilgangskontroll, egneAnsatteTilgangskontroll)

                    get {
                        oppgaveTilgangskontroll(tilgangskontroller)
                        val oppgaveId = call.finnUUID("oppgaveId")
                        val oppgave = oppgaveMediator.hentOppgave(oppgaveId)
                        val oppgaveDTO = oppgaveDTO(oppgave)
                        call.respond(HttpStatusCode.OK, oppgaveDTO)
                    }
                    route("tildel") {
                        put {
                            oppgaveTilgangskontroll(tilgangskontroller)
                            val oppgaveAnsvarHendelse = call.settOppgaveAnsvarHendelse()
                            try {
                                val oppgave = oppgaveMediator.tildelOppgave(oppgaveAnsvarHendelse)
                                call.respond(HttpStatusCode.OK, oppgaveDTO(oppgave))
                            } catch (e: Oppgave.AlleredeTildeltException) {
                                call.respond(HttpStatusCode.Conflict) { e.message.toString() }
                            }
                        }
                    }
                    route("utsett") {
                        put {
                            oppgaveTilgangskontroll(tilgangskontroller)
                            val utsettOppgaveHendelse = call.utsettOppgaveHendelse()
                            logger.info("Utsetter oppgave: $utsettOppgaveHendelse")
                            oppgaveMediator.utsettOppgave(utsettOppgaveHendelse)
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                    route("legg-tilbake") {
                        put {
                            val oppgaveAnsvarHendelse = call.settOppgaveAnsvarHendelse()
                            oppgaveMediator.fristillOppgave(oppgaveAnsvarHendelse)
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                    route("ferdigstill/melding-om-vedtak") {
                        put {
                            oppgaveTilgangskontroll(tilgangskontroller)
                            val meldingOmVedtak = call.receiveText()
                            try {
                                if (!htmlContentType) throw UgyldigContentType("Kun støtte for HTML")
                                val oppgaveId = call.finnUUID("oppgaveId")
                                sikkerlogger.info { "Motatt melding om vedtak for oppgave $oppgaveId: $meldingOmVedtak" }
                                val saksbehandler = call.saksbehandler()

                                // TODO fix saksbehandler vs beslutter
                                oppgaveMediator.ferdigstillOppgave(
                                    GodkjentBehandlingHendelse(
                                        meldingOmVedtak = meldingOmVedtak,
                                        oppgaveId = oppgaveId,
                                        saksbehandlerToken = saksbehandler.token,
                                        aktør = Aktør.Saksbehandler(navIdent = saksbehandler.navIdent),
                                    ),
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
                    route("ferdigstill/melding-om-vedtak-arena") {
                        put {
                            oppgaveTilgangskontroll(tilgangskontroller)
                            val saksbehandler = call.saksbehandler()
                            val oppgaveId = call.finnUUID("oppgaveId")
                            oppgaveMediator.ferdigstillOppgave(
                                GodkjennBehandlingMedBrevIArena(
                                    oppgaveId = oppgaveId,
                                    saksbehandlerToken = saksbehandler.token,
                                    aktør = Aktør.Saksbehandler(navIdent = saksbehandler.navIdent),
                                ),
                            )
                            call.respond(HttpStatusCode.NoContent)
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

private suspend fun ApplicationCall.utsettOppgaveHendelse(): UtsettOppgaveHendelse {
    val utsettOppgaveDto = this.receive<UtsettOppgaveDTO>()

    return UtsettOppgaveHendelse(
        oppgaveId = this.finnUUID("oppgaveId"),
        navIdent = this.navIdent(),
        utsattTil = utsettOppgaveDto.utsettTilDato,
        beholdOppgave = utsettOppgaveDto.beholdOppgave,
        aktør = Aktør.Saksbehandler(this.navIdent()),
    )
}

private fun ApplicationCall.settOppgaveAnsvarHendelse(): SettOppgaveAnsvarHendelse {
    val navIdent = this.navIdent()
    return SettOppgaveAnsvarHendelse(
        oppgaveId = this.finnUUID("oppgaveId"),
        ansvarligIdent = navIdent,
        utførtAv = Aktør.Saksbehandler(navIdent),
    )
}

fun lagOppgaveDTO(
    oppgave: Oppgave,
    person: PDLPersonIntern,
    journalpostIder: Set<String>,
    saksbehandlerDTO: SaksbehandlerDTO? = null,
): OppgaveDTO =

    OppgaveDTO(
        oppgaveId = oppgave.oppgaveId,
        behandlingId = oppgave.behandlingId,
        personIdent = oppgave.ident,
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
        saksbehandlerIdent = oppgave.saksbehandlerIdent,
        tidspunktOpprettet = oppgave.opprettet,
        emneknagger = oppgave.emneknagger.toList(),
        tilstand = oppgave.tilstand().tilOppgaveTilstandDTO(),
        journalpostIder = journalpostIder.toList(),
        utsattTilDato = oppgave.utsattTil(),
        saksbehandler = saksbehandlerDTO,
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
        is Oppgave.PaaVent -> OppgaveTilstandDTO.PAA_VENT
        else -> throw InternDataException("Ukjent tilstand: $this")
    }
}

class InternDataException(message: String) : RuntimeException(message)

internal fun Oppgave.tilOppgaveOversiktDTO() =
    OppgaveOversiktDTO(
        oppgaveId = this.oppgaveId,
        behandlingId = this.behandlingId,
        personIdent = this.ident,
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
        saksbehandlerIdent = this.saksbehandlerIdent,
        utsattTilDato = this.utsattTil(),
    )

internal fun ApplicationCall.finnUUID(pathParam: String): UUID =
    parameters[pathParam]?.let {
        UUID.fromString(it)
    } ?: throw IllegalArgumentException("Kunne ikke finne oppgaveId i path")
