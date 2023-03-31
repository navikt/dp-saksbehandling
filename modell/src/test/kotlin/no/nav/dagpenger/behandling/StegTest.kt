package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Steg.Companion.fastsettelse
import no.nav.dagpenger.behandling.Steg.Vilkår
import org.junit.jupiter.api.Test

class StegTest {
    private val barn1 = fastsettelse<Int>("barn1")
    private val barn2 = fastsettelse<Int>("barn2")
    private val barn3 = Vilkår("barn3")
    private val mor = Vilkår("mor").also {
        it.avhengerAv(barn1)
        it.avhengerAv(barn2)
        it.avhengerAv(barn3)
    }
    private val far = fastsettelse<Int>("far").also {
        it.avhengerAv(barn1)
        it.avhengerAv(barn2)
        it.avhengerAv(barn3)
    }
    private val bestefar = fastsettelse<Int>("bestefar").also {
        it.avhengerAv(mor)
        it.avhengerAv(far)
    }

    @Test
    fun `alleSteg ska hente ut steget selv og alle avhengigheter`() {
        far.alleSteg() shouldBe setOf(far, barn1, barn2, barn3)
    }

    @Test
    fun `alleSteg skal fjerne duplikater`() {
        bestefar.alleSteg() shouldBe setOf(bestefar, mor, far, barn1, barn2, barn3)
    }

    @Test
    fun `nesteSteg skal hente ut avhengigheter som ikke er utførte`() {
        bestefar.alleSteg() shouldBe setOf(bestefar, mor, far, barn1, barn2, barn3)
        far.nesteSteg() shouldBe setOf(barn1, barn2, barn3)
        mor.nesteSteg() shouldBe setOf(barn1, barn2, barn3)

        far.besvar(2)
        far.nesteSteg() shouldBe emptySet()

        mor.besvar(true)
        mor.nesteSteg() shouldBe emptySet()

        bestefar.nesteSteg() shouldBe setOf(bestefar)

        bestefar.besvar(2)
        bestefar.nesteSteg() shouldBe emptySet()
    }

    @Test
    fun `nesteSteg skal fjerne duplikater`() {
        barn3.besvar(true)
        bestefar.nesteSteg() shouldBe setOf(mor, far, barn1, barn2)
    }

    @Test
    fun `allesteg skal hent ut steg som er utført`() {
        mor.besvar(true)
        far.besvar(2)

        bestefar.alleSteg() shouldBe setOf(bestefar, far, mor, barn1, barn2, barn3)
    }

    @Test
    fun `Dersom svaret til et steg endres resettes tilstand til forfedre`() {
        far.besvar(1)
        far.nesteSteg() shouldBe emptySet()
        mor.besvar(false)
        mor.nesteSteg() shouldBe emptySet()
        bestefar.besvar(1)
        bestefar.nesteSteg() shouldBe emptySet()

        barn1.besvar(2)
        far.nesteSteg() shouldBe setOf(barn2, barn3)
        mor.nesteSteg() shouldBe setOf(barn2, barn3)
        bestefar.nesteSteg() shouldBe setOf(far, mor, barn2, barn3)
    }
}
