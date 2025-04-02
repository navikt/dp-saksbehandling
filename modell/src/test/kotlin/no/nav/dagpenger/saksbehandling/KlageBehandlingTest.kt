package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.collections.shouldContainAll
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

        klageBehandling.grupper.map { it.navn } shouldContainAll listOf(FORMKRAV, KLAGESAK, FRIST)
        val opplysninngId = klageBehandling.grupper.single { it.navn == FORMKRAV }.opplysninger.first().id

        // do someting
        klageBehandling.svar(opplysninngId, false)
        klageBehandling.utfall shouldBe Utfall.Avvist
    }
}
