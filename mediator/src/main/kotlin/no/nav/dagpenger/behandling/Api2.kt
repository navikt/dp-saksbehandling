package no.nav.dagpenger.behandling

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.api2() {

    routing {
        get("oppgaver") {
            this.call.respondText(
                contentType = ContentType.Application.Json, status = HttpStatusCode.OK
            ) {
                """
                   {} 
                """.trimIndent()
            }
        }
    }

}

