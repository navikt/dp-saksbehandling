package no.nav.dagpenger.saksbehandling.db.innsending

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.testPerson
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.innsending.Innsending
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PostgresInnsendingRepositoryTest {
    @Test
    fun `Skal lagre, endre og hente innsending fra database`() {
        DBTestHelper.withPerson { ds ->
            val nå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)

            val innsending =
                Innsending.rehydrer(
                    innsendingId = UUIDv7.ny(),
                    person = testPerson,
                    journalpostId = "jp123",
                    mottatt = nå,
                    skjemaKode = "skjemaKode",
                    kategori = Kategori.NY_SØKNAD,
                    søknadId = UUIDv7.ny(),
                    tilstand = "BEHANDLES",
                    vurdering = "Dette er en vurdering",
                    innsendingResultat =
                        Innsending.InnsendingResultat.RettTilDagpenger(
                            UUIDv7.ny(),
                        ),
                )
            val repository = PostgresInnsendingRepository(ds)
            repository.lagre(innsending = innsending)

            repository.hent(innsending.innsendingId).also { dbInnsending ->
                dbInnsending shouldBe innsending
            }

            val endretInnsending =
                Innsending.rehydrer(
                    innsendingId = innsending.innsendingId,
                    person = testPerson.copy(ident = "22345678901"),
                    journalpostId = "nyJp",
                    mottatt = nå.plusMonths(1),
                    skjemaKode = "nySkjema",
                    kategori = Kategori.ETTERSENDING,
                    søknadId = UUIDv7.ny(),
                    tilstand = "FERDIGSTILT",
                    vurdering = "Endret vurdering",
                    innsendingResultat = Innsending.InnsendingResultat.Klage(UUIDv7.ny()),
                )

            repository.lagre(innsending = endretInnsending)
            repository.hent(innsending.innsendingId).also {
                it.person shouldBe testPerson
                it.journalpostId shouldBe innsending.journalpostId
                it.mottatt shouldBe innsending.mottatt
                it.skjemaKode shouldBe innsending.skjemaKode
                it.kategori shouldBe innsending.kategori
                it.søknadId shouldBe innsending.søknadId

                it.tilstand() shouldBe endretInnsending.tilstand()
                it.vurdering() shouldBe endretInnsending.vurdering()
                it.innsendingResultat() shouldBe endretInnsending.innsendingResultat()
            }
        }
    }
}
