package no.nav.dagpenger.saksbehandling.statistikk.api

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
import no.nav.dagpenger.saksbehandling.api.models.OppgavestatistikkDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkGruppeMedAntallDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkResultatDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkResultatSerieDTO
import no.nav.dagpenger.saksbehandling.api.models.TilstandNavnDTO
import no.nav.dagpenger.saksbehandling.jwt.navIdent
import no.nav.dagpenger.saksbehandling.statistikk.ProduksjonsstatistikkFilter
import no.nav.dagpenger.saksbehandling.statistikk.db.AntallOppgaverForTilstandOgRettighet
import no.nav.dagpenger.saksbehandling.statistikk.db.AntallOppgaverForTilstandOgUtløstAv
import no.nav.dagpenger.saksbehandling.statistikk.db.ProduksjonsstatistikkRepository

internal fun Application.statistikkApi(produksjonsstatistikkRepository: ProduksjonsstatistikkRepository) {
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
                            li { +"Antall brev sendt: ${produksjonsstatistikkRepository.hentAntallBrevSendt()}" }
                        }
                    }
                }
            }
        }

        authenticate("azureAd") {
            route("statistikk") {
                get {
                    val statistikk = produksjonsstatistikkRepository.hentSaksbehandlerStatistikk(call.navIdent())
                    val generellStatistikk = produksjonsstatistikkRepository.hentAntallVedtakGjort()
                    val beholdningsinfo = produksjonsstatistikkRepository.hentBeholdningsInfo()
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
            route("produksjonsstatistikk") {
                get {
                    val produksjonsstatistikkFilter =
                        ProduksjonsstatistikkFilter.Companion.fra(
                            call.request.queryParameters,
                        )

                    if (produksjonsstatistikkFilter.grupperEtter == GrupperEtterDTO.RETTIGHETSTYPE.name) {
                        val grupper = produksjonsstatistikkRepository.hentTilstanderMedRettighetFilter(produksjonsstatistikkFilter)
                        val serier = produksjonsstatistikkRepository.hentRettigheterMedTilstandFilter(produksjonsstatistikkFilter)
                        val resultat = produksjonsstatistikkRepository.hentResultatSerierForRettigheter(produksjonsstatistikkFilter)
                        call.respond(
                            status = HttpStatusCode.OK,
                            message =
                                OppgavestatistikkDTO(
                                    grupper = grupper,
                                    serier = serier,
                                    resultat =
                                        StatistikkResultatDTO(
                                            grupper = produksjonsstatistikkFilter.rettighetstyper.map { TilstandNavnDTO(navn = it) },
                                            serier = resultat.tilDtoForRettighet(),
                                        ),
                                ),
                        )
                    } else {
                        val grupper = produksjonsstatistikkRepository.hentTilstanderMedUtløstAvFilter(produksjonsstatistikkFilter)
                        val serier = produksjonsstatistikkRepository.hentUtløstAvMedTilstandFilter(produksjonsstatistikkFilter)
                        val resultat = produksjonsstatistikkRepository.hentResultatSerierForUtløstAv(produksjonsstatistikkFilter)

                        call.respond(
                            status = HttpStatusCode.OK,
                            message =
                                OppgavestatistikkDTO(
                                    grupper = grupper,
                                    serier = serier,
                                    resultat =
                                        StatistikkResultatDTO(
                                            grupper =
                                                produksjonsstatistikkFilter.tilstander
                                                    .ifEmpty {
                                                        Oppgave.Tilstand.Type.Companion.søkbareTilstander
                                                    }.map { TilstandNavnDTO(navn = it.tilTilstandNavn()) },
                                            serier = resultat.tilDtoForUtløstAv(),
                                        ),
                                ),
                        )
                    }
                }
            }
        }
    }
}

private fun Oppgave.Tilstand.Type.tilTilstandNavn(): String {
    when (this) {
        Oppgave.Tilstand.Type.OPPRETTET -> return "Opprettet"
        Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING -> return "Klar til behandling"
        Oppgave.Tilstand.Type.UNDER_BEHANDLING -> return "Under behandling"
        Oppgave.Tilstand.Type.FERDIG_BEHANDLET -> return "Ferdig behandlet"
        Oppgave.Tilstand.Type.PAA_VENT -> return "På vent"
        Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL -> return "Klar til kontroll"
        Oppgave.Tilstand.Type.UNDER_KONTROLL -> return "Under kontroll"
        Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING -> return "Avventer lås av behandling"
        Oppgave.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING -> return "Avventer opplåsing av behandling"
        Oppgave.Tilstand.Type.AVBRUTT -> return "Avbrutt"
        Oppgave.Tilstand.Type.AVBRUTT_MASKINELT -> return "Avbrutt maskinelt"
    }
}

internal fun List<AntallOppgaverForTilstandOgRettighet>.tilDtoForRettighet(): List<StatistikkResultatSerieDTO> =
    this
        .groupBy { it.rettighet }
        .map { (rettighet, antallOppgaverForTilstandOgRettighetListe) ->
            StatistikkResultatSerieDTO(
                navn = rettighet,
                verdier =
                    antallOppgaverForTilstandOgRettighetListe.map { antallOppgaverForTilstandOgRettighet ->
                        StatistikkGruppeMedAntallDTO(
                            gruppe = antallOppgaverForTilstandOgRettighet.tilstand.tilGruppeNavn(),
                            antall = antallOppgaverForTilstandOgRettighet.antall,
                        )
                    },
            )
        }

internal fun List<AntallOppgaverForTilstandOgUtløstAv>.tilDtoForUtløstAv(): List<StatistikkResultatSerieDTO> =
    this
        .groupBy { it.utløstAv }
        .map { (utlostAv, antallOppgaverForTilstandOgUtløstAvListe) ->
            StatistikkResultatSerieDTO(
                navn = utlostAv.tilSerieNavn(),
                verdier =
                    antallOppgaverForTilstandOgUtløstAvListe.map { antallOppgaverForTilstandOgUtløstAv ->
                        StatistikkGruppeMedAntallDTO(
                            gruppe = antallOppgaverForTilstandOgUtløstAv.tilstand.tilGruppeNavn(),
                            antall = antallOppgaverForTilstandOgUtløstAv.antall,
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
