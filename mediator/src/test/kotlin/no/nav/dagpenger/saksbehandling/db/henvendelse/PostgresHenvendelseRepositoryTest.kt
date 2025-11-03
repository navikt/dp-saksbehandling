package no.nav.dagpenger.saksbehandling.db.henvendelse

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.testPerson
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PostgresHenvendelseRepositoryTest {
    @Test
    fun `Skal lagres og hente henvendelse fra database`() {
        DBTestHelper.withPerson { ds ->
            val repository = PostgresHenvendelseRepository(ds)
            val henvendelseMottattHendelse =
                HenvendelseMottattHendelse(
                    ident = testPerson.ident,
                    journalpostId = "jp12",
                    registrertTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
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
                        utførtAv =
                            Saksbehandler(
                                navIdent = "Z12345",
                                grupper = emptySet(),
                                tilganger = emptySet(),
                            ),
                        ansvarligIdent = "ABS123",
                    ),
            )
            repository.lagre(henvendelse)

            repository.hent(henvendelse.henvendelseId) shouldBe henvendelse
        }
    }
}
