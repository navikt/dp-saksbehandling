package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VilkårStegTest {

    @Test
    fun `Tilstand oppfylt`() {
        val opplysninger = listOf(
            Opplysning(
                navn = "Testvilkår",
                verdi = "true",
                dataType = "boolean",
                status = OpplysningStatus.Faktum,
            ),
        )

        TestVilkår(opplysninger).tilstand shouldBe Steg.Tilstand.OPPFYLT
    }

    @Test
    fun `Tilstand ikke oppfylt`() {
        val opplysninger = listOf(
            Opplysning(
                navn = "Testvilkår",
                verdi = "false",
                dataType = "boolean",
                status = OpplysningStatus.Faktum,
            ),
        )

        TestVilkår(opplysninger).tilstand shouldBe Steg.Tilstand.IKKE_OPPFYLT
    }

    @Test
    fun `Tilstand manuell behandling`() {
        val opplysninger1 = listOf(
            Opplysning(
                navn = "Testvilkår",
                verdi = "false",
                dataType = "boolean",
                status = OpplysningStatus.Hypotese,
            ),
        )
        TestVilkår(opplysninger1).tilstand shouldBe Steg.Tilstand.MANUELL_BEHANDLING

        val opplysninger2 = listOf(
            Opplysning(
                navn = "Testvilkår",
                verdi = "true",
                dataType = "boolean",
                status = OpplysningStatus.Hypotese,
            ),
        )

        TestVilkår(opplysninger2).tilstand shouldBe Steg.Tilstand.MANUELL_BEHANDLING
    }

    private class TestVilkår(opplysninger: List<Opplysning>) : VilkårSteg(
        beskrivendeId = "steg.testvilkaar",
        opplysninger = opplysninger,
    ) {
        override val rotNodeNavn: String = "Testvilkår"
    }
}
