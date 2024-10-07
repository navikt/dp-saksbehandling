package no.nav.dagpenger.saksbehandling.statistikk

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.title
import kotlinx.html.ul
import no.nav.dagpenger.saksbehandling.jwt.navIdent

internal fun Application.statistikkApi(statistikkTjeneste: StatistikkTjeneste) {
    routing {
        route("public/statistikk") {
            get {
                call.respondHtml {
                    head {
                        title { +"Statistikk" }
                    }
                    body {
                        h1 { +"Statistikk" }
                        p { +"Her finner du statistikk over antall brev sendt." }
                        ul {
                            li { +"Antall brev sendt: ${statistikkTjeneste.hentAntallBrevSendt()}" }
                        }
                    }
                }
            }
        }

        authenticate("azureAd") {
            route("statistikk") {
                get {
                    val statistikk = statistikkTjeneste.hentStatistikk(call.navIdent())
                    call.respond(HttpStatusCode.OK, statistikk)
                }
            }
        }
    }
}
