package no.nav.dagpenger.saksbehandling.db.henvendelse

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.testPerson
import no.nav.dagpenger.saksbehandling.hendelser.FjernAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.Ferdigbehandlet
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand.UnderBehandling
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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

            repository.lagre(henvendelse = henvendelse)

            repository.hent(henvendelse.henvendelseId).also { dbHenvendelse ->
                dbHenvendelse.tilstand() shouldBe KlarTilBehandling
                dbHenvendelse shouldBe henvendelse
            }

            henvendelse.tildel(
                tildelHendelse =
                    TildelHendelse(
                        utførtAv = saksbehandler,
                        ansvarligIdent = saksbehandler.navIdent,
                    ),
            )
            repository.lagre(henvendelse)

            repository.hent(henvendelse.henvendelseId).also { dbHenvendelse ->
                dbHenvendelse.tilstand() shouldBe UnderBehandling
                dbHenvendelse shouldBe henvendelse
            }

            henvendelse.leggTilbake(
                fjernAnsvarHendelse =
                    FjernAnsvarHendelse(
                        utførtAv = saksbehandler,
                    ),
            )
            repository.lagre(henvendelse)

            repository.hent(henvendelse.henvendelseId).also { dbHenvendelse ->
                dbHenvendelse.tilstand() shouldBe KlarTilBehandling
                dbHenvendelse shouldBe henvendelse
            }

            henvendelse.tildel(
                tildelHendelse =
                    TildelHendelse(
                        utførtAv = saksbehandler,
                        ansvarligIdent = saksbehandler.navIdent,
                    ),
            )
            repository.lagre(henvendelse)

            repository.hent(henvendelse.henvendelseId).also { dbHenvendelse ->
                dbHenvendelse.tilstand() shouldBe UnderBehandling
                dbHenvendelse shouldBe henvendelse
            }

            henvendelse.ferdigstill(
                henvendelseFerdigstiltHendelse =
                    HenvendelseFerdigstiltHendelse(
                        henvendelseId = henvendelse.henvendelseId,
                        aksjon = "Avslutt",
                        behandlingId = null,
                        utførtAv = saksbehandler,
                    ),
            )
            repository.lagre(henvendelse)

            repository.hent(henvendelse.henvendelseId).also { dbHenvendelse ->
                dbHenvendelse.tilstand() shouldBe Ferdigbehandlet
                dbHenvendelse shouldBe henvendelse
            }
        }
    }
}
