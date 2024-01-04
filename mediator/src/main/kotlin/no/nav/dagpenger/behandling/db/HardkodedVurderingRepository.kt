package no.nav.dagpenger.behandling.db

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.behandling.InntektPeriode
import no.nav.dagpenger.behandling.MinsteInntektVurdering
import no.nav.dagpenger.behandling.api.json.objectMapper
import no.nav.dagpenger.behandling.db.PacketMapper.beregningsdato
import no.nav.dagpenger.behandling.db.PacketMapper.beregningsregel
import no.nav.dagpenger.behandling.db.PacketMapper.inntektPerioder
import no.nav.dagpenger.behandling.db.PacketMapper.inntektsId
import no.nav.dagpenger.behandling.db.PacketMapper.oppFyllerMinsteInntekt
import no.nav.dagpenger.behandling.db.PacketMapper.regelIdentifikator
import no.nav.dagpenger.behandling.db.PacketMapper.subsumsjonsId
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class HardkodedVurderingRepository : VurderingRepository {
    private val resourceRetriever = object {}.javaClass
    private val vurderinger =
        mutableListOf<MinsteInntektVurdering>().also {
            it.populate()
        }

    private fun MutableList<MinsteInntektVurdering>.populate() {
        val jsonNode = objectMapper.readTree(resourceRetriever.getResource("/LEL_eksempel.json")?.readText()!!)
        this.add(
            MinsteInntektVurdering(
                virkningsdato = jsonNode.beregningsdato(),
                vilkaarOppfylt = jsonNode.oppFyllerMinsteInntekt(),
                inntektsId = jsonNode.inntektsId(),
                inntektPerioder = jsonNode.inntektPerioder(),
                subsumsjonsId = jsonNode.subsumsjonsId(),
                regelIdentifikator = jsonNode.regelIdentifikator(),
                beregningsregel = jsonNode.beregningsregel(),
            ),
        )
    }

    override fun hentMinsteInntektVurdering(oppgaveId: UUID): MinsteInntektVurdering {
        return vurderinger.first()
    }

    override fun lagreMinsteInntektVurdering(
        oppgaveId: UUID,
        vurdering: MinsteInntektVurdering,
    ) {
        TODO("Not yet implemented")
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

    fun JsonNode.oppFyllerMinsteInntekt() = this["minsteinntektResultat"]["oppfyllerMinsteinntekt"].asBooleanStrict()

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
