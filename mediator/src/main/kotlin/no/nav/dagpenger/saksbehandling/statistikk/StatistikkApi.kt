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
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.api.models.GrupperEtterDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2DTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2ResultatDTO
import no.nav.dagpenger.saksbehandling.api.models.V2GruppeMedAntallDTO
import no.nav.dagpenger.saksbehandling.api.models.V2SerieDTO
import no.nav.dagpenger.saksbehandling.api.models.V2StatusNavnDTO
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
                                    resultat =
                                        StatistikkV2ResultatDTO(
                                            grupper = statistikkFilter.rettighetstyper.map { V2StatusNavnDTO(navn = it) },
                                            serier = emptyList(),
//                                            statistikkV2Tjeneste.hentResultatSerierForRettigheter(statistikkFilter)
                                        ),
                                ),
                        )
                        return@get
                    }
                    val grupper = statistikkV2Tjeneste.hentTilstanderMedUtløstAvFilter(statistikkFilter)
                    val serier = statistikkV2Tjeneste.hentUtløstAvMedTilstandFilter(statistikkFilter)
                    val resultatSerierForUtløstAv = statistikkV2Tjeneste.hentResultatSerierForUtløstAv(statistikkFilter)

                    call.respond(
                        status = HttpStatusCode.OK,
                        message =
                            StatistikkV2DTO(
                                grupper = grupper,
                                serier = serier,
                                resultat =
                                    StatistikkV2ResultatDTO(
                                        grupper = statistikkFilter.rettighetstyper.map { V2StatusNavnDTO(navn = it) },
                                        serier = resultatSerierForUtløstAv.tilDto(),
                                    ),
                            ),
                    )
                }
            }
        }
    }
}

internal fun List<AntallOppgaverForTilstandOgUtløstAv>.tilDto(): List<V2SerieDTO> =
    this
        .groupBy { it.utløstAv }
        .map { (utlostAv, hubbas) ->
            V2SerieDTO(
                navn = utlostAv.tilSerieNavn(),
                verdier =
                    hubbas.map { hubba ->
                        V2GruppeMedAntallDTO(
                            gruppe = hubba.tilstand.tilGruppeNavn(),
                            antall = hubba.antall,
                        )
                    },
            )
        }

private fun Oppgave.Tilstand.Type.tilGruppeNavn(): String =
    when (this) {
        Oppgave.Tilstand.Type.OPPRETTET -> "Opprettet"
        Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING -> "Klar til behandling"
        Oppgave.Tilstand.Type.UNDER_BEHANDLING -> "Under behandling"
        Oppgave.Tilstand.Type.FERDIG_BEHANDLET -> "Ferdig behandlet"
        Oppgave.Tilstand.Type.PAA_VENT -> "På vent"
        Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL -> "Klar til kontroll"
        Oppgave.Tilstand.Type.UNDER_KONTROLL -> "Under kontroll"
        Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING -> "Avventer lås av behandling"
        Oppgave.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING -> "Avventer opplåsing av behandling"
        Oppgave.Tilstand.Type.AVBRUTT -> "Avbrutt"
        Oppgave.Tilstand.Type.AVBRUTT_MASKINELT -> "Avbrutt maskinelt"
    }

private fun UtløstAvType.tilSerieNavn(): String =
    when (this) {
        UtløstAvType.KLAGE -> "Klage"
        UtløstAvType.SØKNAD -> "Søknad"
        UtløstAvType.MELDEKORT -> "Meldekort"
        UtløstAvType.MANUELL -> "Manuell"
        UtløstAvType.INNSENDING -> "Innsending"
    }
