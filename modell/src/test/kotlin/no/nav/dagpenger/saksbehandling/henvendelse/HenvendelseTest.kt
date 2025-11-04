package no.nav.dagpenger.saksbehandling.henvendelse

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.ModellTestHelper
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.FjernAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.KlageOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettManuellBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HenvendelseTest {
    @Test
    fun `Livssyklus til en generell henvendelse`() {
        val saksbehandler = ModellTestHelper.lagSaksbehandler()
        val testPerson = ModellTestHelper.lagPerson()
        val henvendelseMottattHendelse =
            HenvendelseMottattHendelse(
                ident = testPerson.ident,
                journalpostId = "jp12",
                registrertTidspunkt = LocalDateTime.now(),
                søknadId = null,
                skjemaKode = "skjemaKode",
                kategori = Kategori.GENERELL,
            )

        val henvendelse =
            Henvendelse.opprett(
                hendelse = henvendelseMottattHendelse,
            ) { ident -> testPerson }

        henvendelse.tildel(
            TildelHendelse(
                utførtAv = saksbehandler,
                ansvarligIdent = saksbehandler.navIdent,
            ),
        )

        henvendelse.tilstand() shouldBe Henvendelse.Tilstand.UnderBehandling
        henvendelse.behandlerIdent() shouldBe saksbehandler.navIdent
        henvendelse.tilstandslogg.size shouldBe 2

        henvendelse.leggTilbake(FjernAnsvarHendelse(utførtAv = saksbehandler))

        henvendelse.tilstand() shouldBe Henvendelse.Tilstand.KlarTilBehandling
        henvendelse.behandlerIdent() shouldBe null
        henvendelse.tilstandslogg.size shouldBe 3

        henvendelse.tildel(
            TildelHendelse(
                utførtAv = saksbehandler,
                ansvarligIdent = saksbehandler.navIdent,
            ),
        )

        henvendelse.tilstand() shouldBe Henvendelse.Tilstand.UnderBehandling
        henvendelse.behandlerIdent() shouldBe saksbehandler.navIdent
        henvendelse.tilstandslogg.size shouldBe 4

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
        henvendelse.tilstandslogg.size shouldBe 5
    }

    @Test
    fun `Livssyklus til en klage henvendelse`() {
        val saksbehandler = ModellTestHelper.lagSaksbehandler()
        val testPerson = ModellTestHelper.lagPerson()
        val sakId = UUIDv7.ny()
        val klageBehandlingId = UUIDv7.ny()
        val henvendelseMottattHendelse =
            HenvendelseMottattHendelse(
                ident = testPerson.ident,
                journalpostId = "jp12",
                registrertTidspunkt = LocalDateTime.now(),
                søknadId = null,
                skjemaKode = "skjemaKode",
                kategori = Kategori.KLAGE,
            )

        val henvendelse =
            Henvendelse.opprett(
                hendelse = henvendelseMottattHendelse,
            ) { ident -> testPerson }

        henvendelse.tildel(
            TildelHendelse(
                utførtAv = saksbehandler,
                ansvarligIdent = saksbehandler.navIdent,
            ),
        )

        henvendelse.tilstand() shouldBe Henvendelse.Tilstand.UnderBehandling
        henvendelse.behandlerIdent() shouldBe saksbehandler.navIdent
        henvendelse.tilstandslogg.size shouldBe 2

        henvendelse.ferdigstill(
            KlageOpprettetHendelse(
                behandlingId = klageBehandlingId,
                ident = testPerson.ident,
                mottatt = henvendelse.mottatt,
                journalpostId = henvendelse.journalpostId,
                sakId = sakId,
                utførtAv = saksbehandler,
            ),
        )

        henvendelse.tilstand() shouldBe Henvendelse.Tilstand.Ferdigbehandlet
        henvendelse.behandlerIdent() shouldBe saksbehandler.navIdent
        henvendelse.tilstandslogg.size shouldBe 3
    }
}
