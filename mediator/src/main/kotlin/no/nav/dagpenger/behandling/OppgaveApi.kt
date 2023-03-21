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
                        Oppgave(
                            person = "123",
                            saksBehandler = "saksbehandler",
                            opprettet = LocalDate.now(),
                            hendelse = Hendelse(
                                id = UUID.randomUUID().toString(),
                                type = "type",
                                tilstand = "vet ikke",
                            ),
                            steg = listOf(
                                Steg(id = UUID.randomUUID().toString(), type = "string", tilstand = "vet ikke"),
                                Steg(id = UUID.randomUUID().toString(), type = "string", tilstand = "vet ikke"),
                                Steg(id = UUID.randomUUID().toString(), type = "string", tilstand = "vet ikke"),
                                Steg(id = UUID.randomUUID().toString(), type = "string", tilstand = "vet ikke"),
                            ),
                        ),
                        Oppgave(
                            person = "123",
                            saksBehandler = "saksbehandler",
                            opprettet = LocalDate.now(),
                            hendelse = Hendelse(
                                id = UUID.randomUUID().toString(),
                                type = "type",
                                tilstand = "vet ikke",
                            ),
                            steg = listOf(
                                Steg(id = UUID.randomUUID().toString(), type = "string", tilstand = "vet ikke"),
                                Steg(id = UUID.randomUUID().toString(), type = "string", tilstand = "vet ikke"),
                                Steg(id = UUID.randomUUID().toString(), type = "string", tilstand = "vet ikke"),
                                Steg(id = UUID.randomUUID().toString(), type = "string", tilstand = "vet ikke"),
                            ),
                        ),
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
                            saksBehandler = "saksbehandler",
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

internal data class Svar(
    val svar: String,
    val begrunnelse: String?,
)

internal data class Oppgave(
    val person: String,
    val saksBehandler: String,
    val opprettet: LocalDate,
    val hendelse: Hendelse,
    val steg: List<Steg>,
)

internal data class Steg(
    val id: String,
    val type: String,
    val tilstand: String,
)

internal data class Hendelse(
    val id: String,
    val type: String,
    val tilstand: String,
)
