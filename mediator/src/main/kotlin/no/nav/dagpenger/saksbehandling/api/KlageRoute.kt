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
import no.nav.dagpenger.saksbehandling.api.KlageDtoMapper.hentVerdi
import no.nav.dagpenger.saksbehandling.api.models.KlageDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.OppdaterKlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.UtfallDTO
import java.time.LocalDate

fun Route.klageApi(mediator: KlageMediator) {
    authenticate("azureAd") {
        route("klage") {
            route("{klageId}") {
                get {
                    val klageId = call.finnUUID("klageId")
                    val klageDTO =
                        mediator.hentKlage(klageId).let { klage ->
                            KlageDtoMapper.lageKlageDTO(klage)
                        }
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
                                verdi = oppdaterKlageOpplysningDTO.hentVerdi(),
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
    fun lageKlageDTO(klage: KlageBehandling): KlageDTO {
        return KlageDTO(
            id = klage.id,
            behandlingOpplysninger =
                klage.opplysninger.map { opplysning ->
                    KlageOpplysningDTO(
                        id = opplysning.id,
                        navn = opplysning.navn,
                        type = opplysning.type.tilDto(),
                        // todo
                        paakrevd = true,
                        // todo
                        gruppe = KlageOpplysningDTO.Gruppe.FORMKRAV,
                        // todo
                        redigerbar = true,
                        verdi = KlageOpplysningVerdiDTO(),
                        // todo
                        valgmuligheter = emptyList(),
                    )
                },
            utfallOpplysninger = emptyList(),
            utfall =
                UtfallDTO(
                    verdi = klage.utfall.tilDto(),
                    tilgjeneligeUtfall = emptyList(),
                ),
            saksbehandler = null,
            meldingOmVedtak = null,
        )
    }

    fun Utfall.tilDto(): UtfallDTO.Verdi {
        return when (this) {
            Utfall.Avvist -> UtfallDTO.Verdi.AVVIST
            Utfall.TomtUtfall -> UtfallDTO.Verdi.IKKE_SATT
        }
    }

    fun Opplysning.OpplysningType.tilDto(): KlageOpplysningDTO.Type {
        return when (this) {
            Opplysning.OpplysningType.TEKST -> KlageOpplysningDTO.Type.TEKST
            Opplysning.OpplysningType.DATO -> KlageOpplysningDTO.Type.DATO
            Opplysning.OpplysningType.BOOLSK -> KlageOpplysningDTO.Type.BOOLSK
            Opplysning.OpplysningType.FLERVALG -> KlageOpplysningDTO.Type.LISTEVALG
        }
    }

    fun OppdaterKlageOpplysningDTO.hentVerdi(): OpplysningerVerdi {
        return when (this.opplysningType) {
            OppdaterKlageOpplysningDTO.OpplysningType.TEKST -> OpplysningerVerdi.Tekst(this.verdi as String)
            OppdaterKlageOpplysningDTO.OpplysningType.BOOLSK -> OpplysningerVerdi.Boolsk(this.verdi as Boolean)
            OppdaterKlageOpplysningDTO.OpplysningType.DATO -> OpplysningerVerdi.Dato(LocalDate.parse(this.verdi as String))
            OppdaterKlageOpplysningDTO.OpplysningType.LISTEVALG -> OpplysningerVerdi.Tekst(this.verdi as String)
            OppdaterKlageOpplysningDTO.OpplysningType.FLERMinusLISTEVALG -> OpplysningerVerdi.TekstListe(this.verdi as List<String>)
        }
    }
}
