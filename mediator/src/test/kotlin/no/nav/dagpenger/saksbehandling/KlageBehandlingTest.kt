package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import org.junit.jupiter.api.Test

class KlageBehandlingTest {
    @Test
    fun `Utfall er synlig når foregående steg er utfylt`() {
        val person: Person =
            Person(
                ident = "12345612345",
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            )
        val klageBehandling = KlageBehandling(person = person)
        klageBehandling.synligeOpplysninger().filter { it.type in OpplysningerBygger.utfallOpplysningTyper }.size shouldBe 2

    }
}
