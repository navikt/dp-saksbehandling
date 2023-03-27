package no.nav.dagpenger.behandling

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveApiMappingTest {

    @Test
    fun `mapping til dtoer`() {
        val person = Person("123")

        val testBehandling = behandling(person) {
            val fellessteg = steg {
                fastsettelse("felles steg")
            }
            steg {
                fastsettelse("fastsettelse 1 avhenger av vilkår") {
                    avhengerAvVilkår("vilkår1") {
                        avhengerAv(fellessteg)
                    }
                }
            }

            steg {
                vilkår("vilkår 2 avhenger av fastsettelse 2") {
                    avhengerAvFastsettelse("fastsettelse 2") {
                        avhengerAv(fellessteg)
                    }
                }
            }
        }

        testBehandling.toOppgaveDTO().let { dto ->
            dto.person shouldBe "123"
            dto.saksbehandler shouldBe null
            dto.opprettet shouldBe LocalDate.now()
            dto.hendelse shouldBe emptyList()

            dto.steg.size shouldBe 5
        }
    }
}
