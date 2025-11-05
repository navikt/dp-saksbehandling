package no.nav.dagpenger.saksbehandling.henvendelse

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.saksbehandling.ModellTestHelper
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.Avbrutt
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.Ferdigbehandlet
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.UnderBehandling
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HenvendelseTest {
    @Test
    fun `Livssyklus til en henvendelse som ferdigstilles`() {
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

        shouldThrow<RuntimeException> {
            henvendelse.ferdigstill(
                henvendelseFerdigstiltHendelse =
                    HenvendelseFerdigstiltHendelse(
                        henvendelseId = henvendelse.henvendelseId,
                        aksjon = Aksjon.OpprettManuellBehandling::class.java.simpleName,
                        behandlingId = UUIDv7.ny(),
                        utførtAv = saksbehandler,
                    ),
            )
        }

        henvendelse.tildel(
            TildelHendelse(
                utførtAv = saksbehandler,
                ansvarligIdent = saksbehandler.navIdent,
                henvendelseId = henvendelse.henvendelseId,
            ),
        )

        henvendelse.tilstand() shouldBe UnderBehandling
        henvendelse.behandlerIdent() shouldBe saksbehandler.navIdent
        henvendelse.tilstandslogg.size shouldBe 2

        henvendelse.leggTilbake(FjernAnsvarHendelse(utførtAv = saksbehandler))

        henvendelse.tilstand() shouldBe KlarTilBehandling
        henvendelse.behandlerIdent() shouldBe null
        henvendelse.tilstandslogg.size shouldBe 3

        henvendelse.tildel(
            TildelHendelse(
                utførtAv = saksbehandler,
                ansvarligIdent = saksbehandler.navIdent,
                henvendelseId = henvendelse.henvendelseId,
            ),
        )

        henvendelse.tilstand() shouldBe UnderBehandling
        henvendelse.behandlerIdent() shouldBe saksbehandler.navIdent
        henvendelse.tilstandslogg.size shouldBe 4

        henvendelse.ferdigstill(
            henvendelseFerdigstiltHendelse =
                HenvendelseFerdigstiltHendelse(
                    henvendelseId = henvendelse.henvendelseId,
                    aksjon = Aksjon.OpprettManuellBehandling::class.java.simpleName,
                    behandlingId = UUIDv7.ny(),
                    utførtAv = saksbehandler,
                ),
        )

        henvendelse.tilstand() shouldBe Ferdigbehandlet
        henvendelse.behandlerIdent() shouldBe saksbehandler.navIdent
        henvendelse.tilstandslogg.size shouldBe 5
        henvendelse.tilstandslogg.first().hendelse.shouldBeInstanceOf<HenvendelseFerdigstiltHendelse>()

        shouldThrow<RuntimeException> {
            henvendelse.ferdigstill(
                henvendelseFerdigstiltHendelse =
                    HenvendelseFerdigstiltHendelse(
                        henvendelseId = henvendelse.henvendelseId,
                        aksjon = Aksjon.OpprettManuellBehandling::class.java.simpleName,
                        behandlingId = UUIDv7.ny(),
                        utførtAv = saksbehandler,
                    ),
            )
        }
    }

    @Test
    fun `Livssyklus til en henvendelse som avbrytes`() {
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
        val behandlingOpprettetForSøknadHendelse =
            BehandlingOpprettetForSøknadHendelse(
                ident = testPerson.ident,
                søknadId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
            )

        val henvendelse =
            Henvendelse.opprett(
                hendelse = henvendelseMottattHendelse,
            ) { ident -> testPerson }

        henvendelse.tildel(
            TildelHendelse(
                utførtAv = saksbehandler,
                ansvarligIdent = saksbehandler.navIdent,
                henvendelseId = henvendelse.henvendelseId,
            ),
        )

        henvendelse.tilstand() shouldBe UnderBehandling
        henvendelse.behandlerIdent() shouldBe saksbehandler.navIdent
        henvendelse.tilstandslogg.size shouldBe 2

        shouldThrow<RuntimeException> {
            henvendelse.avbryt(behandlingOpprettetForSøknadHendelse)
        }

        henvendelse.leggTilbake(FjernAnsvarHendelse(utførtAv = saksbehandler))

        henvendelse.tilstand() shouldBe KlarTilBehandling
        henvendelse.behandlerIdent() shouldBe null
        henvendelse.tilstandslogg.size shouldBe 3

        henvendelse.avbryt(behandlingOpprettetForSøknadHendelse)

        henvendelse.tilstand() shouldBe Avbrutt
        henvendelse.behandlerIdent() shouldBe null
        henvendelse.tilstandslogg.size shouldBe 4
        henvendelse.tilstandslogg.first().hendelse.shouldBeInstanceOf<BehandlingOpprettetForSøknadHendelse>()

        shouldThrow<RuntimeException> {
            henvendelse.avbryt(behandlingOpprettetForSøknadHendelse)
        }
    }
}
