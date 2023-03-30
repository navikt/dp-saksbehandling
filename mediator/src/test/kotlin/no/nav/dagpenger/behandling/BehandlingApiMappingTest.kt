package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.StegtypeDTO.Fastsetting
import no.nav.dagpenger.behandling.StegtypeDTO.Vilkår
import no.nav.dagpenger.behandling.Tilstand.IkkeUtført
import no.nav.dagpenger.behandling.Tilstand.Utført
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingApiMappingTest {
    @Test
    fun `mapping til dtoer`() {
        val person = Person("123")
        val testBehandling = behandling(person) {
            val fellessteg = steg {
                fastsettelse<Boolean>("felles steg").also {
                    it.besvar(false)
                }
            }
            steg {
                fastsettelse<String>("fastsettelse 1 avhenger av vilkår") {
                    avhengerAvVilkår("vilkår1") {
                        avhengerAv(fellessteg)
                    }
                }
            }

            steg {
                vilkår("vilkår 2 avhenger av fastsettelse 2") {
                    avhengerAvFastsettelse<Int>("fastsettelse 2") {
                        avhengerAv(fellessteg)
                    }
                }
            }
        }

        testBehandling.toBehandlingDTO().let { dto ->
            dto.person shouldBe "123"
            dto.saksbehandler shouldBe null
            dto.opprettet shouldBe LocalDate.now()
            dto.hendelse shouldBe emptyList()

            dto.steg.size shouldBe 5
            dto.steg.count { it.type == Fastsetting } shouldBe 3
            dto.steg.count { it.type == Vilkår } shouldBe 2
            dto.steg.count { it.tilstand == IkkeUtført } shouldBe 4
            dto.steg.count { it.tilstand == Utført } shouldBe 1
            dto.steg.count { it.svartype == SvartypeDTO.Int } shouldBe 1
        }
    }
}
