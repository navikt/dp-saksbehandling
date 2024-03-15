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
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.DataType
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
import no.nav.dagpenger.saksbehandling.api.models.SokDTO
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
                    val oppgaver = mediator.hentOppgaverKlarTilBehandling().tilOppgaverDTO()
                    sikkerLogger.info { "Alle oppgaver hentes: $oppgaver" }
                    call.respond(status = HttpStatusCode.OK, oppgaver)
                }

                route("sok") {
                    post {
                        val oppgaver = mediator.finnOppgaverFor(call.receive<SokDTO>().fnr).tilOppgaverDTO()
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
                            null -> call.respond(
                                status = HttpStatusCode.NotFound,
                                message = "Fant ingen oppgave med UUID $oppgaveId",
                            )

                            else -> {
                                val message = oppgave.tilOppgaveDTO()
                                sikkerLogger.info { "Oppgave $oppgaveId skal gjøres om til OppgaveDTO: $oppgave" }
                                sikkerLogger.info { "OppgaveDTO $oppgaveId hentes: $message" }
                                call.respond(HttpStatusCode.OK, message)
                            }
                        }
                    }

                    route("avslag") {
                        put {
                            val oppgaveId = call.finnUUID("oppgaveId")
                            val saksbehandlerSignatur = call.request.jwt()
                            val bekreftOppgaveHendelse = BekreftOppgaveHendelse(oppgaveId, saksbehandlerSignatur)

                            mediator.bekreftOppgavensOpplysninger(bekreftOppgaveHendelse)
                                .onSuccess { call.respond(HttpStatusCode.NoContent) }
                                .onFailure { e ->
                                    call.respond(
                                        status = HttpStatusCode.NotFound,
                                        message = e.message.toString(),
                                    )
                                }
                        }
                    }

                    route("lukk") {
                        put {
                            val oppgaveId = call.finnUUID("oppgaveId")
                            val saksbehandlerSignatur = call.request.jwt()
                            val avbrytBehandlingHendelse = AvbrytBehandlingHendelse(oppgaveId, saksbehandlerSignatur)
                            mediator.avbrytBehandling(avbrytBehandlingHendelse)
                                .onSuccess { call.respond(HttpStatusCode.NoContent) }
                                .onFailure { e ->
                                    call.respond(
                                        status = HttpStatusCode.NotFound,
                                        message = e.message.toString(),
                                    )
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
        beskrivendeId = this.beskrivendeId,
        opplysninger = this.opplysninger.map { opplysning -> opplysning.tilOpplysningDTO() },
        tilstand = this.tilstand.tilTilstandDTO(),
    )
}

private fun Steg.Tilstand.tilTilstandDTO(): StegTilstandDTO {
    return when (this) {
        Steg.Tilstand.OPPFYLT -> StegTilstandDTO.OPPFYLT
        Steg.Tilstand.IKKE_OPPFYLT -> StegTilstandDTO.IKKE_OPPFYLT
        Steg.Tilstand.MANUELL_BEHANDLING -> StegTilstandDTO.MANUELL_BEHANDLING
    }
}

private fun Opplysning.tilOpplysningDTO(): OpplysningDTO {
    val datatype: DataTypeDTO =
        when (this.dataType) {
            DataType.Boolean -> DataTypeDTO.BOOLEAN
            DataType.LocalDate -> DataTypeDTO.LOCALDATE
            DataType.Int -> DataTypeDTO.INT
            DataType.Double -> DataTypeDTO.DOUBLE
            DataType.String -> DataTypeDTO.STRING
        }
    return OpplysningDTO(
        opplysningNavn = this.navn,
        status = when (this.status) {
            OpplysningStatus.Hypotese -> OpplysningStatusDTO.HYPOTESE
            OpplysningStatus.Faktum -> OpplysningStatusDTO.FAKTUM
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
