package no.nav.dagpenger.saksbehandling.api.auth

import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

internal fun Route.adminApi() {
    authenticate("admin") {
        route("/admin") {
            route(path = "/ping") {
                get {
                    call.respondText("pong")
                }
            }
        }
    }
}
