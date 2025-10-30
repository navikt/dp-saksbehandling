package no.nav.dagpenger.saksbehandling.henvendelse

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.ModellTestHelper
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.FjernAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettManuellBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HenvendelseTest {
    @Test
    fun `Livssyklus til en henvendelse`() {
        val saksbehandler = ModellTestHelper.lagSaksbehandler()
        val testPerson = ModellTestHelper.lagPerson()
        val henvendelse =
            Henvendelse(
                person = testPerson,
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

        henvendelse.tildel(
            TildelHendelse(
                utførtAv = saksbehandler,
                ansvarligIdent = saksbehandler.navIdent,
            ),
        )

        henvendelse.tilstand() shouldBe Henvendelse.Tilstand.UnderBehandling
        henvendelse.behandlerIdent() shouldBe saksbehandler.navIdent
        henvendelse.tilstandslogg.size shouldBe 3

        henvendelse.ferdigstill(
            OpprettManuellBehandlingHendelse(
                manuellId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
                ident = testPerson.ident,
                opprettet = LocalDateTime.now(),
                basertPåBehandling = UUIDv7.ny(),
                utførtAv = saksbehandler,
            ),
        )

        henvendelse.tilstand() shouldBe Henvendelse.Tilstand.Ferdigbehandlet
        henvendelse.behandlerIdent() shouldBe saksbehandler.navIdent
        henvendelse.tilstandslogg.size shouldBe 4
    }
}
