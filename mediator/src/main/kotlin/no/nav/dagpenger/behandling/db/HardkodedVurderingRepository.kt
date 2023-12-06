package no.nav.dagpenger.behandling.db

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.behandling.InntektPeriode
import no.nav.dagpenger.behandling.MinsteinntektVurdering
import no.nav.dagpenger.behandling.api.json.objectMapper
import no.nav.dagpenger.behandling.db.PacketMapper.beregningsdato
import no.nav.dagpenger.behandling.db.PacketMapper.beregningsregel
import no.nav.dagpenger.behandling.db.PacketMapper.inntektPerioder
import no.nav.dagpenger.behandling.db.PacketMapper.inntektsId
import no.nav.dagpenger.behandling.db.PacketMapper.oppFyllerMinsteinntekt
import no.nav.dagpenger.behandling.db.PacketMapper.regelIdentifikator
import no.nav.dagpenger.behandling.db.PacketMapper.subsumsjonsId
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class HardkodedVurderingRepository : VurderingRepository {
    private val resourceRetriever = object {}.javaClass
    private val vurderinger =
        mutableListOf<MinsteinntektVurdering>().also {
            it.populate()
        }

    private fun MutableList<MinsteinntektVurdering>.populate() {
        val jsonNode = objectMapper.readTree(resourceRetriever.getResource("/LEL_eksempel.json")?.readText()!!)
        this.add(
            MinsteinntektVurdering(
                virkningsdato = jsonNode.beregningsdato(),
                vilkaarOppfylt = jsonNode.oppFyllerMinsteinntekt(),
                inntektsId = jsonNode.inntektsId(),
                inntektPerioder = jsonNode.inntektPerioder(),
                subsumsjonsId = jsonNode.subsumsjonsId(),
                regelIdentifikator = jsonNode.regelIdentifikator(),
                beregningsRegel = jsonNode.beregningsregel(),
            ),
        )
    }

    override fun hentMinsteInntektVurdering(oppgaveId: UUID): MinsteinntektVurdering {
        return vurderinger.first()
    }

    private fun JsonNode.asBooleanStrict(): Boolean = asText().toBooleanStrict()
}


internal object PacketMapper {
    fun JsonNode.subsumsjonsId(): String = this["minsteinntektResultat"]["subsumsjonsId"].asText()

    fun JsonNode.regelIdentifikator(): String = this["minsteinntektResultat"]["regelIdentifikator"].asText()

    fun JsonNode.beregningsregel(): String = this["minsteinntektResultat"]["beregningsregel"].asText()

    fun JsonNode.beregningsdato(): LocalDate =
        this["beregningsDato"].asText().let {
            LocalDate.parse(it)
        }

    fun JsonNode.oppFyllerMinsteinntekt() = this["minsteinntektResultat"]["oppfyllerMinsteinntekt"].asBooleanStrict()

    fun JsonNode.inntektsId(): String = this["inntektV1"]["inntektsId"].asText()

    private fun JsonNode.asYearMonth(): YearMonth = YearMonth.parse(this.asText())

    fun JsonNode.inntektPerioder(): List<InntektPeriode> {
        return this["minsteinntektInntektsPerioder"].map { node ->
            InntektPeriode(
                førsteMåned = node["inntektsPeriode"]["førsteMåned"].asYearMonth(),
                sisteMåned = node["inntektsPeriode"]["sisteMåned"].asYearMonth(),
                inntekt = node["inntekt"].asDouble(),
            )
        }
    }

    private fun JsonNode.asBooleanStrict(): Boolean = asText().toBooleanStrict()
}