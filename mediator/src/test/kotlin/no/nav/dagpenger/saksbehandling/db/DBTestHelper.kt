package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.UtløstAvType.SØKNAD
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonRepository
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource

class DBTestHelper private constructor(private val ds: DataSource) :
    SakRepository by PostgresSakRepository(ds),
    OppgaveRepository by PostgresOppgaveRepository(ds),
    PersonRepository by PostgresPersonRepository(ds) {
        companion object {
            val sakId = UUIDv7.ny()
            val søknadId = UUIDv7.ny()
            val opprettetNå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
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

            fun withSaker(
                person: Person = testPerson,
                saker: List<Sak>,
                block: DBTestHelper.(DataSource) -> Unit,
            ) {
                withPerson(person) { ds ->
                    this.lagre(
                        SakHistorikk(
                            person = person,
                            saker = saker.toMutableSet(),
                        ),
                    )
                    block(ds)
                }
            }

            fun withSak(
                person: Person = testPerson,
                sak: Sak =
                    Sak(
                        sakId = sakId,
                        søknadId = søknadId,
                        opprettet = opprettetNå,
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
                        utløstAv = SØKNAD,
                        opprettet = opprettetNå,
                        hendelse = TomHendelse,
                    ),
                sak: Sak =
                    Sak(
                        sakId = sakId,
                        søknadId = søknadId,
                        opprettet = opprettetNå,
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
                sak: Sak =
                    Sak(
                        sakId = sakId,
                        søknadId = søknadId,
                        opprettet = opprettetNå,
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
                        behandlingId = oppgave.behandling.behandlingId,
                        utløstAv = oppgave.behandling.utløstAv,
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

        fun lagHeleSulamitten(
            person: Person,
            sak: Sak,
            behandling: Behandling,
            oppgave: Oppgave,
        ) {
            sak.leggTilBehandling(behandling)
            this.lagre(person)
            this.lagre(
                SakHistorikk(
                    person = person,
                    saker = mutableSetOf(sak),
                ),
            )
            this.lagre(oppgave)
        }

        fun leggTilOppgave(
            id: UUID = UUIDv7.ny(),
            tilstand: Oppgave.Tilstand = Oppgave.KlarTilBehandling,
            emneknagger: Set<String> = emptySet(),
            person: Person = testPerson,
            opprettet: LocalDateTime = opprettetNå,
            type: UtløstAvType = SØKNAD,
            tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
            saksbehandlerIdent: String? = null,
        ): Oppgave {
            this.lagre(person)

            val behandling =
                Behandling(
                    behandlingId = UUIDv7.ny(),
                    utløstAv = type,
                    opprettet = opprettet,
                    hendelse = TomHendelse,
                )
            val sak =
                Sak(
                    sakId = UUIDv7.ny(),
                    søknadId = søknadId,
                    opprettet = opprettet,
                    behandlinger = mutableSetOf(behandling),
                )
            val sakHistorikk =
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
                behandlerIdent = saksbehandlerIdent,
                behandling = behandling,
                person = person,
                meldingOmVedtak =
                    Oppgave.MeldingOmVedtak(
                        kilde = DP_SAK,
                        kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                    ),
            ).also { this.lagre(it) }
        }

        fun leggTilOppgave(
            oppgaveId: UUID,
            behandlingId: UUID,
            utløstAvType: UtløstAvType = SØKNAD,
            person: Person = testPerson,
        ) {
            val behandling =
                Behandling(
                    behandlingId = behandlingId,
                    utløstAv = utløstAvType,
                    opprettet = opprettetNå,
                    hendelse = TomHendelse,
                )
            Oppgave(
                oppgaveId = oppgaveId,
                emneknagger = setOf(),
                opprettet = behandling.opprettet,
                tilstand = Oppgave.KlarTilBehandling,
                behandling = behandling,
                person = person,
                meldingOmVedtak =
                    Oppgave.MeldingOmVedtak(
                        kilde = DP_SAK,
                        kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                    ),
            ).also { lagre(it) }
        }
    }
