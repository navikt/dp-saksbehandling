package no.nav.dagpenger.saksbehandling.henvendelse

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.ModellTestHelper
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.hendelser.FjernAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HenvendelseTest {
    @Test
    fun `Livssyklus til en henvendelse`() {
        val saksbehandler = ModellTestHelper.lagSaksbehandler()
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
                utførtAv = saksbehandler,
                ansvarligIdent = saksbehandler.navIdent,
            ),
        )

        henvendelse.tilstand() shouldBe Henvendelse.Tilstand.UnderBehandling
        henvendelse.behandlerIdent() shouldBe saksbehandler.navIdent
        henvendelse.tilstandslogg.size shouldBe 1

        henvendelse.leggTilbake(FjernAnsvarHendelse(utførtAv = saksbehandler))

        henvendelse.tilstand() shouldBe Henvendelse.Tilstand.KlarTilBehandling
        henvendelse.behandlerIdent() shouldBe null
        henvendelse.tilstandslogg.size shouldBe 2
    }
}
