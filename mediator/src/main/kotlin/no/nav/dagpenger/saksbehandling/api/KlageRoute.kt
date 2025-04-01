package no.nav.dagpenger.saksbehandling.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.OpplysningerVerdi
import no.nav.dagpenger.saksbehandling.api.KlageDtoMapper.hentVerdi
import no.nav.dagpenger.saksbehandling.api.models.OppdaterKlageOpplysningDTO
import java.time.LocalDate

fun Route.klageApi(mediator: KlageMediator) {
    route("klage") {
        route("{klageId}") {
            get {
                val klageId = call.finnUUID("klageId")
                val klageDTO = mediator.hentKlage(klageId)
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

object KlageDtoMapper {
    val objectMapper = jacksonObjectMapper()

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
