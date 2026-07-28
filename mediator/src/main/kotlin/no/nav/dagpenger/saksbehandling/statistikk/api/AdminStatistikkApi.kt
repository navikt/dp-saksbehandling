package no.nav.dagpenger.saksbehandling.statistikk.api

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.p
import kotlinx.html.title
import no.nav.dagpenger.saksbehandling.Configuration

internal fun Application.adminStatistikkApi() {
    routing {
        if (Configuration.isDev) {
            route("/statistikk/admin") { adminRoutes() }
        } else {
            authenticate("azureAd-stsb-admin") {
                route("/statistikk/admin") { adminRoutes() }
            }
        }
    }
}

private fun io.ktor.server.routing.Route.adminRoutes() {
    get {
        call.respondHtml {
            head { title { +"Admin – Statistikk" } }
            body {
                h1 { +"Admin – Statistikk" }
                p { +"Hello world" }
            }
        }
    }
}
