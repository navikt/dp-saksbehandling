package no.nav.dagpenger.behandling

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.behandling.hendelser.BehandlingSvar
import java.time.LocalDate
import java.util.*

fun Application.behandlingApi(mediator: Mediator) {
    install(CallLogging) { }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            enable(SerializationFeature.INDENT_OUTPUT)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    routing {
        route("behandlinger") {
            get {
                val behandlinger = mediator.hentBehandlinger().toBehandlingerDTO()
                this.call.respond(behandlinger)
            }
            route("{behandlingId}") {
                get() {
                    val behandlingId = this.call.parameters["behandlingId"]
                    require(behandlingId != null)
                    val behandling: BehandlingDTO =
                        mediator.hentBehandling(UUID.fromString(behandlingId)).toBehandlingDTO()

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = behandling,
                    )
                }
                route("steg") {
                    put("{stegId}") {
                        val behandlingId = this.call.finnUUID("behandlingId")
                        val stegId = this.call.finnUUID("stegId")
                        val svar: SvarDTO = this.call.receive()

                        when (svar.type) {
                            SvartypeDTO.String -> mediator.behandle(
                                BehandlingSvar(
                                    "123",
                                    behandlingId,
                                    stegId,
                                    svar.svar,
                                ),
                            )

                            SvartypeDTO.LocalDate -> mediator.behandle(
                                BehandlingSvar(
                                    "123",
                                    behandlingId,
                                    stegId,
                                    LocalDate.parse(svar.svar),
                                ),
                            )

                            SvartypeDTO.Int -> mediator.behandle(
                                BehandlingSvar(
                                    "123",
                                    behandlingId,
                                    stegId,
                                    svar.svar.toInt(),
                                ),
                            )

                            SvartypeDTO.Boolean -> mediator.behandle(
                                BehandlingSvar(
                                    "123",
                                    behandlingId,
                                    stegId,
                                    svar.svar.toBoolean(),
                                ),
                            )
                        }

                        call.respond(status = HttpStatusCode.OK, message = "")
                    }
                }
            }
        }
    }
}

internal fun ApplicationCall.finnUUID(pathParam: String): UUID = parameters[pathParam]?.let {
    UUID.fromString(it)
} ?: throw IllegalArgumentException("Kunne ikke finne behandlingId i path")

internal fun Behandling.toBehandlingDTO(): BehandlingDTO {
    return BehandlingDTO(
        uuid = this.uuid,
        person = this.person.ident,
        saksbehandler = null,
        opprettet = this.opprettet.toLocalDate(),
        hendelse = emptyList(),
        steg = this.alleSteg().toStegDTO(),
    )
}

internal fun Collection<Behandling>.toBehandlingerDTO() = this.map { it.toBehandlingDTO() }

internal fun Collection<Steg<*>>.toStegDTO(): List<StegDTO> = this.map { it.toStegDTO() }

internal fun Steg<*>.toStegDTO(): StegDTO {
    val stegtypeDTO = when (this) {
        is Steg.Fastsettelse<*> -> StegtypeDTO.Fastsetting
        is Steg.Vilkår -> StegtypeDTO.Vilkår
    }
    val tilstand = this.tilstand
    val svarDTO = this.svar.toSvarDTO()
    return StegDTO(
        uuid = this.uuid,
        id = this.id,
        type = stegtypeDTO,
        svartype = svarDTO.type,
        tilstand = tilstand,
        svar = svarDTO,
    )
}

internal fun Svar<*>.toSvarDTO(): SvarDTO {
    val type = when (clazz.simpleName) {
        "Integer" -> SvartypeDTO.Int
        else -> SvartypeDTO.valueOf(clazz.simpleName.replaceFirstChar { it.uppercase() })
    }
    return SvarDTO(
        svar = this.verdi.toString(),
        type = type,
        begrunnelse = BegrunnelseDTO(kilde = "", tekst = ""),
    )
}

internal data class SvarDTO(
    val svar: String,
    val type: SvartypeDTO,
    val begrunnelse: BegrunnelseDTO,
) {

    fun toSvar(): Svar<*> {
        return when (this.type) {
            SvartypeDTO.String -> Svar(verdi = svar, String::class.java)
            SvartypeDTO.LocalDate -> Svar(verdi = LocalDate.parse(svar), LocalDate::class.java)
            SvartypeDTO.Int -> Svar<Int>(verdi = svar.toInt(), Int::class.java)
            SvartypeDTO.Boolean -> Svar<Boolean>(verdi = svar.toBoolean(), Boolean::class.java)
        }
    }
}

internal data class BegrunnelseDTO(
    val kilde: String, // quiz, saksbehandler, dingsebomsA
    val tekst: String,
)

internal enum class SvartypeDTO {
    String,
    LocalDate,
    Int,
    Boolean,
}

internal data class BehandlingDTO(
    val uuid: UUID,
    val person: String,
    val saksbehandler: String?,
    val opprettet: LocalDate,
    val hendelse: List<HendelseDTO>,
    val steg: List<StegDTO>,
)

internal data class StegDTO(
    val uuid: UUID,
    val id: String, // reell arbeidssøker, vurder minsteinntekt, fastsett virkningstidspunkt, fastsett vanlig arbeidstid
    val type: StegtypeDTO,
    val svartype: SvartypeDTO,
    val tilstand: Tilstand,
    val svar: SvarDTO? = null,
)

internal enum class StegtypeDTO {
    Fastsetting,
    Vilkår,
}

internal data class HendelseDTO(
    val id: String,
    val type: String,
    val tilstand: String,
)
