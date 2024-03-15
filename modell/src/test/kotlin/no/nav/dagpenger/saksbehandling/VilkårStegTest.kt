package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VilkårStegTest(beskrivendeId: String) {

    @Test
    fun `Tilstand oppfylt`() {
        val steg = object : VilkårSteg(
            "steg.testvilkaar",
            listOf(
                Opplysning(
                    navn = "Testvilkår",
                    verdi = "true",
                    dataType = "boolean",
                    status = OpplysningStatus.Faktum
                )
            )
            {

            }
    }

    @Test
    fun `Tilstand ikke oppfylt`() {
        val steg = Steg(
            beskrivendeId = "steg.testvilkaar",
            opplysninger = listOf(
                Opplysning(
                    navn = "Testvilkår",
                    verdi = "false",
                    dataType = "boolean",
                    status = OpplysningStatus.Faktum
                )
            ),
        )
        steg.tilstand shouldBe Steg.Tilstand.IKKE_OPPFYLT
    }

    @Test
    fun `Tilstand manuell behandling`() {
        val steg = Steg(
            beskrivendeId = "steg.testvilkaar",
            opplysninger = listOf(
                Opplysning(
                    navn = "Testvilkår",
                    verdi = "true",
                    dataType = "boolean",
                    status = OpplysningStatus.Hypotese
                )
            ),
        )
        steg.tilstand shouldBe Steg.Tilstand.MANUELL_BEHANDLING
        val steg2 = Steg(
            beskrivendeId = "steg.testvilkaar",
            opplysninger = listOf(
                Opplysning(
                    navn = "Testvilkår",
                    verdi = "false",
                    dataType = "boolean",
                    status = OpplysningStatus.Hypotese
                )
            ),
        )
        steg2.tilstand shouldBe Steg.Tilstand.MANUELL_BEHANDLING
    }
}