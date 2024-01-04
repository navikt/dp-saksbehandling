package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.MinsteInntektVurdering
import no.nav.dagpenger.behandling.api.models.InntektPeriodeDTO
import no.nav.dagpenger.behandling.api.models.MinsteInntektVurderingDTO

fun MinsteInntektVurdering.toDTO(): MinsteInntektVurderingDTO {
    return MinsteInntektVurderingDTO(
        uuid = this.uuid.toString(),
        virkningsdato = this.virkningsdato,
        vilkaarOppfylt = this.vilkaarOppfylt,
        inntektsId = this.inntektsId,
        inntektPerioder =
            listOf(
                InntektPeriodeDTO(
                    periodeType = "12 måneder",
                    fra = this.tolvMånederPeriode.førsteMåned.toString(),
                    til = this.tolvMånederPeriode.sisteMåned.toString(),
                    inntekt = this.tolvMånederPeriode.inntekt,
                ),
                InntektPeriodeDTO(
                    periodeType = "36 måneder",
                    fra = this.trettiseksMånederPeriode.førsteMåned.toString(),
                    til = this.trettiseksMånederPeriode.sisteMåned.toString(),
                    inntekt = this.trettiseksMånederPeriode.inntekt,
                ),
            ),
    )
}
