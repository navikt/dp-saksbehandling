package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.NySak
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresRepository
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class DBTestHelper private constructor(private val ds: DataSource) :
    SakRepository by PostgresRepository(ds),
    OppgaveRepository by PostgresOppgaveRepository(ds),
    PersonRepository by PostgresPersonRepository(ds) {
        companion object {
            val sakId = UUIDv7.ny()
            val søknadId = UUIDv7.ny()

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

            fun withBehandling(
                person: Person = testPerson,
                behandling: Behandling =
                    Behandling(
                        behandlingId = UUIDv7.ny(),
                        type = BehandlingType.RETT_TIL_DAGPENGER,
                        opprettet = LocalDateTime.now(),
                        hendelse = TomHendelse,
                    ),
                sak: NySak =
                    NySak(
                        sakId = sakId,
                        søknadId = søknadId,
                        opprettet = LocalDateTime.now(),
                        behandlinger = mutableSetOf(behandling),
                    ),
                sakHistorikk: SakHistorikk =
                    SakHistorikk(
                        person = person,
                        saker = mutableSetOf(sak),
                    ),
                block: DBTestHelper.(DataSource) -> Unit,
            ) {
                withPerson(person) { ds ->
                    this.lagre(sakHistorikk)
                    block(ds)
                }
            }

            fun withBehandlinger(
                person: Person = testPerson,
                behandlinger: List<Behandling> = emptyList(),
                sak: NySak =
                    NySak(
                        sakId = sakId,
                        søknadId = søknadId,
                        opprettet = LocalDateTime.now(),
                        behandlinger = behandlinger.toMutableSet(),
                    ),
                sakHistorikk: SakHistorikk =
                    SakHistorikk(
                        person = person,
                        saker = mutableSetOf(sak),
                    ),
                block: DBTestHelper.(DataSource) -> Unit,
            ) {
                withPerson(person) { ds ->
                    this.lagre(sakHistorikk)
                    block(ds)
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
