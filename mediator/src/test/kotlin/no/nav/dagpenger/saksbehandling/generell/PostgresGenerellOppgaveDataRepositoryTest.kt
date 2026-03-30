package no.nav.dagpenger.saksbehandling.generell

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.GenerellOppgaveData
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.generell.PostgresGenerellOppgaveDataRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PostgresGenerellOppgaveDataRepositoryTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `Skal lagre og hente generell oppgave-data`() {
        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            val sakRepository = PostgresSakRepository(ds)
            val oppgaveRepository = PostgresOppgaveRepository(ds)
            val generellOppgaveDataRepository = PostgresGenerellOppgaveDataRepository(ds)

            val person =
                Person(
                    id = UUIDv7.ny(),
                    ident = "12345678901",
                    skjermesSomEgneAnsatte = false,
                    adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                )
            personRepository.lagre(person)

            val behandling =
                Behandling(
                    behandlingId = UUIDv7.ny(),
                    opprettet = LocalDateTime.now(),
                    hendelse = TomHendelse,
                    utløstAv = UtløstAvType.GENERELL,
                )
            sakRepository.lagreBehandling(
                personId = person.id,
                sakId = null,
                behandling = behandling,
            )

            val oppgave =
                Oppgave(
                    emneknagger = setOf("MeldekortKorrigering"),
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

            val strukturertData =
                objectMapper.readTree(
                    """{"meldeperiodeFra": "2026-03-01", "meldekortId": "abc-123"}""",
                )

            val data =
                GenerellOppgaveData(
                    oppgaveId = oppgave.oppgaveId,
                    oppgaveType = "MeldekortKorrigering",
                    tittel = "Meldekort trenger korrigering",
                    beskrivelse = "Meldekortet for perioden 01.03-14.03 må gjennomgås",
                    strukturertData = strukturertData,
                )

            generellOppgaveDataRepository.lagre(data)

            val hentet = generellOppgaveDataRepository.hent(oppgave.oppgaveId)
            hentet shouldNotBe null
            hentet!!.oppgaveType shouldBe "MeldekortKorrigering"
            hentet.tittel shouldBe "Meldekort trenger korrigering"
            hentet.beskrivelse shouldBe "Meldekortet for perioden 01.03-14.03 må gjennomgås"
            hentet.strukturertData shouldNotBe null
            hentet.strukturertData!!["meldekortId"].asText() shouldBe "abc-123"
        }
    }

    @Test
    fun `Skal returnere null for ukjent oppgaveId`() {
        withMigratedDb { ds ->
            val generellOppgaveDataRepository = PostgresGenerellOppgaveDataRepository(ds)
            generellOppgaveDataRepository.hent(UUIDv7.ny()) shouldBe null
        }
    }

    @Test
    fun `Skal lagre uten valgfrie felter`() {
        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            val sakRepository = PostgresSakRepository(ds)
            val oppgaveRepository = PostgresOppgaveRepository(ds)
            val generellOppgaveDataRepository = PostgresGenerellOppgaveDataRepository(ds)

            val person =
                Person(
                    id = UUIDv7.ny(),
                    ident = "12345678902",
                    skjermesSomEgneAnsatte = false,
                    adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                )
            personRepository.lagre(person)

            val behandling =
                Behandling(
                    behandlingId = UUIDv7.ny(),
                    opprettet = LocalDateTime.now(),
                    hendelse = TomHendelse,
                    utløstAv = UtløstAvType.GENERELL,
                )
            sakRepository.lagreBehandling(
                personId = person.id,
                sakId = null,
                behandling = behandling,
            )

            val oppgave =
                Oppgave(
                    emneknagger = setOf("EnkelOppgave"),
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

            val data =
                GenerellOppgaveData(
                    oppgaveId = oppgave.oppgaveId,
                    oppgaveType = "EnkelOppgave",
                    tittel = "En enkel oppgave",
                )
            generellOppgaveDataRepository.lagre(data)

            val hentet = generellOppgaveDataRepository.hent(oppgave.oppgaveId)
            hentet shouldNotBe null
            hentet!!.beskrivelse shouldBe null
            hentet.strukturertData shouldBe null
        }
    }
}
