package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SikkerhetstiltakTest {
    @Test
    fun `Dato før eller lik gyldigFom`() {
        val nå = LocalDate.now()
        val sikkerhetstiltak =
            Sikkerhetstiltak(
                type = "Sikkerhetstiltak",
                beskrivelse = "Testbeskrivelse",
                gyldigFom = nå,
                gyldigTom = nå.plusDays(1),
            )
        sikkerhetstiltak.erGyldig(nå.minusDays(1)) shouldBe false
        sikkerhetstiltak.erGyldig(nå) shouldBe true
    }

    @Test
    fun `Sikkerhetstiltakets gyldighetsperiode`() {
        val nå = LocalDate.now()
        val sikkerhetstiltak =
            Sikkerhetstiltak(
                type = "Sikkerhetstiltak",
                beskrivelse = "Testbeskrivelse",
                gyldigFom = nå,
                gyldigTom = nå.plusDays(10),
            )

        sikkerhetstiltak.erGyldig(nå) shouldBe true
        sikkerhetstiltak.erGyldig(nå.plusDays(10)) shouldBe true
        sikkerhetstiltak.erGyldig(nå.plusDays(11)) shouldBe false
        sikkerhetstiltak.erGyldig(nå.minusDays(1)) shouldBe false
    }
}
