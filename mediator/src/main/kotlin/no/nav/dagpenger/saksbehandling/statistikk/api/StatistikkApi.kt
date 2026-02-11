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
import no.nav.dagpenger.saksbehandling.api.models.ProduksjonsstatistikkDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkGruppeDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkGruppeMedAntallDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkResultatDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkResultatSerieDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkSerieDTO
import no.nav.dagpenger.saksbehandling.api.models.TilstandNavnDTO
import no.nav.dagpenger.saksbehandling.jwt.navIdent
import no.nav.dagpenger.saksbehandling.statistikk.ProduksjonsstatistikkFilter
import no.nav.dagpenger.saksbehandling.statistikk.db.AntallOppgaverForRettighet
import no.nav.dagpenger.saksbehandling.statistikk.db.AntallOppgaverForTilstandOgRettighet
import no.nav.dagpenger.saksbehandling.statistikk.db.AntallOppgaverForTilstandOgUtløstAv
import no.nav.dagpenger.saksbehandling.statistikk.db.AntallOppgaverForUtløstAv
import no.nav.dagpenger.saksbehandling.statistikk.db.ProduksjonsstatistikkRepository
import no.nav.dagpenger.saksbehandling.statistikk.db.TilstandStatistikk

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
                        val tilstandStatistikker =
                            produksjonsstatistikkRepository.hentTilstanderMedRettighetFilter(produksjonsstatistikkFilter)
                        val antallOppgaverForRettighet =
                            produksjonsstatistikkRepository.hentRettigheterMedTilstandFilter(produksjonsstatistikkFilter)
                        val resultat =
                            produksjonsstatistikkRepository.hentResultatSerierForRettigheter(produksjonsstatistikkFilter)
                        call.respond(
                            status = HttpStatusCode.OK,
                            message =
                                ProduksjonsstatistikkDTO(
                                    grupper = tilstandStatistikker.tilStatistikkGruppeDTO(),
                                    serier = antallOppgaverForRettighet.tilStatistikkSerieDTOForRettighet(),
                                    resultat =
                                        StatistikkResultatDTO(
                                            grupper =
                                                produksjonsstatistikkFilter.tilstander
                                                    .ifEmpty {
                                                        Oppgave.Tilstand.Type.Companion.søkbareTilstander
                                                    }.map { TilstandNavnDTO(navn = it.tilTilstandNavn()) },
                                            serier = resultat.tilStatistikkResultatSerieDTOForRettighet(),
                                        ),
                                ),
                        )
                    } else {
                        val tilstandStatistikker =
                            produksjonsstatistikkRepository.hentTilstanderMedUtløstAvFilter(produksjonsstatistikkFilter)
                        val serier =
                            produksjonsstatistikkRepository.hentUtløstAvMedTilstandFilter(produksjonsstatistikkFilter)
                        val resultat =
                            produksjonsstatistikkRepository.hentResultatSerierForUtløstAv(produksjonsstatistikkFilter)

                        call.respond(
                            status = HttpStatusCode.OK,
                            message =
                                ProduksjonsstatistikkDTO(
                                    grupper = tilstandStatistikker.tilStatistikkGruppeDTO(),
                                    serier = serier.tilStatistikkSerieDTOForUtløstAv(),
                                    resultat =
                                        StatistikkResultatDTO(
                                            grupper =
                                                produksjonsstatistikkFilter.tilstander
                                                    .ifEmpty {
                                                        Oppgave.Tilstand.Type.Companion.søkbareTilstander
                                                    }.map { TilstandNavnDTO(navn = it.tilTilstandNavn()) },
                                            serier = resultat.tilStatistikkResultatSerieDTOForUtløstAv(),
                                        ),
                                ),
                        )
                    }
                }
            }
        }
    }
}

internal fun List<AntallOppgaverForTilstandOgRettighet>.tilStatistikkResultatSerieDTOForRettighet(): List<StatistikkResultatSerieDTO> =
    this
        .groupBy { it.rettighet }
        .map { (rettighet, antallOppgaverForTilstandOgRettighetListe) ->
            StatistikkResultatSerieDTO(
                navn = rettighet,
                verdier =
                    antallOppgaverForTilstandOgRettighetListe.map { antallOppgaverForTilstandOgRettighet ->
                        StatistikkGruppeMedAntallDTO(
                            gruppe = antallOppgaverForTilstandOgRettighet.tilstand.tilTilstandNavn(),
                            antall = antallOppgaverForTilstandOgRettighet.antall,
                        )
                    },
            )
        }

internal fun List<AntallOppgaverForTilstandOgUtløstAv>.tilStatistikkResultatSerieDTOForUtløstAv(): List<StatistikkResultatSerieDTO> =
    this
        .groupBy { it.utløstAv }
        .map { (utlostAv, antallOppgaverForTilstandOgUtløstAvListe) ->
            StatistikkResultatSerieDTO(
                navn = utlostAv.tilSerieNavn(),
                verdier =
                    antallOppgaverForTilstandOgUtløstAvListe.map { antallOppgaverForTilstandOgUtløstAv ->
                        StatistikkGruppeMedAntallDTO(
                            gruppe = antallOppgaverForTilstandOgUtløstAv.tilstand.tilTilstandNavn(),
                            antall = antallOppgaverForTilstandOgUtløstAv.antall,
                        )
                    },
            )
        }

private fun List<TilstandStatistikk>.tilStatistikkGruppeDTO(): List<StatistikkGruppeDTO> =
    this.map { tilstandStatistikk ->
        StatistikkGruppeDTO(
            navn = tilstandStatistikk.tilstand.name,
            total = tilstandStatistikk.antall,
            eldsteOppgave = tilstandStatistikk.eldsteOppgaveTidspunkt,
        )
    }

internal fun List<AntallOppgaverForRettighet>.tilStatistikkSerieDTOForRettighet(): List<StatistikkSerieDTO> =
    this.map { antallOppgaverForRettighet ->
        StatistikkSerieDTO(
            navn = antallOppgaverForRettighet.rettighet,
            total = antallOppgaverForRettighet.antall,
        )
    }

internal fun List<AntallOppgaverForUtløstAv>.tilStatistikkSerieDTOForUtløstAv(): List<StatistikkSerieDTO> =
    this.map { antallOppgaverForUtløstAv ->
        StatistikkSerieDTO(
            navn = antallOppgaverForUtløstAv.utløstAv.name,
            total = antallOppgaverForUtløstAv.antall,
        )
    }

private fun Oppgave.Tilstand.Type.tilTilstandNavn(): String =
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
