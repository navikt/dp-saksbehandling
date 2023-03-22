package no.nav.dagpenger.behandling

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BehandlingTest {
    @Test
    fun `Skal kunne lage en behandling`() {
        val steg = setOf(
            Steg(
                id = "1",
            ),
            Steg(id = "2"),
        )
        assertEquals(steg, Behandling(steg).nesteSteg())
    }

    @Test
    fun `Avhengige steg blir med i planen`() {
        val steg1 = Steg("1")
        val steg2 = Steg(id = "2", avhengerAv = setOf(steg1))
        val steg3 = Steg("3")
        val steg4 = Steg("4", avhengerAv = setOf(steg3))
        assertEquals(setOf(steg1, steg2, steg3, steg4), Behandling(setOf(steg2, steg4)).nesteSteg())
    }

    @Test
    fun `Duplikate steg vises bare engang`() {
        val steg1 = Steg("1")
        val steg2 = Steg(id = "2", avhengerAv = setOf(steg1))
        val steg3 = Steg("3")
        val steg4 = Steg("4", avhengerAv = setOf(steg3))
        val steg5 = Steg("5", avhengerAv = setOf(steg2, steg4))

        assertEquals(setOf(steg1, steg2, steg3, steg4, steg5), Behandling(setOf(steg2, steg4, steg5)).nesteSteg())
    }

    @Test
    fun `ferdig steg blir ikke med i neste steg`() {
        val steg1 = Steg(id = "1")
        val steg2 = Steg(id = "2", svar = Svar(true), avhengerAv = setOf(steg1))

        val steg3 = Steg("3")
        val steg4 = Steg("4", avhengerAv = setOf(steg3))
        val steg5 = Steg(id = "5", svar = Svar(true), avhengerAv = setOf(steg1))

        assertEquals(setOf(steg3, steg4), Behandling(steg = setOf(steg2, steg4, steg5)).nesteSteg())
    }

    @Test
    fun `Steg nullstilles når avhengighet endres`() {
        val steg1 = Steg("1")
        val steg2 = Steg(id = "2", svar = Svar(true), avhengerAv = setOf(steg1))
        val steg3 = Steg("3")
        val behandling = Behandling(setOf(steg2, steg3))
        assertEquals(setOf(steg3), behandling.nesteSteg())

        steg1.besvar(Svar(true))
        assertEquals(setOf(steg3, steg2), behandling.nesteSteg())
    }
}
