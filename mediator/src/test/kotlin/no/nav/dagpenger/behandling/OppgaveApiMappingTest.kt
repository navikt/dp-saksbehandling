package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSporing
import no.nav.dagpenger.behandling.Tilstand.IkkeUtført
import no.nav.dagpenger.behandling.Tilstand.Utført
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import no.nav.dagpenger.behandling.dto.StegtypeDTO.Fastsetting
import no.nav.dagpenger.behandling.dto.StegtypeDTO.Vilkår
import no.nav.dagpenger.behandling.dto.SvartypeDTO
import no.nav.dagpenger.behandling.dto.toBehandlingDTO
import no.nav.dagpenger.behandling.hendelser.Hendelse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveApiMappingTest {
    @Test
    fun `mapping til dtoer`() {
        val person = Person("12345678910")
        val testBehandling = behandling(person, object : Hendelse("12312312311") {}) {
            val fellessteg = steg {
                fastsettelse<Boolean>("felles steg").also {
                    it.besvar(false, testSporing)
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
            dto.person shouldBe "12345678910"
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
