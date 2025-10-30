package no.nav.dagpenger.saksbehandling.henvendelse

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HenvendelseTest {
    @Test
    fun `Livssyklus  til en henvendelse`() {
        val henvendelse =
            Henvendelse(
                person =
                    Person(
                        ident = "12345678901",
                        skjermesSomEgneAnsatte = false,
                        adressebeskyttelseGradering = UGRADERT,
                    ),
                journalpostId = "JP123456",
                registrert = LocalDateTime.now(),
            )

        henvendelse.tildel(
            TildelHendelse(
                utførtAv =
                    Saksbehandler(
                        navIdent = "1234",
                        grupper = emptySet(),
                        tilganger = emptySet(),
                    ),
                ansvarligIdent = "1234",
            ),
        )

        henvendelse.tilstand() shouldBe Henvendelse.Tilstand.UnderBehandling
        henvendelse.behandlerIdent() shouldBe "1234"
    }
}
