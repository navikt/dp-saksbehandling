package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VilkårStegTest {

    companion object {
        private val testvilkårToppnodeNavn = "Testvilkår"
    }

    @Test
    fun `VilkårSteg må ha en bestemt toppnode med boolsk verdi`() {
        shouldThrow<IllegalStateException> {
            TestVilkårSteg(opplysninger = listOf(stringFaktum, bolskFaktum))
        }

        shouldNotThrowAny {
            TestVilkårSteg(opplysninger = listOf(testvilkårOppfyltFaktum, stringFaktum))
        }.toppnode shouldBe testvilkårOppfyltFaktum
    }

    @Test
    fun `Tilstand oppfylt`() {
        val opplysninger = listOf(testvilkårOppfyltFaktum)
        TestVilkårSteg(opplysninger).tilstand shouldBe Steg.Tilstand.OPPFYLT
    }

    @Test
    fun `Tilstand ikke oppfylt`() {
        val opplysninger = listOf(testvilkårIkkeOppfyltFaktum)
        TestVilkårSteg(opplysninger).tilstand shouldBe Steg.Tilstand.IKKE_OPPFYLT
    }

    @Test
    fun `Tilstand manuell behandling`() {
        val opplysninger1 = listOf(testvilkårIkkeOppfyltHypotese)
        TestVilkårSteg(opplysninger1).tilstand shouldBe Steg.Tilstand.MANUELL_BEHANDLING

        val opplysninger2 = listOf(testvilkårOppfyltHypotese)
        TestVilkårSteg(opplysninger2).tilstand shouldBe Steg.Tilstand.MANUELL_BEHANDLING
    }

    private class TestVilkårSteg(opplysninger: List<Opplysning>) : VilkårSteg(
        beskrivendeId = "steg.testvilkaar",
        opplysninger = opplysninger,
        toppnodeNavn = testvilkårToppnodeNavn,
    )

    private val testvilkårOppfyltFaktum = Opplysning(
        navn = testvilkårToppnodeNavn,
        verdi = "true",
        dataType = DataType.Boolean,
        status = OpplysningStatus.Faktum,
    )
    private val testvilkårIkkeOppfyltFaktum = Opplysning(
        navn = testvilkårToppnodeNavn,
        verdi = "false",
        dataType = DataType.Boolean,
        status = OpplysningStatus.Faktum,
    )
    private val testvilkårOppfyltHypotese = Opplysning(
        navn = testvilkårToppnodeNavn,
        verdi = "true",
        dataType = DataType.Boolean,
        status = OpplysningStatus.Hypotese,
    )
    private val testvilkårIkkeOppfyltHypotese = Opplysning(
        navn = testvilkårToppnodeNavn,
        verdi = "false",
        dataType = DataType.Boolean,
        status = OpplysningStatus.Hypotese,
    )
    private val stringFaktum = Opplysning(
        navn = "String opplysning",
        verdi = "Dette er en fin tekst",
        dataType = DataType.String,
        status = OpplysningStatus.Faktum,
    )

    private val bolskFaktum = Opplysning(
        navn = "Bolsk opplysning som ikke er toppnode",
        verdi = "true",
        dataType = DataType.Boolean,
        status = OpplysningStatus.Faktum,
    )
}
