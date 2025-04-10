package no.nav.dagpenger.saksbehandling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.KlageBehandling
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.Opplysning
import no.nav.dagpenger.saksbehandling.OpplysningerVerdi
import no.nav.dagpenger.saksbehandling.Utfall
import no.nav.dagpenger.saksbehandling.Verdi
import no.nav.dagpenger.saksbehandling.api.KlageDtoMapper.tilDto
import no.nav.dagpenger.saksbehandling.api.KlageDtoMapper.tilVerdi
import no.nav.dagpenger.saksbehandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageGruppeDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningBoolskDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDatoDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningFlerListeValgDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningTekstDTO
import no.nav.dagpenger.saksbehandling.api.models.ListeVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.OppdaterKlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.TekstVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.UtfallDTO
import no.nav.dagpenger.saksbehandling.api.models.UtfallDTOVerdiDTO

fun Route.klageApi(mediator: KlageMediator) {
    authenticate("azureAd") {
        route("oppgave/klage") {
            route("{klageId}") {
                get {
                    val klageId = call.finnUUID("klageId")
                    val klage = mediator.hentKlage(klageId)
                    val klageDTO = klage.tilDto()
                    call.respond(HttpStatusCode.OK, klageDTO)
                }

                route("opplysning") {
                    route("{opplysningId}") {
                        put {
                            val klageId = call.finnUUID("klageId")
                            val opplysningId = call.finnUUID("opplysningId")
                            val oppdaterKlageOpplysningDTO = call.receive<OppdaterKlageOpplysningDTO>()
                            mediator.oppdaterKlageOpplysning(
                                klageId = klageId,
                                opplysningId = opplysningId,
                                verdi = oppdaterKlageOpplysningDTO.tilVerdi(),
                            )
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
        }
    }
}

object KlageDtoMapper {
    fun OppdaterKlageOpplysningDTO.tilVerdi(): OpplysningerVerdi {
        return when (this) {
            is BoolskVerdiDTO -> OpplysningerVerdi.Boolsk(this.verdi)
            is DatoVerdiDTO -> OpplysningerVerdi.Dato(this.verdi)
            is ListeVerdiDTO -> OpplysningerVerdi.TekstListe(this.verdi)
            is TekstVerdiDTO -> OpplysningerVerdi.Tekst(this.verdi)
        }
    }

    fun KlageBehandling.tilDto(): KlageDTO {
        return KlageDTO(
            id = this.id,
            // todo
            saksbehandler = null,
            behandlingOpplysninger =
                this.opplysninger.map { opplysning ->
                    when (opplysning.type) {
                        Opplysning.OpplysningType.TEKST ->
                            KlageOpplysningTekstDTO(
                                id = opplysning.id,
                                navn = opplysning.navn,
                                // todo
                                paakrevd = true,
                                // todo
                                gruppe = KlageGruppeDTO.FRIST,
                                valgmuligheter = emptyList(),
                                redigerbar = true,
                                verdi =
                                    if (opplysning.verdi is Verdi.TomVerdi) {
                                        null
                                    } else {
                                        (opplysning.verdi as Verdi.TekstVerdi).value
                                    },
                            )
                        Opplysning.OpplysningType.DATO -> {
                            KlageOpplysningDatoDTO(
                                id = opplysning.id,
                                navn = opplysning.navn,
                                // todo
                                paakrevd = true,
                                // todo
                                gruppe = KlageGruppeDTO.FRIST,
                                valgmuligheter = emptyList(),
                                redigerbar = true,
                                verdi =
                                    if (opplysning.verdi is Verdi.TomVerdi) {
                                        null
                                    } else {
                                        (opplysning.verdi as Verdi.Dato).value
                                    },
                            )
                        }
                        Opplysning.OpplysningType.BOOLSK -> {
                            KlageOpplysningBoolskDTO(
                                id = opplysning.id,
                                navn = opplysning.navn,
                                // todo
                                paakrevd = true,
                                // todo
                                gruppe = KlageGruppeDTO.FRIST,
                                valgmuligheter = emptyList(),
                                redigerbar = true,
                                verdi =
                                    if (opplysning.verdi is Verdi.TomVerdi) {
                                        null
                                    } else {
                                        (opplysning.verdi as Verdi.Boolsk).value
                                    },
                            )
                        }
                        Opplysning.OpplysningType.FLERVALG -> {
                            KlageOpplysningFlerListeValgDTO(
                                id = opplysning.id,
                                navn = opplysning.navn,
                                // todo
                                paakrevd = true,
                                // todo
                                gruppe = KlageGruppeDTO.FRIST,
                                valgmuligheter = emptyList(),
                                redigerbar = true,
                                verdi =
                                    if (opplysning.verdi is Verdi.TomVerdi) {
                                        null
                                    } else {
                                        (opplysning.verdi as Verdi.Flervalg).value
                                    },
                            )
                        }
                    }
                },
            utfallOpplysninger = emptyList(),
            utfall =
                UtfallDTO(
                    verdi =
                        when (this.utfall) {
                            Utfall.Avvist -> UtfallDTOVerdiDTO.AVVIST
                            Utfall.TomtUtfall -> UtfallDTOVerdiDTO.IKKE_SATT
                        },
                    tilgjeneligeUtfall = emptyList(),
                ),
            meldingOmVedtak = null,
        )
    }
}
