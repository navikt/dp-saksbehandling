package no.nav.dagpenger.behandling

import com.fasterxml.uuid.Generators
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class MinsteinntektVurdering(
    val uuid: UUID = Generators.timeBasedEpochGenerator().generate(),
    val virkningsdato: LocalDate,
    val vilkaarOppfylt: Boolean,
    val inntektsId: String,
    private val inntektPerioder: List<InntektPeriode>,
    private val subsumsjonsId: String,
    private val regelIdentifikator: String,
    private val beregningsRegel: String,
) {
    val tolveMånedPeriode: InntektPeriode
    val trettiSeksMånedPeriode: InntektPeriode

    init {
        require(inntektPerioder.size == 3) { "InntektPerioder må ha 3 perioder" }

        calculateInntektPerioder().let {
            this.tolveMånedPeriode = it.first
            this.trettiSeksMånedPeriode = it.second
        }
    }

    fun accept(visitor: MinsteinntektVurderingVisitor) {
        visitor.visit(
            uuid = uuid,
            virkningsdato = virkningsdato,
            vilkaarOppfylt = vilkaarOppfylt,
            inntektsId = inntektsId,
            tolveMånedPeriode = tolveMånedPeriode,
            trettiSeksMånedPeriode = trettiSeksMånedPeriode,
            subsumsjonsId = subsumsjonsId,
            regelIdentifikator = regelIdentifikator,
            beregningsRegel = beregningsRegel,
        )
    }

    interface MinsteinntektVurderingVisitor {
        fun visit(
            uuid: UUID,
            virkningsdato: LocalDate,
            vilkaarOppfylt: Boolean,
            inntektsId: String,
            tolveMånedPeriode: InntektPeriode,
            trettiSeksMånedPeriode: InntektPeriode,
            subsumsjonsId: String,
            regelIdentifikator: String,
            beregningsRegel: String,
        ) {
        }
    }

    fun calculateInntektPerioder(): Pair<InntektPeriode, InntektPeriode> {
        return inntektPerioder.sortedByDescending { it.førsteMåned }.let { inntektPerioder ->
            Pair(
                first =
                    InntektPeriode(
                        førsteMåned = inntektPerioder.first().førsteMåned,
                        sisteMåned = inntektPerioder.first().sisteMåned,
                        inntekt = inntektPerioder.first().inntekt,
                    ),
                second =
                    InntektPeriode(
                        førsteMåned = inntektPerioder.last().førsteMåned,
                        sisteMåned = inntektPerioder.first().sisteMåned,
                        inntekt = inntektPerioder.sumOf { it.inntekt },
                    ),
            )
        }
    }
}

data class InntektPeriode(
    val førsteMåned: YearMonth,
    val sisteMåned: YearMonth,
    val inntekt: Double,
)
