package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.GrupperNavn.FORMKRAV
import no.nav.dagpenger.saksbehandling.GrupperNavn.FRIST
import no.nav.dagpenger.saksbehandling.GrupperNavn.KLAGESAK
import org.junit.jupiter.api.Test

class KlageBehandlingTest {
    @Test
    fun `opprett klagebehandling`() {
        val klageBehandling =
            KlageBehandling(
                id = java.util.UUID.randomUUID(),
                person =
                    Person(
                        ident = "12345678901",
                        skjermesSomEgneAnsatte = false,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                    ),
            )
        klageBehandling.grupper.map { it.navn } shouldBe setOf(FORMKRAV, KLAGESAK, FRIST)
    }
}
