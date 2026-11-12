package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.dagpenger.saksbehandling.serder.asUUID
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.firstOrNull

class BehandlingsresultatMapper(
    json: String,
) {
    private val objectMapper = ObjectMapper()
    private val jsonNode = objectMapper.readTree(json)

    private val opplysningerNode =
        jsonNode["opplysninger"]?.takeIf { it.isArray } ?: objectMapper.createArrayNode()

    private fun JsonNode.boolskVerdi(): Boolean = this["datatype"].asText() == "boolsk" && this["verdi"].asBoolean()

    private fun JsonNode?.finnesMedVerdi(): Boolean = this != null && !(this.isMissingNode || this.isNull)

    fun prøvingsdato(): LocalDate? {
        val prøvingsdatoOpplysningNode =
            opplysningerNode.singleOrNull {
                it["opplysningTypeId"].asUUID() ==
                    OpplysningTyper.PRØVINGSDATO.opplysningTypeId
            }
        return prøvingsdatoOpplysningNode
            ?.get("perioder")
            ?.firstOrNull { it["opprinnelse"].asString() == "Ny" }
            ?.get("verdi")
            ?.get("verdi")
            ?.asLocalDate()
    }

    fun erOpplysningTrue(opplysningTypeId: UUID): Boolean {
        val prøvingsdato = prøvingsdato()
        return if (prøvingsdato != null) {
            val verdiNode =
                opplysningerNode.singleOrNull { it["opplysningTypeId"].asUUID() == opplysningTypeId }?.let { opplysningNode ->
                    opplysningNode
                        .get("perioder")
                        .firstOrNull {
                            val fom = it.get("gyldigFraOgMed").asLocalDate()
                            val tom = it.get("gyldigTilOgMed")?.asLocalDate() ?: LocalDate.MAX
                            prøvingsdato in fom..tom
                        }?.get("verdi")
                }
            verdiNode?.boolskVerdi() ?: false
        } else {
            false
        }
    }
}
