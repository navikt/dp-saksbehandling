package no.nav.dagpenger.saksbehandling

import com.fasterxml.uuid.Generators
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class MinsteInntektVurdering(
    val uuid: UUID = Generators.timeBasedEpochGenerator().generate(),
    val virkningsdato: LocalDate,
    val vilkaarOppfylt: Boolean,
    val inntektsId: String,
    private val inntektPerioder: List<InntektPeriode>,
    private val subsumsjonsId: String,
    private val regelIdentifikator: String,
    private val beregningsregel: String,
) {
    val tolvMånederPeriode: InntektPeriode
    val trettiseksMånederPeriode: InntektPeriode

    init {
        require(inntektPerioder.size == 3) { "InntektPerioder må ha 3 perioder" }

        calculateInntektPerioder().let {
            this.tolvMånederPeriode = it.first
            this.trettiseksMånederPeriode = it.second
        }
    }

    fun accept(visitor: MinsteInntektVurderingVisitor) {
        visitor.visit(
            uuid = uuid,
            virkningsdato = virkningsdato,
            vilkårOppfylt = vilkaarOppfylt,
            inntektsId = inntektsId,
            tolvMånederPeriode = tolvMånederPeriode,
            trettiseksMånederPeriode = trettiseksMånederPeriode,
            subsumsjonsId = subsumsjonsId,
            regelIdentifikator = regelIdentifikator,
            beregningsregel = beregningsregel,
        )
    }

    interface MinsteInntektVurderingVisitor {
        fun visit(
            uuid: UUID,
            virkningsdato: LocalDate,
            vilkårOppfylt: Boolean,
            inntektsId: String,
            tolvMånederPeriode: InntektPeriode,
            trettiseksMånederPeriode: InntektPeriode,
            subsumsjonsId: String,
            regelIdentifikator: String,
            beregningsregel: String,
        ) {
        }
    }

    private fun calculateInntektPerioder(): Pair<InntektPeriode, InntektPeriode> {
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
