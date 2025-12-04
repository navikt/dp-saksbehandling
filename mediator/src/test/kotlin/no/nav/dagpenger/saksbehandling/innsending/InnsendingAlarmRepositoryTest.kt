package no.nav.dagpenger.saksbehandling.innsending

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.TestHelper.lagPerson
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import javax.sql.DataSource

class InnsendingAlarmRepositoryTest {
    @Test
    fun `Skal kunne hente innsendinger som ikke er ferdigstilt`() {
        val nå = LocalDateTime.now()
        val tjueFireTimerSiden = nå.minusHours(24)
        val person = lagPerson()

        DBTestHelper.withPerson(person) { ds ->
            val repository = InnsendingAlarmRepository(ds)
            ds.lagreInnsending(
                tilstand = "BEHANDLES",
                tidspunkt = tjueFireTimerSiden,
                journalpostId = "1",
                person = person,
            )
            ds.lagreInnsending(
                tilstand = "FERDIGSTILT",
                tidspunkt = tjueFireTimerSiden,
                journalpostId = "2",
                person = person,
            )
            ds.lagreInnsending(
                tilstand = "FERDIGSTILL_STARTET",
                tidspunkt = tjueFireTimerSiden,
                journalpostId = "3",
                person = person,
            )
            ds.lagreInnsending(
                tilstand = "BEHANDLES",
                tidspunkt = nå,
                journalpostId = "4",
                person = person,
            )
            ds.lagreInnsending(
                tilstand = "FERDIGSTILT",
                tidspunkt = nå,
                journalpostId = "5",
                person = person,
            )
            ds.lagreInnsending(
                tilstand = "FERDIGSTILL_STARTET",
                tidspunkt = nå,
                journalpostId = "6",
                person = person,
            )
            val ventendeInnsendinger = repository.hentInnsendingerSomIkkeErFerdigstilt(23)
            ventendeInnsendinger.size shouldBe 1
            ventendeInnsendinger.first().journalpostId shouldBe "3"
            ventendeInnsendinger.first().tilstand shouldBe "FERDIGSTILL_STARTET"
        }
    }

    private fun DataSource.lagreInnsending(
        tilstand: String,
        tidspunkt: LocalDateTime = LocalDateTime.now(),
        journalpostId: String,
        person: Person = lagPerson(),
    ): Innsending {
        val innsending =
            Innsending.opprett(
                hendelse =
                    InnsendingMottattHendelse(
                        ident = person.ident,
                        journalpostId = journalpostId,
                        registrertTidspunkt = tidspunkt,
                        søknadId = null,
                        skjemaKode = "skjemaKode",
                        kategori = Kategori.GENERELL,
                    ),
            ) { ident -> person }

        sessionOf(this).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO innsending_v1 ( 
                            id, 
                            person_id,
                            journalpost_id, 
                            mottatt,
                            skjema_kode,
                            kategori,
                            tilstand,
                            registrert_tidspunkt, 
                            endret_tidspunkt
                        )
                        VALUES (
                            :id, 
                            :person_id,
                            :journalpost_id, 
                            :mottatt,
                            :skjema_kode,
                            :kategori,
                            :tilstand, 
                            :registrert_tidspunkt, 
                            :endret_tidspunkt
                        )
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "id" to innsending.innsendingId,
                            "person_id" to innsending.person.id,
                            "journalpost_id" to innsending.journalpostId,
                            "mottatt" to innsending.mottatt,
                            "skjema_kode" to innsending.skjemaKode,
                            "kategori" to innsending.kategori.name,
                            "tilstand" to tilstand,
                            "registrert_tidspunkt" to tidspunkt,
                            "endret_tidspunkt" to tidspunkt,
                        ),
                ).asUpdate,
            )
        }
        return innsending
    }
}
