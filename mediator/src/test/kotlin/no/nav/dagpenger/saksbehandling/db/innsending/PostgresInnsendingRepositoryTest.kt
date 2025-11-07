package no.nav.dagpenger.saksbehandling.db.innsending

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.testPerson
import no.nav.dagpenger.saksbehandling.hendelser.FjernAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.innsending.Innsending
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.Ferdigbehandlet
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.UnderBehandling
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PostgresInnsendingRepositoryTest {
    @Test
    fun `Skal lagre endre og hente innsending fra database`() {
        DBTestHelper.withPerson { ds ->
            val nå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
            val saksbehandler =
                Saksbehandler(
                    navIdent = "Z12345",
                    grupper = emptySet(),
                    tilganger = emptySet(),
                )
            val repository = PostgresInnsendingRepository(ds)
            val innsendingMottattHendelse =
                InnsendingMottattHendelse(
                    ident = testPerson.ident,
                    journalpostId = "jp12",
                    registrertTidspunkt = nå,
                    søknadId = null,
                    skjemaKode = "skjemaKode",
                    kategori = Kategori.GENERELL,
                )
            val innsending =
                Innsending.opprett(
                    hendelse = innsendingMottattHendelse,
                    personProvider = { _ -> testPerson },
                )

            repository.lagre(innsending = innsending)

            repository.hent(innsending.innsendingId).also { dbInnsending ->
                dbInnsending.tilstand() shouldBe KlarTilBehandling
                dbInnsending shouldBe innsending
            }

            innsending.tildel(
                tildelHendelse =
                    TildelHendelse(
                        utførtAv = saksbehandler,
                        ansvarligIdent = saksbehandler.navIdent,
                        innsendingId = UUIDv7.ny(),
                    ),
            )
            repository.lagre(innsending)

            repository.hent(innsending.innsendingId).also { dbInnsending ->
                dbInnsending.tilstand() shouldBe UnderBehandling
                dbInnsending shouldBe innsending
            }

            innsending.leggTilbake(
                fjernAnsvarHendelse =
                    FjernAnsvarHendelse(
                        utførtAv = saksbehandler,
                    ),
            )
            repository.lagre(innsending)

            repository.hent(innsending.innsendingId).also { dbInnsending ->
                dbInnsending.tilstand() shouldBe KlarTilBehandling
                dbInnsending shouldBe innsending
            }

            innsending.tildel(
                tildelHendelse =
                    TildelHendelse(
                        utførtAv = saksbehandler,
                        ansvarligIdent = saksbehandler.navIdent,
                        innsendingId = UUIDv7.ny(),
                    ),
            )
            repository.lagre(innsending)

            repository.hent(innsending.innsendingId).also { dbInnsending ->
                dbInnsending.tilstand() shouldBe UnderBehandling
                dbInnsending shouldBe innsending
            }

            innsending.ferdigstill(
                innsendingFerdigstiltHendelse =
                    InnsendingFerdigstiltHendelse(
                        innsendingId = innsending.innsendingId,
                        aksjon = "Avslutt",
                        behandlingId = null,
                        utførtAv = saksbehandler,
                    ),
            )
            repository.lagre(innsending)

            repository.hent(innsending.innsendingId).also { dbInnsending ->
                dbInnsending.tilstand() shouldBe Ferdigbehandlet
                dbInnsending shouldBe innsending
            }
        }
    }
}
