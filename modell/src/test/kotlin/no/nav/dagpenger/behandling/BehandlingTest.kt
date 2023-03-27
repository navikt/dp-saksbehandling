package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Steg.FastSettelse
import no.nav.dagpenger.behandling.Steg.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BehandlingTest {

    private val testPerson = Person("123")

    @Test
    fun `Skal kunne lage en behandling`() {
        val steg1 = FastSettelse("1")
        val steg2 = Vilkår("2")

        assertEquals(
            setOf(steg1, steg2),
            behandling(testPerson) {
                steg(steg1)
                steg(steg2)
            }.nesteSteg(),
        )
    }

    @Test
    fun `Avhengige steg blir med i planen`() {
        val steg1 = Vilkår("1")
        val steg2 = Vilkår(id = "2")
        val steg3 = Vilkår("3")
        val steg4 = Vilkår("4")

        val behandling = behandling(testPerson) {
            steg(steg1) {
                avhengerAv(steg2)
            }
            steg(steg3) {
                avhengerAv(steg4)
            }
        }

        assertEquals(setOf(steg1, steg2, steg3, steg4), behandling.alleSteg())
        assertEquals(setOf(steg2, steg4), behandling.nesteSteg())
    }

    @Test
    fun `Duplikate steg vises bare engang`() {
        val steg1 = Vilkår("1")
        val steg2 = Vilkår(id = "2").also { it.avhengerAv(steg1) }
        val steg3 = Vilkår("3")
        val steg4 = Vilkår("4").also { it.avhengerAv(steg3) }
        val steg5 = Vilkår("5").also {
            it.avhengerAv(steg2)
            it.avhengerAv(steg4)
        }

        assertEquals(
            setOf(steg1, steg2, steg3, steg4, steg5),
            Behandling(testPerson, setOf(steg2, steg4, steg5)).alleSteg(),
        )
    }

    @Test
    fun `ferdig steg blir ikke med i neste steg`() {
        val steg1 = FastSettelse(id = "1")
        val steg2 = Vilkår(id = "2", svar = Svar(true)).also {
            it.avhengerAv(steg1)
        }

        val steg3 = Vilkår("3")
        val steg4 = Vilkår("4").avhengerAv(steg3)
        val steg5 = Vilkår(id = "5", svar = Svar(true)).also {
            it.avhengerAv(steg1)
        }

        assertEquals(setOf(steg3, steg4), Behandling(testPerson, steg = setOf(steg2, steg4, steg5)).nesteSteg())
    }

    @Test
    fun `Steg nullstilles når avhengighet endres`() {
        val steg1 = FastSettelse("1")
        val steg2 = Vilkår(id = "2", svar = Svar(true)).also { it.avhengerAv(steg1) }
        val steg3 = Vilkår("3")
        val behandling = Behandling(testPerson, setOf(steg2, steg3))
        assertEquals(setOf(steg3), behandling.nesteSteg())

        steg1.besvar(Svar(true))
        assertEquals(setOf(steg3, steg2), behandling.nesteSteg())
    }

//    @Test
//    fun `steg dsl test`() {
//        steg(id = "parent") {
//            avhengerAvFastsettelse("child") {
//                avhengerAvFastsettelse("grandchild1")
//                avhengerAvFastsettelse("grandchild2")
//            }
//        }.nesteSteg().map { it.id } shouldBe listOf("parent", "child", "grandchild1", "grandchild2")
//
//        steg("parent") {
//            avhengerAv(steg("child")) {
//                avhengerAv(steg("grandchild1"))
//                avhengerAvFastsettelse("grandchild2")
//            }
//        }.nesteSteg().map { it.id } shouldBe listOf("parent", "child", "grandchild1", "grandchild2")
//    }
//
//    @Test
//    fun `behandling dsl test 2`() {
//        behandling {
//            val stegA = steg("a")
//
//            steg("b") {
//                avhengerAvFastsettelse("c") {
//                    avhengerAvFastsettelse("d")
//                }
//            }
//
//            steg("1") {
//                avhengerAv(stegA)
//                avhengerAvFastsettelse("2")
//            }
//        }.nesteSteg().map { it.id } shouldBe listOf("a", "b", "c", "d", "1", "2")
//    }
}
