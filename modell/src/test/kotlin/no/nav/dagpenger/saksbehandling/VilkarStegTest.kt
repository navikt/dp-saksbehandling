package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VilkarStegTest {

    @Test
    fun `VilkårSteg må ha en toppnode med boolsk verdi`() {
        val stringOpplysning = Opplysning(
            navn = "Testvilkår",
            verdi = "true",
            dataType = "string",
            status = OpplysningStatus.Faktum,
        )
        val booleanOpplysning = Opplysning(
            navn = "Testvilkår",
            verdi = "true",
            dataType = "boolean",
            status = OpplysningStatus.Faktum,
        )

        val ukjentOpplysning = Opplysning(
            navn = "Ukjent",
            verdi = "true",
            dataType = "boolean",
            status = OpplysningStatus.Faktum,
        )

        shouldThrow<IllegalStateException> {
            TestVilkårSteg(opplysninger = listOf(stringOpplysning, ukjentOpplysning))
        }

        shouldNotThrowAny {
            TestVilkårSteg(opplysninger = listOf(booleanOpplysning, stringOpplysning))
        }.toppnode shouldBe booleanOpplysning
    }

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

        TestVilkårSteg(opplysninger).tilstand shouldBe Steg.Tilstand.OPPFYLT
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

        TestVilkårSteg(opplysninger).tilstand shouldBe Steg.Tilstand.IKKE_OPPFYLT
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
        TestVilkårSteg(opplysninger1).tilstand shouldBe Steg.Tilstand.MANUELL_BEHANDLING

        val opplysninger2 = listOf(
            Opplysning(
                navn = "Testvilkår",
                verdi = "true",
                dataType = "boolean",
                status = OpplysningStatus.Hypotese,
            ),
        )

        TestVilkårSteg(opplysninger2).tilstand shouldBe Steg.Tilstand.MANUELL_BEHANDLING
    }

    private class TestVilkårSteg(opplysninger: List<Opplysning>) : VilkårSteg(
        beskrivendeId = "steg.testvilkaar",
        opplysninger = opplysninger,
        toppnodeNavn = "Testvilkår",
    )
}
