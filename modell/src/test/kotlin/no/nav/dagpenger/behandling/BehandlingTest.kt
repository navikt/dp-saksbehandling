package no.nav.dagpenger.behandling

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import no.nav.dagpenger.behandling.Meldingsfabrikk.testHendelse
import no.nav.dagpenger.behandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSporing
import no.nav.dagpenger.behandling.Steg.Companion.fastsettelse
import no.nav.dagpenger.behandling.Steg.Vilkår
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class BehandlingTest {
    @Test
    fun `Skal kunne lage en behandling`() {
        val steg1 = fastsettelse<Int>("1")
        val steg2 = Vilkår("2")

        assertEquals(
            setOf(steg1, steg2),
            Behandling(
                testPerson,
                testHendelse,
                setOf(
                    steg1,
                    steg2,
                ),
                sak = Sak(),
            ).nesteSteg(),
        )
    }

    @Test
    fun `Avhengige steg blir med i planen`() {
        val steg1 = Vilkår("1")
        val steg2 = Vilkår("2")
        val steg3 = Vilkår("3")
        val steg4 = Vilkår("4")
        val behandling = Behandling(
            testPerson,
            testHendelse,
            setOf(
                steg1.also { it.avhengerAv(steg2) },
                steg3.also { it.avhengerAv(steg4) },
            ),
            sak = Sak(),
        )

        assertEquals(setOf(steg1, steg2, steg3, steg4), behandling.alleSteg())
        assertEquals(setOf(steg2, steg4), behandling.nesteSteg())
    }

    @Test
    fun `Duplikate steg vises bare engang`() {
        val steg1 = Vilkår("1")
        val steg2 = Vilkår("2").also { it.avhengerAv(steg1) }
        val steg3 = Vilkår("3")
        val steg4 = Vilkår("4").also { it.avhengerAv(steg3) }
        val steg5 = Vilkår("5").also {
            it.avhengerAv(steg2)
            it.avhengerAv(steg4)
        }

        assertEquals(
            setOf(steg1, steg2, steg3, steg4, steg5),
            Behandling(testPerson, testHendelse, setOf(steg2, steg4, steg5), sak = Sak()).alleSteg(),
        )
    }

    @Test
    fun `Utførte steg blir ikke med i neste steg`() {
        val steg1 = fastsettelse<Int>("1")
        val steg2 = Vilkår("2").also {
            it.avhengerAv(steg1)
            it.besvar(true, testSporing)
        }
        val steg3 = Vilkår("3")
        val steg4 = Vilkår("4").avhengerAv(steg3)
        val steg5 = Vilkår("5").also {
            it.avhengerAv(steg1)
            it.besvar(true, testSporing)
        }

        assertEquals(
            setOf(steg3, steg4),
            Behandling(testPerson, testHendelse, steg = setOf(steg2, steg4, steg5), sak = Sak()).nesteSteg(),
        )
    }

    @Test
    fun `Steg nullstilles når avhengighet endres`() {
        val steg1 = fastsettelse<Int>("1")
        val steg2 = Vilkår("2").also {
            it.avhengerAv(steg1)
            it.besvar(true, testSporing)
        }
        val steg3 = Vilkår("3")
        val behandling = Behandling(testPerson, testHendelse, setOf(steg2, steg3), sak = Sak())
        assertEquals(setOf(steg3), behandling.nesteSteg())

        steg1.besvar(2, testSporing)
        assertEquals(setOf(steg3, steg2), behandling.nesteSteg())
    }

    @Test
    fun `Skal kunne besvare behandling`() {
        lateinit var testUuid: UUID
        val behandling = behandling(testPerson, testHendelse) {
            steg {
                fastsettelse<Int>("noe").also {
                    testUuid = it.uuid
                }
            }
        }

        behandling.besvar(testUuid, 5, testSporing)
        shouldThrow<NoSuchElementException> { behandling.besvar(UUID.randomUUID(), 5, testSporing) }
        shouldThrow<IllegalArgumentException> { behandling.besvar(testUuid, "String svar", testSporing) }
    }

    @Test
    fun `Skal kunne besvare vilkår og fastsettelse av type boolean`() {
        lateinit var vilkår: UUID
        lateinit var fastsettelse: UUID
        val behandling = behandling(testPerson, testHendelse) {
            steg {
                vilkår("noe").also { vilkår = it.uuid }
            }

            steg {
                fastsettelse<Boolean>("boolean").also { fastsettelse = it.uuid }
            }
        }

        shouldNotThrow<IllegalArgumentException> { behandling.besvar(vilkår, true, testSporing) }
        shouldNotThrow<IllegalArgumentException> { behandling.besvar(fastsettelse, false, testSporing) }
    }
}
