package no.nav.dagpenger.behandling.dsl

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingDSLTest {
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
        val felles = behandling.nesteSteg().single { it.id == "felles" }
        behandling.besvar(felles.uuid, LocalDate.now())
        behandling.nesteSteg().map { it.id } shouldBe setOf("første", "blarp", "blarpburp", "foobar")
    }
}
