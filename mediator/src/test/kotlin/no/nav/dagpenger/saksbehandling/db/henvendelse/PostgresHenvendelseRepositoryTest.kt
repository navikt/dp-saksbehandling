package no.nav.dagpenger.saksbehandling.db.henvendelse

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.testPerson
import no.nav.dagpenger.saksbehandling.hendelser.FjernAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.OpprettManuellBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import no.nav.dagpenger.saksbehandling.hendelser.KlageOpprettetHendelse

class PostgresHenvendelseRepositoryTest {
    @Test
    fun `Skal lagre endre og hente henvendelse fra database`() {
        DBTestHelper.withPerson { ds ->
            val nå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
            val saksbehandler =
                Saksbehandler(
                    navIdent = "Z12345",
                    grupper = emptySet(),
                    tilganger = emptySet(),
                )
            val repository = PostgresHenvendelseRepository(ds)
            val henvendelseMottattHendelse =
                HenvendelseMottattHendelse(
                    ident = testPerson.ident,
                    journalpostId = "jp12",
                    registrertTidspunkt = nå,
                    søknadId = null,
                    skjemaKode = "skjemaKode",
                    kategori = Kategori.GENERELL,
                )
            val henvendelse =
                Henvendelse.opprett(
                    hendelse = henvendelseMottattHendelse,
                    personProvider = { ident -> testPerson },
                )
            repository.lagre(
                henvendelse = henvendelse,
            )
            repository.hent(henvendelse.henvendelseId) shouldBe henvendelse

            henvendelse.tildel(
                tildelHendelse =
                    TildelHendelse(
                        utførtAv = saksbehandler,
                        ansvarligIdent = "ABS123",
                    ),
            )
            repository.lagre(henvendelse)

            repository.hent(henvendelse.henvendelseId) shouldBe henvendelse

            henvendelse.leggTilbake(
                fjernAnsvarHendelse =
                    FjernAnsvarHendelse(
                        utførtAv = saksbehandler,
                    ),
            )
            repository.lagre(henvendelse)

            repository.hent(henvendelse.henvendelseId) shouldBe henvendelse

            henvendelse.tildel(
                tildelHendelse =
                    TildelHendelse(
                        utførtAv = saksbehandler,
                        ansvarligIdent = "ABS123",
                    ),
            )
            repository.lagre(henvendelse)

            repository.hent(henvendelse.henvendelseId) shouldBe henvendelse

            henvendelse.ferdigstill(
                opprettManuellBehandlingHendelse =
                    OpprettManuellBehandlingHendelse(
                        manuellId = UUIDv7.ny(),
                        behandlingId = UUIDv7.ny(),
                        ident = testPerson.ident,
                        opprettet = nå,
                        basertPåBehandling = UUIDv7.ny(),
                        utførtAv = saksbehandler,
                    ),
            )
            repository.lagre(henvendelse)

            repository.hent(henvendelse.henvendelseId) shouldBe henvendelse
        }
    }

    @Test
    fun `Skal lagre endre og hente klage-henvendelse fra database`() {
        DBTestHelper.withPerson { ds ->
            val nå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
            val saksbehandler =
                Saksbehandler(
                    navIdent = "Z12345",
                    grupper = emptySet(),
                    tilganger = emptySet(),
                )
            val sakId = UUIDv7.ny()
            val repository = PostgresHenvendelseRepository(ds)
            val henvendelseMottattHendelse =
                HenvendelseMottattHendelse(
                    ident = testPerson.ident,
                    journalpostId = "jp12",
                    registrertTidspunkt = nå,
                    søknadId = null,
                    skjemaKode = "skjemaKode",
                    kategori = Kategori.KLAGE,
                )
            val henvendelse =
                Henvendelse.opprett(
                    hendelse = henvendelseMottattHendelse,
                    personProvider = { ident -> testPerson },
                )
            repository.lagre(
                henvendelse = henvendelse,
            )
            repository.hent(henvendelse.henvendelseId) shouldBe henvendelse

            henvendelse.tildel(
                tildelHendelse =
                    TildelHendelse(
                        utførtAv = saksbehandler,
                        ansvarligIdent = "ABS123",
                    ),
            )
            repository.lagre(henvendelse)
            repository.hent(henvendelse.henvendelseId) shouldBe henvendelse

            henvendelse.ferdigstill(
                klageOpprettetHendelse =
                    KlageOpprettetHendelse(
                        behandlingId = UUIDv7.ny(),
                        ident = testPerson.ident,
                        mottatt = henvendelseMottattHendelse.registrertTidspunkt,
                        journalpostId = henvendelseMottattHendelse.journalpostId,
                        sakId = sakId,
                        utførtAv = saksbehandler,
                    ),
            )
            repository.lagre(henvendelse)

            repository.hent(henvendelse.henvendelseId) shouldBe henvendelse
        }
    }

}
