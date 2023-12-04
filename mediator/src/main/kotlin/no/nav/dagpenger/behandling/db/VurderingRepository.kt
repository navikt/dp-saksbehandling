package no.nav.dagpenger.behandling.db

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.behandling.api.json.objectMapper
import no.nav.dagpenger.behandling.api.models.InntektPeriodeDTO
import no.nav.dagpenger.behandling.api.models.MinsteInntektVurderingDTO
import no.nav.dagpenger.behandling.db.PacketMapper.beregningsdato
import no.nav.dagpenger.behandling.db.PacketMapper.calculateInntektPerioder
import no.nav.dagpenger.behandling.db.PacketMapper.inntektsId
import no.nav.dagpenger.behandling.db.PacketMapper.oppFyllerMinsteinntekt
import no.nav.dagpenger.behandling.db.PacketMapper.subsumsjonsId
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

interface VurderingRepository {
    fun hentMinsteInntektVurdering(oppgaveId: UUID): MinsteInntektVurderingDTO
}

internal object PacketMapper {
    fun JsonNode.subsumsjonsId(): String = this["minsteinntektResultat"]["subsumsjonsId"].asText()

    fun JsonNode.beregningsdato(): LocalDate =
        this["beregningsDato"].asText().let {
            LocalDate.parse(it)
        }

    fun JsonNode.oppFyllerMinsteinntekt() = this["minsteinntektResultat"]["oppfyllerMinsteinntekt"].asBooleanStrict()

    fun JsonNode.inntektsId(): String = this["inntektV1"]["inntektsId"].asText()

    private data class InntektPeriodeInfo(
        val inntektsPeriode: InntektPeriode,
        val inntekt: Double,
        val periode: Int,
        val inneholderFangstOgFisk: Boolean,
        val andel: Double,
    ) {
        data class InntektPeriode(
            val førsteMåned: YearMonth,
            val sisteMåned: YearMonth,
        )
    }

    fun JsonNode.calculateInntektPerioder(): List<InntektPeriodeDTO> {
        val list =
            this["minsteinntektInntektsPerioder"].map {
                objectMapper.convertValue(it, InntektPeriodeInfo::class.java)
            }.sortedBy { it.periode }

        val first =
            list.first().let {
                InntektPeriodeDTO(
                    periodeType = "12 måneder",
                    fra = it.inntektsPeriode.førsteMåned.toString(),
                    til = it.inntektsPeriode.sisteMåned.toString(),
                    // todo sjekke andel
                    inntekt = it.inntekt,
                )
            }

        val second =
            InntektPeriodeDTO(
                periodeType = "36 måneder",
                fra = list.last().inntektsPeriode.førsteMåned.toString(),
                til = list.first().inntektsPeriode.sisteMåned.toString(),
                inntekt = list.sumOf { it.inntekt },
            )

        return listOf(first, second)
    }
}

private fun JsonNode.asBooleanStrict(): Boolean = asText().toBooleanStrict()

internal class HardkodedVurderingRepository : VurderingRepository {
    private val resourceRetriever = object {}.javaClass
    private val vurderinger =
        mutableListOf<MinsteInntektVurderingDTO>().also {
            it.populate()
        }

    private fun MutableList<MinsteInntektVurderingDTO>.populate() {
        val jsonNode = objectMapper.readTree(resourceRetriever.getResource("/LEL_eksempel.json")?.readText()!!)
        this.add(
            MinsteInntektVurderingDTO(
                uuid = jsonNode.subsumsjonsId(),
                virkningsdato = jsonNode.beregningsdato(),
                vilkaarOppfylt = jsonNode.oppFyllerMinsteinntekt(),
                inntektsId = jsonNode.inntektsId(),
                inntektPerioder = jsonNode.calculateInntektPerioder(),
            ),
        )
    }

    override fun hentMinsteInntektVurdering(oppgaveId: UUID): MinsteInntektVurderingDTO {
        return vurderinger.first()
    }
}
