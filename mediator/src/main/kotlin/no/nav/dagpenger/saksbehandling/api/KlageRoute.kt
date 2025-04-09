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
import no.nav.dagpenger.saksbehandling.api.models.KlageDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningBoolskDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDTOGruppeDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningDatoDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningFlerListeValgDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningListeValgDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningTekstDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.KlageOpplysningVerdiDTO
import no.nav.dagpenger.saksbehandling.api.models.OppdaterKlageOpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.UtfallDTO
import no.nav.dagpenger.saksbehandling.api.models.UtfallDTOVerdiDTO

fun Route.klageApi(mediator: KlageMediator) {
    authenticate("azureAd") {
        route("oppgave/klage") {
            route("{klageId}") {
                get {
                    val klageId = call.finnUUID("klageId")
                    val klageDTO =
                        mediator.hentKlage(klageId).tilDto()
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
        return this.verdi.tilVerdi()
    }

    fun KlageOpplysningVerdiDTO.tilVerdi(): OpplysningerVerdi {
        return when (this) {
            is KlageOpplysningBoolskDTO -> OpplysningerVerdi.Boolsk(this.verdi)
            is KlageOpplysningDatoDTO -> OpplysningerVerdi.Dato(this.verdi)
            is KlageOpplysningFlerListeValgDTO -> OpplysningerVerdi.TekstListe(this.verdi)
            is KlageOpplysningListeValgDTO -> OpplysningerVerdi.Tekst(this.verdi)
            is KlageOpplysningTekstDTO -> OpplysningerVerdi.Tekst(this.verdi)
        }
    }

    fun KlageBehandling.tilDto(): KlageDTO {
        return KlageDTO(
            id = this.id,
            // todo
            saksbehandler = null,
            behandlingOpplysninger =
                this.synligeOpplysninger().map { opplysning ->
                    KlageOpplysningDTO(
                        id = opplysning.id,
                        navn = opplysning.type.navn,
                        klageopplysningType = opplysning.type.datatype.tilDto(),
                        // todo
                        paakrevd = true,
                        // todo
                        gruppe = KlageOpplysningDTOGruppeDTO.KLAGESAK,
                        verdi = opplysning.verdi.tilDto(),
                        // todo
                        valgmuligheter = emptyList(),
                        // todo
                        redigerbar = true,
                    )
                },
            utfallOpplysninger = emptyList(),
            utfall = this.utfall.tilDto(),
        )
    }

    fun Utfall.tilDto(): UtfallDTO {
        return UtfallDTO(
            verdi =
                when (this) {
                    Utfall.Avvist -> UtfallDTOVerdiDTO.AVVIST
                    Utfall.TomtUtfall -> UtfallDTOVerdiDTO.IKKE_SATT
                    Utfall.Opprettholdelse -> UtfallDTOVerdiDTO.IKKE_SATT
                },
            // todo
            tilgjeneligeUtfall = emptyList(),
        )
    }

    fun Opplysning.Datatype.tilDto(): KlageOpplysningTypeDTO {
        return when (this) {
            Opplysning.Datatype.TEKST -> KlageOpplysningTypeDTO.TEKST
            Opplysning.Datatype.DATO -> KlageOpplysningTypeDTO.DATO
            Opplysning.Datatype.BOOLSK -> KlageOpplysningTypeDTO.BOOLSK
            Opplysning.Datatype.FLERVALG -> KlageOpplysningTypeDTO.FLER_LISTEVALG
        }
    }

    fun Verdi.tilDto(): KlageOpplysningVerdiDTO? {
        return when (this) {
            is Verdi.Boolsk -> KlageOpplysningBoolskDTO(this.value)
            is Verdi.Dato -> KlageOpplysningDatoDTO(this.value)
            is Verdi.Flervalg -> KlageOpplysningFlerListeValgDTO(this.value)
            is Verdi.TekstVerdi -> KlageOpplysningTekstDTO(this.value)
            Verdi.TomVerdi -> null
        }
    }
}
