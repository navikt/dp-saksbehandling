package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Meldingsfabrikk.testHendelse
import no.nav.dagpenger.saksbehandling.Meldingsfabrikk.testIdent
import no.nav.dagpenger.saksbehandling.Meldingsfabrikk.testPerson
import no.nav.dagpenger.saksbehandling.Meldingsfabrikk.testSporing
import no.nav.dagpenger.saksbehandling.Steg.Companion.fastsettelse
import no.nav.dagpenger.saksbehandling.Steg.Vilkår
import no.nav.dagpenger.saksbehandling.dsl.BehandlingDSL.Companion.behandling
import no.nav.dagpenger.saksbehandling.hendelser.VedtakStansetHendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
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
        val behandling =
            Behandling(
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
        val steg5 =
            Vilkår("5").also {
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
        val steg2 =
            Vilkår("2").also {
                it.avhengerAv(steg1)
                it.besvar(true, testSporing(listOf(Rolle.Saksbehandler)))
            }
        val steg3 = Vilkår("3")
        val steg4 = Vilkår("4").avhengerAv(steg3)
        val steg5 =
            Vilkår("5").also {
                it.avhengerAv(steg1)
                it.besvar(true, testSporing(listOf(Rolle.Saksbehandler)))
            }

        assertEquals(
            setOf(steg3, steg4),
            Behandling(testPerson, testHendelse, steg = setOf(steg2, steg4, steg5), sak = Sak()).nesteSteg(),
        )
    }

    @Test
    fun `Steg nullstilles når avhengighet endres`() {
        val steg1 = fastsettelse<Int>("1")
        val steg2 =
            Vilkår("2").also {
                it.avhengerAv(steg1)
                it.besvar(true, testSporing(listOf(Rolle.Saksbehandler)))
            }
        val steg3 = Vilkår("3")
        val behandling = Behandling(testPerson, testHendelse, setOf(steg2, steg3), sak = Sak())
        assertEquals(setOf(steg3), behandling.nesteSteg())

        steg1.besvar(2, testSporing(listOf(Rolle.Saksbehandler)))
        assertEquals(setOf(steg3, steg2), behandling.nesteSteg())
    }

    @Test
    fun `Skal kunne besvare behandling`() {
        lateinit var intUUID: UUID
        lateinit var vilkårUUID: UUID
        val behandling =
            behandling(testPerson, testHendelse, sak = Sak()) {
                steg {
                    fastsettelse<Int>("noe").also {
                        intUUID = it.uuid
                    }
                }
                steg {
                    vilkår("bær").also {
                        vilkårUUID = it.uuid
                    }
                }
            }

        behandling.erFerdig() shouldBe false
        behandling.besvar(vilkårUUID, true, testSporing(listOf(Rolle.Saksbehandler)))
        behandling.erFerdig() shouldBe false

        shouldThrow<NoSuchElementException> { behandling.besvar(UUID.randomUUID(), 5, testSporing(listOf(Rolle.Saksbehandler))) }
        shouldThrow<IllegalArgumentException> { behandling.besvar(intUUID, "String svar", testSporing(listOf(Rolle.Saksbehandler))) }
        shouldThrow<IllegalArgumentException> { behandling.besvar(intUUID, LocalDate.EPOCH, testSporing(listOf(Rolle.Saksbehandler))) }
        shouldThrow<IllegalArgumentException> { behandling.besvar(intUUID, false, testSporing(listOf(Rolle.Saksbehandler))) }
        shouldThrow<IllegalArgumentException> { behandling.besvar(intUUID, 2.2, testSporing(listOf(Rolle.Saksbehandler))) }

        behandling.besvar(intUUID, 5, testSporing(listOf(Rolle.Saksbehandler)))
        behandling.erFerdig() shouldBe true
    }

    @Test
    fun `Skal kunne besvare vilkår og fastsettelse av type boolean`() {
        lateinit var vilkår: UUID
        lateinit var fastsettelse: UUID
        val behandling =
            behandling(testPerson, testHendelse, sak = Sak()) {
                steg {
                    vilkår("noe").also { vilkår = it.uuid }
                }

                steg {
                    fastsettelse<Boolean>("boolean").also { fastsettelse = it.uuid }
                }
            }

        shouldNotThrow<IllegalArgumentException> { behandling.besvar(vilkår, true, testSporing(listOf(Rolle.Saksbehandler))) }
        shouldNotThrow<IllegalArgumentException> { behandling.besvar(fastsettelse, false, testSporing(listOf(Rolle.Saksbehandler))) }
    }

    @Test
    fun `Setter riktig type utfall ved stans`() {
        lateinit var fastsettelse: UUID

        val stansBehandling =
            behandling(testPerson, VedtakStansetHendelse(ident = testIdent, oppgaveId = UUID.randomUUID()), Sak()) {
                steg {
                    fastsettelse<LocalDate>("en fastsettelse").also { fastsettelse = it.uuid }
                }
            }

        stansBehandling.besvar(fastsettelse, LocalDate.now(), testSporing(listOf(Rolle.Saksbehandler)))
        stansBehandling.utfall() shouldBe Utfall.Stans
    }

    @Test
    fun `Setter riktig type utfall ved innvilgelse og avslag`() {
        lateinit var vilkår1: UUID
        lateinit var vilkår2: UUID

        val behandling =
            behandling(testPerson, testHendelse, Sak()) {
                steg {
                    vilkår("et vilkår").also { vilkår1 = it.uuid }
                }

                steg {
                    vilkår("et til vilkår").also { vilkår2 = it.uuid }
                }
            }

        behandling.besvar(vilkår1, true, testSporing(listOf(Rolle.Saksbehandler)))
        behandling.besvar(vilkår2, true, testSporing(listOf(Rolle.Saksbehandler)))
        behandling.utfall() shouldBe Utfall.Innvilgelse

        behandling.besvar(vilkår1, false, testSporing(listOf(Rolle.Saksbehandler)))
        behandling.utfall() shouldBe Utfall.Avslag
    }
}
