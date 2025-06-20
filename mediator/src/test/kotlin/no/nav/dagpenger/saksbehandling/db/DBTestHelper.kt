package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.NySak
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.Tilstandslogg
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

            fun withMigratedDb(block: DBTestHelper.(DataSource) -> Unit) {
                Postgres.withMigratedDb { ds ->
                    DBTestHelper(ds).apply {
                        block(ds)
                    }
                }
            }

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

            fun withSak(
                person: Person = testPerson,
                sak: NySak =
                    NySak(
                        sakId = sakId,
                        søknadId = søknadId,
                        opprettet = LocalDateTime.now(),
                    ),
                block: DBTestHelper.(DataSource) -> Unit,
            ) {
                withPerson(person) { ds ->
                    this.lagre(
                        SakHistorikk(
                            person = person,
                            saker = mutableSetOf(sak),
                        ),
                    )
                    block(ds)
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
                block: DBTestHelper.(DataSource) -> Unit,
            ) = withSak(
                person = person,
                sak = sak,
                block = block,
            )

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
                block: DBTestHelper.(DataSource) -> Unit,
            ) = withSak(
                person = person,
                sak = sak,
                block = block,
            )

            fun withOppgave(
                oppgave: Oppgave,
                block: DBTestHelper.(DataSource) -> Unit,
            ) {
                val behandling =
                    Behandling(
                        behandlingId = oppgave.behandlingId,
                        type = oppgave.behandlingType,
                        opprettet = oppgave.opprettet,
                        hendelse = TomHendelse,
                    )
                withBehandling(
                    person = oppgave.person,
                    behandling = behandling,
                ) { ds ->
                    this.lagre(oppgave)
                    block(ds)
                }
            }
        }

        fun leggTilOppgave(
            id: UUID = UUIDv7.ny(),
            tilstand: Oppgave.Tilstand = Oppgave.KlarTilBehandling,
            emneknagger: Set<String> = emptySet(),
            person: Person = testPerson,
            opprettet: LocalDateTime = LocalDateTime.now(),
            type: BehandlingType = BehandlingType.RETT_TIL_DAGPENGER,
            tilstandslogg: Tilstandslogg = Tilstandslogg(),
            saksBehandlerIdent: String? = null,
        ): Oppgave {
            this.lagre(person)

            val behandling =
                Behandling(
                    behandlingId = UUIDv7.ny(),
                    type = type,
                    opprettet = opprettet,
                    hendelse = TomHendelse,
                )
            val sak =
                NySak(
                    sakId = UUIDv7.ny(),
                    søknadId = søknadId,
                    opprettet = opprettet,
                    behandlinger = mutableSetOf(behandling),
                )

            val sakHistorikk: SakHistorikk =
                SakHistorikk(
                    person = person,
                    saker = mutableSetOf(sak),
                )
            this.lagre(sakHistorikk)

            return Oppgave(
                oppgaveId = id,
                opprettet = opprettet,
                tilstand = tilstand,
                emneknagger = emneknagger,
                tilstandslogg = tilstandslogg,
                behandlerIdent = saksBehandlerIdent,
                behandlingId = behandling.behandlingId,
                behandlingType = type,
                person = person,
            ).also { this.lagre(it) }
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
