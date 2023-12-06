package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class MinsteinntektVurderingTest {
    private val inntekstPerioder =
        listOf(
            InntektPeriode(
                førsteMåned = YearMonth.of(2020, 2),
                sisteMåned = YearMonth.of(2020, 3),
                inntekt = 2000.0,
            ),
            InntektPeriode(
                førsteMåned = YearMonth.of(2020, 1),
                sisteMåned = YearMonth.of(2020, 2),
                inntekt = 1000.0,
            ),
            InntektPeriode(
                førsteMåned = YearMonth.of(2020, 3),
                sisteMåned = YearMonth.of(2020, 4),
                inntekt = 1000.0,
            ),
        )

    @Test
    fun `12 måned periode fylles ut riktig`() {
        MinsteinntektVurdering(
            virkningsdato = LocalDate.MAX,
            vilkaarOppfylt = false,
            inntektsId = "",
            inntektPerioder = inntekstPerioder,
            subsumsjonsId = "",
            regelIdentifikator = "",
            beregningsRegel = "",
        ).tolveMånedPeriode shouldBe
            InntektPeriode(
                førsteMåned = YearMonth.of(2020, 3),
                sisteMåned = YearMonth.of(2020, 4),
                inntekt = 1000.0,
            )
    }

    @Test
    fun `36 måned periode fylles ut riktig`() {
        MinsteinntektVurdering(
            virkningsdato = LocalDate.MAX,
            vilkaarOppfylt = false,
            inntektsId = "",
            inntektPerioder = inntekstPerioder,
            subsumsjonsId = "",
            regelIdentifikator = "",
            beregningsRegel = "",
        ).trettiSeksMånedPeriode shouldBe
            InntektPeriode(
                førsteMåned = YearMonth.of(2020, 1),
                sisteMåned = YearMonth.of(2020, 4),
                inntekt = 4000.0,
            )
    }
}
