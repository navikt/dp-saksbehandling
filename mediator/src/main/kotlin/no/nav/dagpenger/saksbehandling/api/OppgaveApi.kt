package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Configuration.egneAnsatteADGruppe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.api.models.KjonnDTO
import no.nav.dagpenger.saksbehandling.api.models.NesteOppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonIdentDTO
import no.nav.dagpenger.saksbehandling.api.models.UtsettOppgaveDTO
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.EgneAnsatteTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.oppgaveTilgangskontroll
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdClient
import no.nav.dagpenger.saksbehandling.jwt.navIdent
import no.nav.dagpenger.saksbehandling.jwt.saksbehandler
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import java.util.UUID

private val logger = KotlinLogging.logger { }

internal fun Application.oppgaveApi(
    oppgaveMediator: OppgaveMediator,
    pdlKlient: PDLKlient,
    journalpostIdClient: JournalpostIdClient,
) {
    suspend fun oppgaveDTO(oppgave: Oppgave): OppgaveDTO =
        coroutineScope {
            val person = async { pdlKlient.person(oppgave.ident).getOrThrow() }
            val journalpostIder = async { journalpostIdClient.hentJournalPostIder(oppgave.behandling) }
            val oppgaveDTO = lagOppgaveDTO(oppgave, person.await(), journalpostIder.await())
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
                        val saksbehandler = call.principal<JWTPrincipal>()?.saksbehandler
                        var saksbehandlerTilgangEgneAnsatte = false
                        if (saksbehandler != null) {
                            saksbehandlerTilgangEgneAnsatte = saksbehandler.grupper.contains(egneAnsatteADGruppe)
                        }
                        val søkefilter = TildelNesteOppgaveFilter.fra(dto.queryParams, saksbehandlerTilgangEgneAnsatte)

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
                    val egneAnsatteTilgangskontroll =
                        EgneAnsatteTilgangskontroll(
                            tillatteGrupper = setOf(egneAnsatteADGruppe),
                            skjermesSomEgneAnsatteFun = oppgaveMediator::personSkjermesSomEgneAnsatte,
                        )
                    oppgaveTilgangskontroll(setOf(egneAnsatteTilgangskontroll)) {
                        get {
                            val oppgaveId = call.finnUUID("oppgaveId")
                            val oppgave = oppgaveMediator.hentOppgave(oppgaveId)
                            call.respond(HttpStatusCode.OK, oppgaveDTO(oppgave))
                        }
                        route("tildel") {
                            put {
                                val oppgaveAnsvarHendelse = call.oppgaveAnsvarHendelse()
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
                                val utsettOppgaveHendelse = call.utsettOppgaveHendelse()
                                logger.info("Utsetter oppgave: $utsettOppgaveHendelse")
                                oppgaveMediator.utsettOppgave(utsettOppgaveHendelse)
                                call.respond(HttpStatusCode.NoContent)
                            }
                        }
                    }
                    route("legg-tilbake") {
                        put {
                            val oppgaveAnsvarHendelse = call.oppgaveAnsvarHendelse()
                            oppgaveMediator.fristillOppgave(oppgaveAnsvarHendelse)
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
            route("behandling/{behandlingId}/oppgaveId") {
                get {
                    val behandlingId = call.finnUUID("behandlingId")
                    val oppgaveId: UUID? = oppgaveMediator.hentOppgaveIdFor(behandlingId = behandlingId)
                    when (oppgaveId) {
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

private suspend fun JournalpostIdClient.hentJournalPostIder(behandling: Behandling): Set<String> {
    return when (val hendelse = behandling.hendelse) {
        is SøknadsbehandlingOpprettetHendelse -> {
            this.hentJournalpostId(hendelse.søknadId).map {
                setOf(it)
            }.getOrElse {
                emptySet()
            }
        }

        TomHendelse -> emptySet()
    }
}

private suspend fun ApplicationCall.utsettOppgaveHendelse(): UtsettOppgaveHendelse {
    val utsettOppgaveDto = this.receive<UtsettOppgaveDTO>()

    return UtsettOppgaveHendelse(
        oppgaveId = this.finnUUID("oppgaveId"),
        navIdent = this.navIdent(),
        utsattTil = utsettOppgaveDto.utsettTilDato,
        beholdOppgave = utsettOppgaveDto.beholdOppgave,
    )
}

private fun ApplicationCall.oppgaveAnsvarHendelse(): OppgaveAnsvarHendelse =
    OppgaveAnsvarHendelse(this.finnUUID("oppgaveId"), this.navIdent())

fun lagOppgaveDTO(
    oppgave: Oppgave,
    person: PDLPersonIntern,
    journalpostIder: Set<String>,
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
            ),
        saksbehandlerIdent = oppgave.saksbehandlerIdent,
        tidspunktOpprettet = oppgave.opprettet,
        emneknagger = oppgave.emneknagger.toList(),
        tilstand = oppgave.tilstand().tilOppgaveTilstandDTO(),
        journalpostIder = journalpostIder.toList(),
        utsattTilDato = oppgave.utsattTil(),
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
        tilstand = this.tilstand().tilOppgaveTilstandDTO(),
        saksbehandlerIdent = this.saksbehandlerIdent,
        utsattTilDato = this.utsattTil(),
    )

internal fun ApplicationCall.finnUUID(pathParam: String): UUID =
    parameters[pathParam]?.let {
        UUID.fromString(it)
    } ?: throw IllegalArgumentException("Kunne ikke finne oppgaveId i path")
