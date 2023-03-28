package no.nav.dagpenger.behandling

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.time.LocalDate
import java.util.UUID

fun Application.oppgaveApi(mediator: Mediator) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    routing {
        route("oppgaver") {
            get {
                val oppgaver = mediator.hentBehandlinger().toOppgaverDTO()
                this.call.respond(oppgaver)
            }
            route("{oppgaveId}") {
                get() {
                    val oppgaveId = this.call.parameters["oppgaveId"]
                    require(oppgaveId != null)
                    val oppgave: OppgaveDTO = mediator.hentBehandling(oppgaveId).toOppgaveDTO()

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = oppgave,
                    )
                }
                route("steg") {
                    put("{stegId}") {
                        try {
                            val oppgaveId = this.call.parameters["oppgaveId"]
                            val stegId = this.call.parameters["stegId"]
                            require(oppgaveId != null && stegId != null)
                            val svar: SvarDTO = this.call.receive()
                            call.respond(status = HttpStatusCode.OK, message = "")
                        } catch (e: Exception) {
                            println(e)
                        }
                    }
                }
            }
        }
    }
}

internal fun Behandling.toOppgaveDTO(): OppgaveDTO {
    return OppgaveDTO(
        person = this.person.ident,
        saksbehandler = null,
        opprettet = this.opprettet.toLocalDate(),
        hendelse = emptyList(),
        steg = this.alleSteg().toStegDTO(),
    )
}

internal fun Collection<Behandling>.toOppgaverDTO() = this.map { it.toOppgaveDTO() }

internal fun Collection<Steg<*>>.toStegDTO(): List<StegDTO> = this.map { it.toStegDTO() }

internal fun Steg<*>.toStegDTO(): StegDTO {
    val stegtypeDTO = when (this) {
        is Steg.Fastsettelse<*> -> StegtypeDTO.Fastsetting
        is Steg.Vilkår -> StegtypeDTO.Vilkår
    }
    val tilstandDTO = when (this.svar.ubesvart) {
        true -> TilstandDTO.IkkeUtført
        false -> TilstandDTO.Utført
    }
    val svarDTO = this.svar.toSvarDTO()
    return StegDTO(
        uuid = UUID.randomUUID(),
        id = this.id,
        type = stegtypeDTO,
        svartype = svarDTO.type,
        tilstand = tilstandDTO,
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

private fun oppgave() = OppgaveDTO(
    person = "123",
    saksbehandler = "saksbehandler",
    opprettet = LocalDate.now(),
    hendelse = listOf(
        HendelseDTO(
            id = UUID.randomUUID().toString(),
            type = "type",
            tilstand = "vet ikke",
        ),
    ),
    steg = listOf(
        StegDTO(
            uuid = UUID.randomUUID(),
            id = "Fødselsdato",
            type = StegtypeDTO.Fastsetting,
            tilstand = TilstandDTO.Utført,
            svartype = SvartypeDTO.LocalDate,
            svar = SvarDTO(
                LocalDate.now().minusYears(44).toString(),
                SvartypeDTO.LocalDate,
                BegrunnelseDTO("pdl", "Henta fra PDL"),
            ),
        ),
        StegDTO(
            uuid = UUID.randomUUID(),
            id = "Alder",
            type = StegtypeDTO.Fastsetting,
            tilstand = TilstandDTO.IkkeUtført,
            svartype = SvartypeDTO.Int,
        ),
        StegDTO(
            uuid = UUID.randomUUID(),
            id = "Vilkår67",
            type = StegtypeDTO.Vilkår,
            tilstand = TilstandDTO.IkkeUtført,
            svartype = SvartypeDTO.Boolean,
        ),
        StegDTO(
            uuid = UUID.randomUUID(),
            id = "Virkningstidspunkt",
            type = StegtypeDTO.Fastsetting,
            tilstand = TilstandDTO.IkkeUtført,
            svartype = SvartypeDTO.LocalDate,
        ),
        StegDTO(
            uuid = UUID.randomUUID(),
            id = "Verneplikt",
            type = StegtypeDTO.Fastsetting,
            tilstand = TilstandDTO.IkkeUtført,
            svartype = SvartypeDTO.Boolean,
        ),
        StegDTO(
            uuid = UUID.randomUUID(),
            id = "KravTilMinsteinntekt",
            type = StegtypeDTO.Vilkår,
            tilstand = TilstandDTO.Utført,
            svartype = SvartypeDTO.Boolean,
            svar = SvarDTO(
                "true",
                type = SvartypeDTO.Boolean,
                begrunnelse = BegrunnelseDTO(
                    "saksbehandler",
                    "Jeg bestemmer!",
                ),
            ),
        ),
        StegDTO(
            uuid = UUID.randomUUID(),
            id = "FastsattVanligArbeidstid",
            type = StegtypeDTO.Fastsetting,
            tilstand = TilstandDTO.IkkeUtført,
            svartype = SvartypeDTO.Int,
        ),
        StegDTO(
            uuid = UUID.randomUUID(),
            id = "OppfyllerKravTilTaptArbeidstid",
            type = StegtypeDTO.Vilkår,
            tilstand = TilstandDTO.IkkeUtført,
            svartype = SvartypeDTO.Boolean,
        ),
    ),
)

internal data class SvarDTO(
    val svar: String,
    val type: SvartypeDTO,
    val begrunnelse: BegrunnelseDTO,
)

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

internal data class OppgaveDTO(
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
    val tilstand: TilstandDTO,
    val svar: SvarDTO? = null,
)

internal enum class StegtypeDTO {
    Fastsetting,
    Vilkår,
}

internal enum class TilstandDTO {
    Utført,
    MåGodkjennes,
    IkkeUtført,
}

internal data class HendelseDTO(
    val id: String,
    val type: String,
    val tilstand: String,
)
