package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Steg.Companion.fastsettelse
import no.nav.dagpenger.behandling.Steg.Fastsettelse
import no.nav.dagpenger.behandling.Steg.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingTest {
    private val testPerson = Person("123")

    @Test
    fun `Skal kunne lage en behandling`() {
        val steg1 = fastsettelse<Int>("1")
        val steg2 = Vilkår("2")

        assertEquals(
            setOf(steg1, steg2),
            Behandling(
                testPerson,
                setOf(
                    steg1,
                    steg2,
                ),
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
            setOf(
                steg1.also { it.avhengerAv(steg2) },
                steg3.also { it.avhengerAv(steg4) },
            ),
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
            Behandling(testPerson, setOf(steg2, steg4, steg5)).alleSteg(),
        )
    }

    @Test
    fun `ferdig steg blir ikke med i neste steg`() {
        val steg1 = fastsettelse<Int>("1")
        val steg2 = Vilkår("2").also {
            it.avhengerAv(steg1)
            it.besvar(true)
        }
        val steg3 = Vilkår("3")
        val steg4 = Vilkår("4").avhengerAv(steg3)
        val steg5 = Vilkår("5").also {
            it.avhengerAv(steg1)
            it.besvar(true)
        }

        assertEquals(setOf(steg3, steg4), Behandling(testPerson, steg = setOf(steg2, steg4, steg5)).nesteSteg())
    }

    @Test
    fun `Steg nullstilles når avhengighet endres`() {
        val steg1 = fastsettelse<Int>("1")
        val steg2 = Vilkår("2").also {
            it.avhengerAv(steg1)
            it.besvar(true)
        }
        val steg3 = Vilkår("3")
        val behandling = Behandling(testPerson, setOf(steg2, steg3))
        assertEquals(setOf(steg3), behandling.nesteSteg())

        steg1.besvar(2)
        assertEquals(setOf(steg3, steg2), behandling.nesteSteg())
    }

    @Test
    fun `dsl test`() {
        val behandling = behandling(Person("123")) {
            val felles = steg {
                fastsettelse<LocalDate>("felles")
            }

            steg {
                vilkår("første") {
                    avhengerAv(felles)
                }
            }

            steg {
                fastsettelse<Boolean>("blurp") {
                    avhengerAvFastsettelse<Int>("blarp")
                    avhengerAvFastsettelse<String>("burp") {
                        avhengerAvFastsettelse<String>("blarpburp")
                        avhengerAvVilkår("foobar")
                        avhengerAv(felles)
                    }
                }
            }
        }

        behandling.alleSteg().size shouldBe 7
        behandling.nesteSteg().map { it.id } shouldBe setOf("felles", "blarp", "blarpburp", "foobar")
        behandling.nesteSteg().size shouldBe 4
        // Besvare
        val felles = behandling.nesteSteg().single { it.id == "felles" } as Fastsettelse<LocalDate>
        felles.besvar(LocalDate.now())
        behandling.nesteSteg().map { it.id } shouldBe setOf("første", "blarp", "blarpburp", "foobar")
    }
}
