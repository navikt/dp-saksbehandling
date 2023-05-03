package no.nav.dagpenger.behandling.dsl

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Meldingsfabrikk.testHendelse
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSporing
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingDSLTest {
    @Test
    fun `dsl test`() {
        val behandling = behandling(Person("12345678910"), testHendelse) {
            val grandparentFelles = steg {
                fastsettelse<LocalDate>("GrandparentFelles")
            }

            steg {
                vilkår("Grandparent2") {
                    avhengerAv(grandparentFelles)
                }
            }

            steg {
                fastsettelse<Boolean>("Grandparent3") {
                    avhengerAvFastsettelse<Int>("Parent1")
                    avhengerAvFastsettelse<String>("Parent2") {
                        avhengerAvFastsettelse<String>("Child21")
                        avhengerAvVilkår("Child22")
                        avhengerAv(grandparentFelles)
                    }
                }
            }
        }

        behandling.steg.map { it.id } shouldBe setOf("GrandparentFelles", "Grandparent2", "Grandparent3")

        behandling.alleSteg().size shouldBe 7
        behandling.alleSteg().map { it.id } shouldBe setOf(
            "GrandparentFelles",
            "Grandparent2",
            "Grandparent3",
            "Parent1",
            "Parent2",
            "Child21",
            "Child22",
        )
        behandling.nesteSteg().map { it.id } shouldBe setOf(
            "GrandparentFelles",
            "Parent1",
            "Parent2",
            "Child21",
            "Child22",
        )
        // Besvare
        val grandparentFelles = behandling.nesteSteg().single { it.id == "GrandparentFelles" }
        behandling.besvar(grandparentFelles.uuid, LocalDate.now(), testSporing)
        behandling.nesteSteg().map { it.id } shouldBe setOf("Grandparent2", "Parent1", "Parent2", "Child21", "Child22")
        val grandparent2 = behandling.nesteSteg().single { it.id == "Grandparent2" }
        behandling.besvar(grandparent2.uuid, true, testSporing)
        behandling.nesteSteg().map { it.id } shouldBe setOf("Parent1", "Parent2", "Child21", "Child22")
        val parent1 = behandling.nesteSteg().single { it.id == "Parent1" }
        behandling.besvar(parent1.uuid, 3, testSporing)
        behandling.nesteSteg().map { it.id } shouldBe setOf("Parent2", "Child21", "Child22")
        val child21 = behandling.nesteSteg().single { it.id == "Child21" }
        behandling.besvar(child21.uuid, "Dette er et svare", testSporing)
        behandling.nesteSteg().map { it.id } shouldBe setOf("Parent2", "Child22")
        val child22 = behandling.nesteSteg().single { it.id == "Child22" }
        behandling.besvar(child22.uuid, true, testSporing)
        behandling.nesteSteg().map { it.id } shouldBe setOf("Parent2")
        val parent2 = behandling.nesteSteg().single { it.id == "Parent2" }
        behandling.besvar(parent2.uuid, "Dette er et svar også", testSporing)
        behandling.nesteSteg().map { it.id } shouldBe setOf("Grandparent3")
        val grandparent3 = behandling.nesteSteg().single { it.id == "Grandparent3" }
        behandling.besvar(grandparent3.uuid, true, testSporing)
        behandling.nesteSteg().map { it.id } shouldBe emptyList()
    }
}
