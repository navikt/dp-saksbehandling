package no.nav.dagpenger.behandling

import com.fasterxml.uuid.Generators
import java.time.LocalDate
import java.util.UUID


class MinsteinntektVurdering(
    val uuid: UUID = Generators.timeBasedEpochGenerator().generate(),
    val virkningsdato: LocalDate,
    val vilkaarOppfylt: Boolean,
    val inntektsId: String,
    val inntektPerioder: List<InntektPeriode>,
    val vurderingReferanse: String
) {

}

data class InntektPeriode(
    val periodeType: String,
    val fra: String,
    val til: String,
    val inntekt: Double,
)