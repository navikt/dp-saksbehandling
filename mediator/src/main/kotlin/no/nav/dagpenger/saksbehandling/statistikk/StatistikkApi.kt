package no.nav.dagpenger.saksbehandling.statistikk

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
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
import no.nav.dagpenger.saksbehandling.api.models.GrupperEtterDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2DTO
import no.nav.dagpenger.saksbehandling.jwt.navIdent

internal fun Application.statistikkApi(
    statistikkTjeneste: StatistikkTjeneste,
    statistikkV2Tjeneste: StatistikkV2Tjeneste,
) {
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
                    val statistikk = statistikkTjeneste.hentSaksbehandlerStatistikk(call.navIdent())
                    val generellStatistikk = statistikkTjeneste.hentAntallVedtakGjort()
                    val beholdningsinfo = statistikkTjeneste.hentBeholdningsInfo()
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "individuellStatistikk" to statistikk,
                            "generellStatistikk" to generellStatistikk,
                            "beholdningsinfo" to beholdningsinfo,
                        ),
                    )
                }
            }
            route("v2/statistikk") {
                get {
                    val statistikkFilter =
                        StatistikkFilter.fra(
                            call.request.queryParameters,
                        )

                    if (statistikkFilter.grupperEtter == GrupperEtterDTO.RETTIGHETSTYPE.name) {
                        val grupper = statistikkV2Tjeneste.hentTilstanderMedRettighetFilter(statistikkFilter)
                        val serier = statistikkV2Tjeneste.hentRettigheterMedTilstandFilter(statistikkFilter)
                        call.respond(
                            status = HttpStatusCode.OK,
                            message =
                                StatistikkV2DTO(
                                    grupper = grupper,
                                    serier = serier,
                                    // TODO: hent resultat
                                    resultat = emptyList(),
                                ),
                        )
                        return@get
                    }
                    val grupper = statistikkV2Tjeneste.hentTilstanderMedUtløstAvFilter(statistikkFilter)
                    val serier = statistikkV2Tjeneste.hentUtløstAvMedTilstandFilter(statistikkFilter)

                    call.respond(
                        status = HttpStatusCode.OK,
                        message =
                            StatistikkV2DTO(
                                grupper = grupper,
                                serier = serier,
                                // TODO: hent resultat
                                resultat = emptyList(),
                            ),
                    )
                }
            }
        }
    }
}
