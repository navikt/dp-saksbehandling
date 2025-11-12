package no.nav.dagpenger.saksbehandling.innsending

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.saksbehandling.ModellTestHelper
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.Avbrutt
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.Ferdigbehandlet
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.UnderBehandling
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class InnsendingTest {
    @Test
    fun `Livssyklus til en innsending som ferdigstilles`() {
        val saksbehandler = ModellTestHelper.lagSaksbehandler()
        val testPerson = ModellTestHelper.lagPerson()
        val innsendingMottattHendelse =
            InnsendingMottattHendelse(
                ident = testPerson.ident,
                journalpostId = "jp12",
                registrertTidspunkt = LocalDateTime.now(),
                søknadId = null,
                skjemaKode = "skjemaKode",
                kategori = Kategori.GENERELL,
            )

        val innsending =
            Innsending.opprett(
                hendelse = innsendingMottattHendelse,
            ) { ident -> testPerson }

        shouldThrow<RuntimeException> {
            innsending.ferdigstill(
                innsendingFerdigstiltHendelse =
                    InnsendingFerdigstiltHendelse(
                        innsendingId = innsending.innsendingId,
                        aksjon = Aksjon.OpprettManuellBehandling::class.java.simpleName,
                        behandlingId = UUIDv7.ny(),
                        utførtAv = saksbehandler,
                    ),
            )
        }

        innsending.tildel(
            TildelHendelse(
                utførtAv = saksbehandler,
                ansvarligIdent = saksbehandler.navIdent,
                innsendingId = innsending.innsendingId,
            ),
        )

        innsending.tilstand() shouldBe UnderBehandling
        innsending.behandlerIdent() shouldBe saksbehandler.navIdent
        innsending.tilstandslogg.size shouldBe 2

        innsending.leggTilbake(FjernAnsvarHendelse(utførtAv = saksbehandler))

        innsending.tilstand() shouldBe KlarTilBehandling
        innsending.behandlerIdent() shouldBe null
        innsending.tilstandslogg.size shouldBe 3

        innsending.tildel(
            TildelHendelse(
                utførtAv = saksbehandler,
                ansvarligIdent = saksbehandler.navIdent,
                innsendingId = innsending.innsendingId,
            ),
        )

        innsending.tilstand() shouldBe UnderBehandling
        innsending.behandlerIdent() shouldBe saksbehandler.navIdent
        innsending.tilstandslogg.size shouldBe 4

        innsending.ferdigstill(
            innsendingFerdigstiltHendelse =
                InnsendingFerdigstiltHendelse(
                    innsendingId = innsending.innsendingId,
                    aksjon = Aksjon.OpprettManuellBehandling::class.java.simpleName,
                    behandlingId = UUIDv7.ny(),
                    utførtAv = saksbehandler,
                ),
        )

        innsending.tilstand() shouldBe Ferdigbehandlet
        innsending.behandlerIdent() shouldBe saksbehandler.navIdent
        innsending.tilstandslogg.size shouldBe 5
        innsending.tilstandslogg.first().hendelse.shouldBeInstanceOf<InnsendingFerdigstiltHendelse>()

        shouldThrow<RuntimeException> {
            innsending.ferdigstill(
                innsendingFerdigstiltHendelse =
                    InnsendingFerdigstiltHendelse(
                        innsendingId = innsending.innsendingId,
                        aksjon = Aksjon.OpprettManuellBehandling::class.java.simpleName,
                        behandlingId = UUIDv7.ny(),
                        utførtAv = saksbehandler,
                    ),
            )
        }
    }

    @Test
    fun `Livssyklus til en innsending som avbrytes`() {
        val saksbehandler = ModellTestHelper.lagSaksbehandler()
        val testPerson = ModellTestHelper.lagPerson()
        val innsendingMottattHendelse =
            InnsendingMottattHendelse(
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

        val innsending =
            Innsending.opprett(
                hendelse = innsendingMottattHendelse,
            ) { ident -> testPerson }

        innsending.tildel(
            TildelHendelse(
                utførtAv = saksbehandler,
                ansvarligIdent = saksbehandler.navIdent,
                innsendingId = innsending.innsendingId,
            ),
        )

        innsending.tilstand() shouldBe UnderBehandling
        innsending.behandlerIdent() shouldBe saksbehandler.navIdent
        innsending.tilstandslogg.size shouldBe 2

        shouldThrow<RuntimeException> {
            innsending.avbryt(behandlingOpprettetForSøknadHendelse)
        }

        innsending.leggTilbake(FjernAnsvarHendelse(utførtAv = saksbehandler))

        innsending.tilstand() shouldBe KlarTilBehandling
        innsending.behandlerIdent() shouldBe null
        innsending.tilstandslogg.size shouldBe 3

        innsending.avbryt(behandlingOpprettetForSøknadHendelse)

        innsending.tilstand() shouldBe Avbrutt
        innsending.behandlerIdent() shouldBe null
        innsending.tilstandslogg.size shouldBe 4
        innsending.tilstandslogg.first().hendelse.shouldBeInstanceOf<BehandlingOpprettetForSøknadHendelse>()

        shouldThrow<RuntimeException> {
            innsending.avbryt(behandlingOpprettetForSøknadHendelse)
        }
    }
}
