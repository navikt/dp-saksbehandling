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

fun Application.oppgaveApi() {
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
                this.call.respond(
                    listOf(
                        oppgave(),
                    ),
                )
            }
            route("{oppgaveId}") {
                get() {
                    val oppgaveId = this.call.parameters["oppgaveId"]
                    require(oppgaveId != null)

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = Oppgave(
                            person = "123",
                            saksbehandler = "saksbehandler",
                            opprettet = LocalDate.now(),
                            hendelse = Hendelse(id = "", type = "", tilstand = ""),
                            steg = listOf(),
                        ),
                    )
                }
                route("steg") {
                    put("{stegId}") {
                        val oppgaveId = this.call.parameters["oppgaveId"]
                        val stegId = this.call.parameters["stegId"]
                        require(oppgaveId != null && stegId != null)
                        val svar: Svar = this.call.receive()
                        call.respond(status = HttpStatusCode.OK, message = "")
                    }
                }
            }
        }
    }
}

private fun oppgave() = Oppgave(
    person = "123",
    saksbehandler = "saksbehandler",
    opprettet = LocalDate.now(),
    hendelse = Hendelse(
        id = UUID.randomUUID().toString(),
        type = "type",
        tilstand = "vet ikke",
    ),
    steg = listOf(
        Steg(
            uuid = UUID.randomUUID(),
            id = "Fødselsdato",
            type = Stegtype.Fastsetting,
            tilstand = Tilstand.Utført,
            svartype = Svartype.Localdate,
            svar = Svar(
                LocalDate.now().minusYears(44).toString(),
                Svartype.Localdate,
                Begrunnelse("pdl", "Henta fra PDL"),
            ),
        ),
        Steg(
            uuid = UUID.randomUUID(),
            id = "Alder",
            type = Stegtype.Fastsetting,
            tilstand = Tilstand.IkkeUtført,
            svartype = Svartype.Int,
        ),
        Steg(
            uuid = UUID.randomUUID(),
            id = "Vilkår67",
            type = Stegtype.Vilkår,
            tilstand = Tilstand.IkkeUtført,
            svartype = Svartype.Boolean,
        ),
        Steg(
            uuid = UUID.randomUUID(),
            id = "Virkningstidspunkt",
            type = Stegtype.Fastsetting,
            tilstand = Tilstand.IkkeUtført,
            svartype = Svartype.Localdate,
        ),
        Steg(
            uuid = UUID.randomUUID(),
            id = "Verneplikt",
            type = Stegtype.Fastsetting,
            tilstand = Tilstand.IkkeUtført,
            svartype = Svartype.Boolean,
        ),
        Steg(
            uuid = UUID.randomUUID(),
            id = "KravTilMinsteinntekt",
            type = Stegtype.Vilkår,
            tilstand = Tilstand.Utført,
            svartype = Svartype.Boolean,
            svar = Svar(
                "true",
                type = Svartype.Boolean,
                begrunnelse = Begrunnelse(
                    "saksbehandler",
                    "Jeg bestemmer!",
                ),
            ),
        ),
        Steg(
            uuid = UUID.randomUUID(),
            id = "FastsattVanligArbeidstid",
            type = Stegtype.Fastsetting,
            tilstand = Tilstand.IkkeUtført,
            svartype = Svartype.Int,
        ),
        Steg(
            uuid = UUID.randomUUID(),
            id = "OppfyllerKravTilTaptArbeidstid",
            type = Stegtype.Vilkår,
            tilstand = Tilstand.IkkeUtført,
            svartype = Svartype.Boolean,
        ),
    ),
)

internal data class Svar(
    val svar: String,
    val type: Svartype,
    val begrunnelse: Begrunnelse,
)

internal data class Begrunnelse(
    val kilde: String, // quiz, saksbehandler, dingsebomsA
    val tekst: String,
)

internal enum class Svartype {
    String,
    Localdate,
    Int,
    Boolean,
}

internal data class Oppgave(
    val person: String,
    val saksbehandler: String,
    val opprettet: LocalDate,
    val hendelse: Hendelse,
    val steg: List<Steg>,
)

internal data class Steg(
    val uuid: UUID,
    val id: String, // reell arbeidssøker, vurder minsteinntekt, fastsett virkningstidspunkt, fastsett vanlig arbeidstid
    val type: Stegtype,
    val svartype: Svartype,
    val tilstand: Tilstand,
    val svar: Svar? = null,
)

internal enum class Stegtype {
    Fastsetting,
    Vilkår,
}

internal enum class Tilstand {
    Utført,
    MåGodkjennes,
    IkkeUtført,
}

internal data class Hendelse(
    val id: String,
    val type: String,
    val tilstand: String,
)
