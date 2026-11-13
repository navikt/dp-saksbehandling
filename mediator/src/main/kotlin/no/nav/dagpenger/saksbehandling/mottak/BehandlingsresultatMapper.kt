package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.serder.asUUID
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.firstOrNull

private val logger = KotlinLogging.logger { }
private val sikkerLogg = KotlinLogging.logger("tjenestekall")

class BehandlingsresultatMapper(
    json: String,
) {
    private val objectMapper = ObjectMapper()
    private val jsonNode =
        try {
            objectMapper.readTree(json)
        } catch (e: Exception) {
            logger.error(e) { "Kunne ikke parse json: ${e.message}" }
            sikkerLogg.error { "Kunne ikke parse json: $json" }
            throw IllegalArgumentException("Kunne ikke parse json: $e")
        }.also {
            require(it.isObject) {
                sikkerLogg.error { "Forventet at json er et objekt: $it" }
            }
        }

    private val opplysningerNode =
        jsonNode["opplysninger"]?.takeIf { it.isArray } ?: objectMapper.createArrayNode()

    private val rettighetsperioderNode =
        jsonNode["rettighetsperioder"]?.takeIf { it.isArray } ?: objectMapper.createArrayNode()

    private val behandletHendelseNode =
        jsonNode["behandletHendelse"].also {
            require(it.finnesMedVerdi() && it.isObject) {
                sikkerLogg.error { "Kunne ikke parse behandletHendelse: $it" }
                "Forventet at behandletHendelse er et objekt"
            }
        }

    private fun JsonNode.boolskVerdi(): Boolean = this["datatype"].asText() == "boolsk" && this["verdi"].asBoolean()

    private fun JsonNode?.finnesMedVerdi(): Boolean = this != null && !(this.isMissingNode || this.isNull)

    fun erSøknad(): Boolean = behandletHendelseNode["type"].stringValue() == "Søknad"

    fun søknadOmDagpengerInnvilges(): Boolean =
        erSøknad() &&
            rettighetsperioderNode.any {
                it["harRett"].finnesMedVerdi() &&
                    it["opprinnelse"].finnesMedVerdi() &&
                    it["harRett"].asBoolean() &&
                    it["opprinnelse"].stringValue() == "Ny"
            }

    fun behandletHendelseType(): String = behandletHendelseNode["type"].stringValue()

    fun erSøknadOmGjenopptak(): Boolean = erSøknad() && jsonNode["basertPå"].finnesMedVerdi()

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

    fun finnesOpplysningMedVerdiTrue(opplysningTypeId: UUID): Boolean =
        finnOpplysningNodeForPrøvingsdato(opplysningTypeId)?.let {
            it.boolskVerdi()
        } ?: false

    fun finnesOpplysningMedVerdiFalse(opplysningTypeId: UUID): Boolean =
        finnOpplysningNodeForPrøvingsdato(opplysningTypeId)?.let {
            !it.boolskVerdi()
        } ?: false

    private fun finnOpplysningNodeForPrøvingsdato(opplysningTypeId: UUID): JsonNode? =
        prøvingsdato()?.let { prøvingsdato ->
            opplysningerNode
                .singleOrNull { it["opplysningTypeId"].asUUID() == opplysningTypeId }
                ?.let { opplysningNode ->
                    opplysningNode
                        .get("perioder")
                        .firstOrNull {
                            val fom = it.get("gyldigFraOgMed").asLocalDate()
                            val tom = it.get("gyldigTilOgMed")?.asLocalDate() ?: LocalDate.MAX
                            prøvingsdato in fom..tom
                        }?.get("verdi")
                }
        }
}
