package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Opplysning
import no.nav.dagpenger.saksbehandling.OpplysningStatus
import no.nav.dagpenger.saksbehandling.Steg
import no.nav.dagpenger.saksbehandling.api.config.apiConfig
import no.nav.dagpenger.saksbehandling.api.models.DataTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningStatusDTO
import no.nav.dagpenger.saksbehandling.api.models.StegDTO
import no.nav.dagpenger.saksbehandling.api.models.StegTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.SvarDTO
import java.util.UUID

internal fun Application.oppgaveApi(mediator: Mediator) {
    val sikkerLogger = KotlinLogging.logger("tjenestekall")

    apiConfig()

    routing {
        swaggerUI(path = "openapi", swaggerFile = "saksbehandling-api.yaml")

        authenticate("azureAd") {
            route("oppgave") {
                get {
                    val oppgaver = mediator.hentOppgaverKlarTilBehandling().tilOppgaverDTO() + oppgaveDtos
                    sikkerLogger.info { "Alle oppgaver hentes: $oppgaver" }
                    call.respond(status = HttpStatusCode.OK, oppgaver)
                }

                route("sok") {
                    post {
                        val oppgaver = oppgaveDtos
                        call.respond(status = HttpStatusCode.OK, oppgaver)
                    }
                }

                route("{oppgaveId}") {
                    get {
                        val oppgaveId = call.finnUUID("oppgaveId")
                        val saksbehandlerSignatur = call.request.jwt()
                        val oppdaterOppgaveHendelse = OppdaterOppgaveHendelse(oppgaveId, saksbehandlerSignatur)
                        val oppgave = mediator.oppdaterOppgaveMedSteg(oppdaterOppgaveHendelse)
                        when (oppgave) {
                            null -> {
                                when (oppgaveId) {
                                    minsteinntektOppgaveTilBehandlingId ->
                                        call.respond(
                                            HttpStatusCode.OK,
                                            minsteinntektOppgaveTilBehandling,
                                        )

                                    minsteinntektOppgaveFerdigBehandletId ->
                                        call.respond(
                                            HttpStatusCode.OK,
                                            minsteinntektOppgaveFerdigBehandlet,
                                        )

                                    else ->
                                        call.respond(
                                            status = HttpStatusCode.NotFound,
                                            message = "Fant ingen oppgave med UUID $oppgaveId",
                                        )
                                }
                            }

                            else -> {
                                val message = oppgave.tilOppgaveDTO()
                                sikkerLogger.info { "Oppgave $oppgaveId skal gjøres om til OppgaveDTO: $oppgave" }
                                sikkerLogger.info { "OppgaveDTO $oppgaveId hentes: $message" }
                                call.respond(HttpStatusCode.OK, message)
                            }
                        }
                    }

                    put {
                        call.respond(HttpStatusCode.NoContent)
                    }

                    route("avslag") {
                        put {
                            val oppgaveId = call.finnUUID("oppgaveId")
                            val saksbehandlerSignatur = call.request.jwt()
                            val bekreftOppgaveHendelse = BekreftOppgaveHendelse(oppgaveId, saksbehandlerSignatur)
                            val oppgave = mediator.bekreftOppgavensOpplysninger(bekreftOppgaveHendelse)
                            when (oppgave) {
                                null ->
                                    call.respond(
                                        status = HttpStatusCode.NotFound,
                                        message = "Fant ingen oppgave med UUID $oppgaveId",
                                    )
                                else -> call.respond(HttpStatusCode.NoContent)
                            }
                        }
                    }

                    route("lukk") {
                        put {
                            val oppgaveId = call.finnUUID("oppgaveId")
                            val saksbehandlerSignatur = call.request.jwt()
                            val avbrytBehandlingHendelse = AvbrytBehandlingHendelse(oppgaveId, saksbehandlerSignatur)
                            val oppgave = mediator.avbrytBehandling(avbrytBehandlingHendelse)
                            when (oppgave) {
                                null ->
                                    call.respond(
                                        status = HttpStatusCode.NotFound,
                                        message = "Fant ingen oppgave med UUID $oppgaveId",
                                    )
                                else -> call.respond(HttpStatusCode.NoContent)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun List<Oppgave>.tilOppgaverDTO(): List<OppgaveDTO> {
    return this.map { oppgave -> oppgave.tilOppgaveDTO() }
}

private fun Oppgave.Tilstand.Type.toOppgaveTilstandDTO() =
    when (this) {
        Oppgave.Tilstand.Type.OPPRETTET -> OppgaveTilstandDTO.OPPRETTET
        Oppgave.Tilstand.Type.FERDIG_BEHANDLET -> OppgaveTilstandDTO.FERDIG_BEHANDLET
        Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING -> OppgaveTilstandDTO.KLAR_TIL_BEHANDLING
    }

internal fun Oppgave.tilOppgaveDTO(): OppgaveDTO {
    return OppgaveDTO(
        oppgaveId = this.oppgaveId,
        personIdent = this.ident,
        behandlingId = this.behandlingId,
        tidspunktOpprettet = this.opprettet,
        journalpostIder = emptyList(),
        emneknagger = this.emneknagger.toList(),
        tilstand = this.tilstand.toOppgaveTilstandDTO(),
        steg = this.steg.map { steg -> steg.tilStegDTO() },
    )
}

internal fun Steg.tilStegDTO(): StegDTO {
    return StegDTO(
        stegNavn = this.navn,
        opplysninger = this.opplysninger.map { opplysning -> opplysning.tilOpplysningDTO() },
        // @TODO: Hent stegtilstand fra steg?
        tilstand = StegTilstandDTO.Groenn,
    )
}

private fun Opplysning.tilOpplysningDTO(): OpplysningDTO {
    val datatype: DataTypeDTO =
        when (this.dataType) {
            "boolean" -> DataTypeDTO.Boolean
            "LocalDate" -> DataTypeDTO.LocalDate
            "int" -> DataTypeDTO.Int
            "double" -> DataTypeDTO.Double
            else -> DataTypeDTO.String
        }
    return OpplysningDTO(
        opplysningNavn = this.navn,
        status = when (this.status) {
            OpplysningStatus.Hypotese -> OpplysningStatusDTO.Hypotese
            OpplysningStatus.Faktum -> OpplysningStatusDTO.Faktum
        },
        dataType = datatype,
        svar = SvarDTO(this.verdi),
    )
}

internal fun ApplicationCall.finnUUID(pathParam: String): UUID =
    parameters[pathParam]?.let {
        UUID.fromString(it)
    } ?: throw IllegalArgumentException("Kunne ikke finne oppgaveId i path")

internal fun ApplicationRequest.jwt(): String =
    this.parseAuthorizationHeader().let { authHeader ->
        (authHeader as? HttpAuthHeader.Single)?.blob ?: throw IllegalArgumentException("JWT not found")
    }
