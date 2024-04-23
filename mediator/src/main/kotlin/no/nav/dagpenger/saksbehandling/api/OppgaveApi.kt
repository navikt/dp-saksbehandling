package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.api.config.apiConfig
import no.nav.dagpenger.saksbehandling.api.models.KjonnDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonDTO
import no.nav.dagpenger.saksbehandling.api.models.SokDTO
import no.nav.dagpenger.saksbehandling.db.Søkefilter
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.jwt.navIdent
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import java.util.UUID

internal fun Application.oppgaveApi(mediator: Mediator, pdlKlient: PDLKlient) {
    val sikkerLogger = KotlinLogging.logger("tjenestekall")

    apiConfig()

    routing {
        swaggerUI(path = "openapi", swaggerFile = "saksbehandling-api.yaml")

        authenticate("azureAd") {
            route("oppgave") {
                get {
                    val søkefilter = Søkefilter.fra(call.request.queryParameters, call.navIdent())

                    val oppgaver = mediator.søk(søkefilter).tilOppgaverOversiktDTO()
                    sikkerLogger.info { "Alle oppgaver hentes: $oppgaver" }
                    call.respond(status = HttpStatusCode.OK, oppgaver)
                }

                route("sok") {
                    post {
                        val oppgaver = mediator.finnOppgaverFor(call.receive<SokDTO>().fnr).tilOppgaverOversiktDTO()
                        call.respond(status = HttpStatusCode.OK, oppgaver)
                    }
                }

                route("neste") {
                    put {
                        val oppgave = mediator.hentNesteOppgavenTil(call.navIdent())
                        when (oppgave) {
                            null -> call.respond(HttpStatusCode.NotFound)
                            else -> {
                                val person = pdlKlient.person(oppgave.ident).getOrThrow()
                                val oppgaveDTO = lagOppgaveDTO(oppgave, person)
                                call.respond(HttpStatusCode.OK, oppgaveDTO)
                            }
                        }
                    }
                }

                route("{oppgaveId}") {
                    get {
                        val oppgaveId = call.finnUUID("oppgaveId")
                        val oppgave = mediator.hentOppgave(oppgaveId)
                        val person: PDLPersonIntern = pdlKlient.person(oppgave.ident).getOrThrow()
                        val oppgaveDTO = lagOppgaveDTO(oppgave, person)
                        call.respond(HttpStatusCode.OK, oppgaveDTO)
                    }

                    route("tildel") {
                        put {
                            val oppgaveAnsvarHendelse = call.oppgaveAnsvarHendelse()
                            val oppgave = mediator.tildelOppgave(oppgaveAnsvarHendelse)
                            val person = pdlKlient.person(oppgave.ident).getOrThrow()
                            val oppgaveDTO = lagOppgaveDTO(oppgave, person)
                            call.respond(HttpStatusCode.OK, oppgaveDTO)
                        }
                    }
                    route("leggTilbake") {
                        put {
                            val oppgaveAnsvarHendelse = call.oppgaveAnsvarHendelse()
                            mediator.fristillOppgave(oppgaveAnsvarHendelse)
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
            route("behandling/{behandlingId}/oppgaveId") {
                get {
                    val behandlingId = call.finnUUID("behandlingId")
                    val oppgaveId: UUID? = mediator.hentOppgaveIdFor(behandlingId = behandlingId)
                    when (oppgaveId) {
                        null -> call.respond(HttpStatusCode.NotFound)
                        else -> call.respondText(
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

private fun ApplicationCall.oppgaveAnsvarHendelse(): OppgaveAnsvarHendelse =
    OppgaveAnsvarHendelse(this.finnUUID("oppgaveId"), this.navIdent())

fun lagOppgaveDTO(oppgave: Oppgave, person: PDLPersonIntern): OppgaveDTO =

    OppgaveDTO(
        oppgaveId = oppgave.oppgaveId,
        behandlingId = oppgave.behandlingId,
        personIdent = oppgave.ident,
        person = PersonDTO(
            ident = person.ident,
            fornavn = person.fornavn,
            etternavn = person.etternavn,
            mellomnavn = person.mellomnavn,
            fodselsdato = person.fødselsdato,
            alder = person.alder,
            kjonn = when (person.kjønn) {
                PDLPerson.Kjonn.MANN -> KjonnDTO.MANN
                PDLPerson.Kjonn.KVINNE -> KjonnDTO.KVINNE
                PDLPerson.Kjonn.UKJENT -> KjonnDTO.UKJENT
            },
            statsborgerskap = person.statsborgerskap,
        ),
        tidspunktOpprettet = oppgave.opprettet,
        emneknagger = oppgave.emneknagger.toList(),
        tilstand = oppgave.tilstand.tilOppgaveTilstandDTO(),
        journalpostIder = listOf(),
    )

private fun List<Oppgave>.tilOppgaverOversiktDTO(): List<OppgaveOversiktDTO> {
    return this.map { oppgave -> oppgave.tilOppgaveOversiktDTO() }
}

private fun Type.tilOppgaveTilstandDTO() =
    when (this) {
        Type.OPPRETTET -> OppgaveTilstandDTO.OPPRETTET
        Type.UNDER_BEHANDLING -> OppgaveTilstandDTO.UNDER_BEHANDLING
        KLAR_TIL_BEHANDLING -> OppgaveTilstandDTO.KLAR_TIL_BEHANDLING
        Type.FERDIG_BEHANDLET -> OppgaveTilstandDTO.FERDIG_BEHANDLET
    }

internal fun Oppgave.tilOppgaveOversiktDTO() = OppgaveOversiktDTO(
    oppgaveId = this.oppgaveId,
    personIdent = this.ident,
    behandlingId = this.behandlingId,
    tidspunktOpprettet = this.opprettet,
    emneknagger = this.emneknagger.toList(),
    tilstand = this.tilstand.tilOppgaveTilstandDTO(),
)

internal fun ApplicationCall.finnUUID(pathParam: String): UUID =
    parameters[pathParam]?.let {
        UUID.fromString(it)
    } ?: throw IllegalArgumentException("Kunne ikke finne oppgaveId i path")

internal fun ApplicationRequest.jwt(): String =
    this.parseAuthorizationHeader().let { authHeader ->
        (authHeader as? HttpAuthHeader.Single)?.blob ?: throw IllegalArgumentException("JWT not found")
    }
