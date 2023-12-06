package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.MinsteinntektVurdering
import no.nav.dagpenger.behandling.api.models.InntektPeriodeDTO
import no.nav.dagpenger.behandling.api.models.MinsteInntektVurderingDTO

fun MinsteinntektVurdering.toDTO(): MinsteInntektVurderingDTO {
    return MinsteInntektVurderingDTO(
        uuid = this.uuid.toString(),
        virkningsdato = this.virkningsdato,
        vilkaarOppfylt = this.vilkaarOppfylt,
        inntektsId = this.inntektsId,
        inntektPerioder =
            listOf(
                InntektPeriodeDTO(
                    periodeType = "12 måneder",
                    fra = this.tolveMånedPeriode.førsteMåned.toString(),
                    til = this.tolveMånedPeriode.sisteMåned.toString(),
                    inntekt = this.tolveMånedPeriode.inntekt,
                ),
                InntektPeriodeDTO(
                    periodeType = "36 måneder",
                    fra = this.trettiSeksMånedPeriode.førsteMåned.toString(),
                    til = this.trettiSeksMånedPeriode.sisteMåned.toString(),
                    inntekt = this.trettiSeksMånedPeriode.inntekt,
                ),
            ),
    )
}
