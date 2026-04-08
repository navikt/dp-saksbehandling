package no.nav.dagpenger.saksbehandling.db.oppgave

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.GenerellOppgave
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PostgresOppgaveRepositoryGenerellOppgaveTest {
    @Test
    fun `Skal lagre og hente generell oppgave-data`() {
        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            val sakRepository = PostgresSakRepository(ds)
            val oppgaveRepository = PostgresOppgaveRepository(ds)

            val person = lagPerson(personRepository)
            val behandling = lagBehandlingForGenerellOppgave(sakRepository, person)
            val oppgave = lagOppgave(oppgaveRepository, behandling, person)

            val strukturertData = objectMapper.readTree("""{"periode": "2024-01", "beløp": 1234}""")
            val data =
                GenerellOppgave(
                    oppgaveId = oppgave.oppgaveId,
                    emneknagg = "MeldekortKorrigering",
                    tittel = "Korrigert meldekort",
                    beskrivelse = "Meldekortet må gjennomgås",
                    strukturertData = strukturertData,
                )

            oppgaveRepository.lagreGenerellOppgave(data)

            val hentet = oppgaveRepository.hentGenerellOppgave(oppgave.oppgaveId)
            hentet shouldNotBe null
            hentet!!.oppgaveId shouldBe oppgave.oppgaveId
            hentet.emneknagg shouldBe "MeldekortKorrigering"
            hentet.tittel shouldBe "Korrigert meldekort"
            hentet.beskrivelse shouldBe "Meldekortet må gjennomgås"
            hentet.strukturertData shouldNotBe null
            hentet.strukturertData!!["periode"].asText() shouldBe "2024-01"
            hentet.strukturertData!!["beløp"].asInt() shouldBe 1234
        }
    }

    @Test
    fun `Skal lagre generell oppgave-data uten valgfrie felter`() {
        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            val sakRepository = PostgresSakRepository(ds)
            val oppgaveRepository = PostgresOppgaveRepository(ds)

            val person = lagPerson(personRepository)
            val behandling = lagBehandlingForGenerellOppgave(sakRepository, person)
            val oppgave = lagOppgave(oppgaveRepository, behandling, person)

            val data =
                GenerellOppgave(
                    oppgaveId = oppgave.oppgaveId,
                    emneknagg = "EnkelOppgave",
                    tittel = "En enkel oppgave",
                    beskrivelse = null,
                    strukturertData = null,
                )

            oppgaveRepository.lagreGenerellOppgave(data)

            val hentet = oppgaveRepository.hentGenerellOppgave(oppgave.oppgaveId)
            hentet shouldNotBe null
            hentet!!.beskrivelse shouldBe null
            hentet.strukturertData shouldBe null
        }
    }

    @Test
    fun `Skal returnere null for ukjent oppgaveId`() {
        withMigratedDb { ds ->
            val oppgaveRepository = PostgresOppgaveRepository(ds)
            val hentet = oppgaveRepository.hentGenerellOppgave(UUIDv7.ny())
            hentet shouldBe null
        }
    }

    private fun lagPerson(personRepository: PostgresPersonRepository): Person {
        val person =
            Person(
                id = UUIDv7.ny(),
                ident = "12345678901",
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
            )
        personRepository.lagre(person)
        return person
    }

    private fun lagBehandlingForGenerellOppgave(
        sakRepository: PostgresSakRepository,
        person: Person,
    ): Behandling {
        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                opprettet = LocalDateTime.now(),
                hendelse =
                    OpprettGenerellOppgaveHendelse(
                        ident = person.ident,
                        emneknagg = "Test",
                        tittel = "Test",
                    ),
                utløstAv = UtløstAvType.GENERELL,
            )
        sakRepository.lagreBehandling(
            personId = person.id,
            sakId = null,
            behandling = behandling,
        )
        return behandling
    }

    private fun lagOppgave(
        oppgaveRepository: PostgresOppgaveRepository,
        behandling: Behandling,
        person: Person,
    ): Oppgave {
        val oppgave =
            Oppgave(
                emneknagger = setOf("Test"),
                opprettet = LocalDateTime.now(),
                behandling = behandling,
                person = person,
                meldingOmVedtak =
                    Oppgave.MeldingOmVedtak(
                        kilde = Oppgave.MeldingOmVedtakKilde.INGEN,
                        kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                    ),
            )
        oppgaveRepository.lagre(oppgave)
        return oppgave
    }
}
