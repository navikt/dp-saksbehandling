package no.nav.dagpenger.behandling.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.behandling.Mediator
import no.nav.dagpenger.behandling.api.auth.AzureAd
import no.nav.dagpenger.behandling.api.auth.verifier
import no.nav.dagpenger.behandling.api.models.OppgaveDTO
import no.nav.dagpenger.behandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.behandling.api.models.OpplysningDTO
import no.nav.dagpenger.behandling.api.models.OpplysningTypeDTO
import no.nav.dagpenger.behandling.api.models.StegDTO
import no.nav.dagpenger.behandling.api.models.StegTilstandDTO
import no.nav.dagpenger.behandling.api.models.SvarDTO
import java.time.LocalDate
import java.util.UUID

internal fun Application.oppgaveApi(mediator: Mediator) {
    install(CallLogging) {
        disableDefaultColors()
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            enable(SerializationFeature.INDENT_OUTPUT)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }

    install(Authentication) {
        jwt("azureAd") {
            verifier(AzureAd)
            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }
    }

    routing {
        swaggerUI(path = "openapi", swaggerFile = "behandling-api.yaml")

        authenticate("azureAd") {
            route("oppgave") {
                get {
                    val oppgaver = oppgaveDtos
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
                        try {
                            when (oppgaveId) {
                                oppgaveTilBehandlingUUID -> call.respond(HttpStatusCode.OK, oppgaveTilBehandlingDTO)
                                oppgaveFerdigBehandletUUID -> call.respond(HttpStatusCode.OK, oppgaveFerdigBehandletDTO)
                                else -> throw NoSuchElementException()
                            }
                        } catch (e: NoSuchElementException) {
                            call.respond(
                                status = HttpStatusCode.NotFound,
                                message = "Fant ingen oppgave med UUID $oppgaveId",
                            )
                        }
                    }

                    route("steg") {
                        put("{stegId}") {
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
        }
    }
}

internal val oppgaveFerdigBehandletUUID = UUID.fromString("7f9c2ac7-5bf2-46e6-a618-c1f4f85cd3f2")
internal val oppgaveTilBehandlingUUID = UUID.fromString("018d7964-347c-788b-aa97-8acaba091245")

internal val oppgaveTilBehandlingDTO =
    OppgaveDTO(
        uuid = oppgaveTilBehandlingUUID,
        personIdent = "12345678901",
        datoOpprettet = LocalDate.now(),
        journalpostIder = listOf("12345678"),
        emneknagger = listOf("VurderAvslagPåMinsteinntekt"),
        tilstand = OppgaveTilstandDTO.TilBehandling,
        steg =
            listOf(
                StegDTO(
                    uuid = UUID.randomUUID(),
                    stegNavn = "Gjenopptak / 8 uker",
                    opplysninger =
                        listOf(
                            OpplysningDTO(
                                opplysningNavn = "Mulig gjenopptak",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = null,
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Har hatt lukkede saker siste 8 uker",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = null,
                            ),
                        ),
                ),
                StegDTO(
                    uuid = UUID.randomUUID(),
                    stegNavn = "Minste arbeidsinntekt",
                    opplysninger =
                        listOf(
                            OpplysningDTO(
                                opplysningNavn = "EØS-arbeid",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = null,
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Jobb utenfor Norge",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = null,
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Svangerskapsrelaterte sykepenger",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = null,
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Det er inntekt neste kalendermåned",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = null,
                            ),
                        ),
                    tilstand = null,
                ),
            ),
    )

internal val oppgaveFerdigBehandletDTO =
    OppgaveDTO(
        uuid = oppgaveFerdigBehandletUUID,
        personIdent = "12345678901",
        datoOpprettet = LocalDate.now(),
        journalpostIder = listOf("98989", "76767"),
        emneknagger = listOf("VurderAvslagPåMinsteinntekt"),
        tilstand = OppgaveTilstandDTO.FerdigBehandlet,
        steg =
            listOf(
                StegDTO(
                    uuid = UUID.randomUUID(),
                    stegNavn = "Gjenopptak / 8 uker",
                    opplysninger =
                        listOf(
                            OpplysningDTO(
                                opplysningNavn = "Mulig gjenopptak",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = SvarDTO("false"),
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Har hatt lukkede saker siste 8 uker",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = SvarDTO("false"),
                            ),
                        ),
                ),
                StegDTO(
                    uuid = UUID.randomUUID(),
                    stegNavn = "Minste arbeidsinntekt",
                    opplysninger =
                        listOf(
                            OpplysningDTO(
                                opplysningNavn = "EØS-arbeid",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = SvarDTO("false"),
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Jobb utenfor Norge",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = SvarDTO("false"),
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Svangerskapsrelaterte sykepenger",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = SvarDTO("false"),
                            ),
                            OpplysningDTO(
                                opplysningNavn = "Det er inntekt neste kalendermåned",
                                opplysningType = OpplysningTypeDTO.Boolean,
                                svar = SvarDTO("false"),
                            ),
                        ),
                    tilstand = StegTilstandDTO.Groenn,
                ),
            ),
    )

internal val oppgaveDtos = listOf(oppgaveTilBehandlingDTO, oppgaveFerdigBehandletDTO)

internal fun ApplicationCall.finnUUID(pathParam: String): UUID =
    parameters[pathParam]?.let {
        UUID.fromString(it)
    } ?: throw IllegalArgumentException("Kunne ikke finne oppgaveId i path")

internal fun ApplicationCall.saksbehandlerId() =
    this.authentication.principal<JWTPrincipal>()?.payload?.claims?.get("NAVident")?.asString()
        ?: throw IllegalArgumentException("Ikke autentisert")

/*
internal fun ApplicationCall.roller(): List<Rolle> {
    return this.authentication.principal<JWTPrincipal>()?.payload?.claims?.get("groups")?.asList(String::class.java)
        ?.mapNotNull {
            when (it) {
                beslutterGruppe -> Rolle.Beslutter
                saksbehandlerGruppe -> Rolle.Saksbehandler
                else -> null
            }
        } ?: emptyList()
}*/

internal fun ApplicationRequest.jwt(): String =
    this.parseAuthorizationHeader().let { authHeader ->
        (authHeader as? HttpAuthHeader.Single)?.blob ?: throw IllegalArgumentException("JWT not found")
    }
