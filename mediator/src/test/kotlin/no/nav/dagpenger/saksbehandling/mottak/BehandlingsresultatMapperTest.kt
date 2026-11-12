package no.nav.dagpenger.saksbehandling.mottak

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal fun String.readFile(): String =
    object {}.javaClass.getResource(this)?.readText()
        ?: throw RuntimeException("Fant ikke ressurs $this")

class BehandlingsresultatMapperTest {
    @Test
    fun `skal kunne finne periodisert verdi for boolsk opplysning`() {
        "/gjenopptak_permittering.json".readFile().also {
            val mapper = BehandlingsresultatMapper(it)
            mapper.prøvingsdato() shouldBe LocalDate.of(2026, 11, 9)
            mapper.erOpplysningTrue(OpplysningTyper.RETTIGHET_DAGPEGNER_UNDER_PERMITTERING.opplysningTypeId) shouldBe true
            mapper.erOpplysningTrue(OpplysningTyper.RETTIGHET_ORDINÆRE_DAGPENGER.opplysningTypeId) shouldBe false
        }
    }
}
