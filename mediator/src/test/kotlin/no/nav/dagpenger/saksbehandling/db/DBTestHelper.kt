package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresRepository
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class DBTestHelper private constructor(private val ds: DataSource) :
    SakRepository by PostgresRepository(ds),
    OppgaveRepository by PostgresOppgaveRepository(ds),
    PersonRepository by PostgresPersonRepository(ds) {
        companion object {
            val testPerson =
                Person(
                    id = UUIDv7.ny(),
                    ident = "12345678901",
                    skjermesSomEgneAnsatte = false,
                    adressebeskyttelseGradering = UGRADERT,
                )

            fun withPerson(
                person: Person = testPerson,
                block: DBTestHelper.(DataSource) -> Unit,
            ) {
                Postgres.withMigratedDb { ds ->
                    DBTestHelper(ds).apply {
                        this.lagre(person)
                        block(ds)
                    }
                }
            }
        }

        fun leggTilOppgave(
            oppgaveId: UUID,
            behandlingId: UUID,
            behandlingType: BehandlingType = BehandlingType.RETT_TIL_DAGPENGER,
            person: Person = testPerson,
        ) {
            Oppgave(
                oppgaveId = oppgaveId,
                emneknagger = setOf(),
                opprettet = LocalDateTime.now(),
                tilstand = Oppgave.KlarTilBehandling,
                behandlingId = behandlingId,
                behandlingType = behandlingType,
                person = person,
            ).also { lagre(it) }
        }
    }
