package no.nav.dagpenger.saksbehandling.mottak

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal fun String.readFile(): String =
    object {}.javaClass.getResource(this)?.readText()
        ?: throw RuntimeException("Fant ikke ressurs $this")

class BehandlingsresultatMapperTest {
    @Test
    fun `skal kunne finne periodisert verdi for boolsk opplysning`() {
        "/gjenopptak_permittering.json".readFile().also {
            val mapper = BehandlingsresultatMapper(it)
            mapper.prøvingsdato() shouldBe LocalDate.of(2026, 11, 9)
            mapper.finnesOpplysningMedVerdiTrue(
                OpplysningTyper.RETTIGHET_DAGPEGNER_UNDER_PERMITTERING_SKAL_VURDERES.opplysningTypeId,
            ) shouldBe
                true
            mapper.finnesOpplysningMedVerdiTrue(OpplysningTyper.RETTIGHET_ORDINÆRE_DAGPENGER.opplysningTypeId) shouldBe false
            mapper.finnesOpplysningMedVerdiTrue(UUID.randomUUID()) shouldBe false
        }
    }

    @Test
    fun `skal kunne finne periodisert verdi for boolsk opplysning som er false`() {
        "/gjenopptak_permittering.json".readFile().also {
            val mapper = BehandlingsresultatMapper(it)
            mapper.prøvingsdato() shouldBe LocalDate.of(2026, 11, 9)
            mapper.finnesOpplysningMedVerdiFalse(
                OpplysningTyper.RETTIGHET_DAGPEGNER_UNDER_PERMITTERING_SKAL_VURDERES.opplysningTypeId,
            ) shouldBe
                false
            mapper.finnesOpplysningMedVerdiFalse(OpplysningTyper.RETTIGHET_ORDINÆRE_DAGPENGER.opplysningTypeId) shouldBe true
            mapper.finnesOpplysningMedVerdiFalse(UUID.randomUUID()) shouldBe false
        }
    }
}
