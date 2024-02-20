package no.nav.dagpenger.saksbehandling.api

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
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Steg
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.auth.AzureAd
import no.nav.dagpenger.saksbehandling.api.auth.verifier
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.StegDTO
import no.nav.dagpenger.saksbehandling.api.models.StegTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.SvarDTO
import no.nav.dagpenger.saksbehandling.maskinell.BehandlingKlient
import java.time.LocalDate
import java.util.UUID

internal fun Application.oppgaveApi(
    mediator: Mediator,
    behandlingKlient: BehandlingKlient,
) {
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
        swaggerUI(path = "openapi", swaggerFile = "saksbehandling-api.yaml")

        authenticate("azureAd") {
            route("oppgave") {
                get {
                    val oppgaver = mediator.hentAlleOppgaver().tilOppgaverDTO() + oppgaveDtos
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
                        val oppgave: OppgaveDTO? =
                            when (oppgaveId) {
                                oppgaveTilBehandlingUUID -> oppgaveTilBehandlingDTO
                                oppgaveFerdigBehandletUUID -> oppgaveFerdigBehandletDTO
                                else -> mediator.hent(oppgaveId)?.tilOppgaveDTO()
                            }
                        if (oppgave == null) {
                            call.respond(
                                status = HttpStatusCode.NotFound,
                                message = "Fant ingen oppgave med UUID $oppgaveId",
                            )
                        } else {
                            val behandling = behandlingKlient.hentBehandling(oppgave.behandlingId)
                            val minsteinntektOpplysning = behandling.opplysning.findLast { it.opplysningstype == "Minsteinntekt" }
                            val alerdsKravOpplysning = behandling.opplysning.findLast { it.opplysningstype == "Oppfyller kravet til alder" }
                            val nyeSteg = mutableListOf<StegDTO>()
                            minsteinntektOpplysning?.let { minsteinntekt ->
                                val utledetOpplysninger = hentUtledetOpplysning(minsteinntekt)

                                nyeSteg.add(
                                    StegDTO(
                                        uuid = UUIDv7.ny(),
                                        stegNavn = "Har minste arbeidsinntekt",
                                        opplysninger =
                                            listOf(
                                                OpplysningDTO(
                                                    opplysningNavn = "Minsteinntekt",
                                                    opplysningType = OpplysningTypeDTO.Boolean,
                                                    svar = SvarDTO(minsteinntekt.verdi),
                                                ),
                                            ) + utledetOpplysninger,
                                    ),
                                )
                            }

                            alerdsKravOpplysning?.let { aldersKrav ->
                                val utledetOpplysninger = hentUtledetOpplysning(aldersKrav)
                                nyeSteg.add(
                                    StegDTO(
                                        uuid = UUIDv7.ny(),
                                        stegNavn = "Under 67 år",
                                        opplysninger =
                                            listOf(
                                                OpplysningDTO(
                                                    opplysningNavn = "Under 67 år",
                                                    opplysningType = OpplysningTypeDTO.Boolean,
                                                    svar = SvarDTO(aldersKrav.verdi),
                                                ),
                                            ) + utledetOpplysninger,
                                    ),
                                )
                            }

                            val oppdatertOppgave = oppgave.copy(steg = oppgave.steg + nyeSteg)

                            call.respond(HttpStatusCode.OK, oppdatertOppgave)
                        }
                    }

                    route("steg") {
                        put("{stegId}") {
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }

                    route("avslag") {
                        put {
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }

                    route("lukk") {
                        put {
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
        }
    }
}

private fun hentUtledetOpplysning(fraOpplysning: no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO) =
    fraOpplysning.utledetAv?.opplysninger?.map {
        OpplysningDTO(
            opplysningNavn = it.opplysningstype,
            opplysningType =
                when (it.datatype) {
                    "boolean" -> OpplysningTypeDTO.Boolean
                    "string" -> OpplysningTypeDTO.String
                    "double" -> OpplysningTypeDTO.Double
                    "LocalDate" -> OpplysningTypeDTO.LocalDate
                    else -> OpplysningTypeDTO.String
                },
            svar = SvarDTO(it.verdi),
        )
    } ?: emptyList()

private fun List<Oppgave>.tilOppgaverDTO(): List<OppgaveDTO> {
    return this.map { oppgave -> oppgave.tilOppgaveDTO() }
}

internal fun Oppgave.tilOppgaveDTO(): OppgaveDTO {
    return OppgaveDTO(
        oppgaveId = this.oppgaveId,
        personIdent = this.ident,
        behandlingId = this.behandlingId,
        datoOpprettet = this.opprettet.toLocalDate(),
        journalpostIder = emptyList(),
        emneknagger = this.emneknagger.toList(),
        // @TODO: Hent tilstand fra oppgave? (FerdigBehandlet, TilBehandling)
        tilstand = OppgaveTilstandDTO.TilBehandling,
        steg = this.steg.map { steg -> steg.tilStegDTO() },
    )
}

private fun Steg.tilStegDTO(): StegDTO {
    return StegDTO(
        uuid = this.stegId,
        stegNavn = this.navn,
        opplysninger = emptyList(),
        // @TODO: Hent stegtilstand fra steg?
        tilstand = StegTilstandDTO.Groenn,
    )
}

internal val oppgaveFerdigBehandletUUID = UUID.fromString("7f9c2ac7-5bf2-46e6-a618-c1f4f85cd3f2")
internal val oppgaveTilBehandlingUUID = UUID.fromString("018d7964-347c-788b-aa97-8acaba091245")
internal val stegIdGjenopptak8Uker = UUID.fromString("8d936e88-b5fe-4e6b-96de-82a341494954")
internal val opplysningerGjenopptak8uker =
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
    )

internal val oppgaveTilBehandlingDTO =
    OppgaveDTO(
        oppgaveId = oppgaveTilBehandlingUUID,
        personIdent = "12345678901",
        datoOpprettet = LocalDate.now(),
        journalpostIder = listOf("12345678"),
        emneknagger = listOf("VurderAvslagPåMinsteinntekt"),
        tilstand = OppgaveTilstandDTO.TilBehandling,
        behandlingId = UUIDv7.ny(),
        steg =
            listOf(
                StegDTO(
                    uuid = stegIdGjenopptak8Uker,
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
        oppgaveId = oppgaveFerdigBehandletUUID,
        personIdent = "12345678901",
        datoOpprettet = LocalDate.now(),
        journalpostIder = listOf("98989", "76767"),
        emneknagger = listOf("VurderAvslagPåMinsteinntekt"),
        tilstand = OppgaveTilstandDTO.FerdigBehandlet,
        behandlingId = UUIDv7.ny(),
        steg =
            listOf(
                StegDTO(
                    uuid = UUID.randomUUID(),
                    stegNavn = "Gjenopptak / 8 uker",
                    opplysninger = opplysningerGjenopptak8uker,
                    tilstand = StegTilstandDTO.Groenn,
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
