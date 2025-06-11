package no.nav.dagpenger.saksbehandling.db.sak

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.NyBehandling
import no.nav.dagpenger.saksbehandling.NyPerson
import no.nav.dagpenger.saksbehandling.NySak
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import java.util.UUID
import javax.sql.DataSource

interface SakRepository {
    fun lagre(sak: NySak)

    fun hent(sakId: UUID): NySak

    fun finnAlle(): Set<NySak>
}

interface NyPersonRepository {
    fun lagre(person: NyPerson)

    fun hent(ident: String): NyPerson

    fun finn(ident: String): NyPerson?
}

class PostgresRepository(
    private val dataSource: DataSource,
) : NyPersonRepository {
    override fun lagre(person: NyPerson) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagrePerson(person)
                tx.lagreSaker(person.id, person.saker())
            }
        }
    }

    override fun hent(ident: String): NyPerson {
        return finn(ident) ?: throw DataNotFoundException("Kan ikke finne person med ident $ident")
    }

    override fun finn(ident: String): NyPerson? {
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT * 
                        FROM   person_v1
                        WHERE  ident = :ident
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                        ),
                ).map { row ->
                    row.tilPerson()
                }.asSingle,
            )
        }
    }

    private fun TransactionalSession.lagreSaker(
        personId: UUID,
        saker: List<NySak>,
    ) {
        saker.forEach { sak -> this.lagreSak(personId, sak) }
    }

    private fun NyPerson.finnSaker() {
        sessionOf(dataSource).use { session ->
            val saker: List<NySak> =
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            SELECT id, soknad_id, opprettet
                            FROM   sak_v2
                            WHERE  person_id = :person_id
                            """.trimIndent(),
                        paramMap =
                            mapOf(
                                "person_id" to this.id,
                            ),
                    ).map { row ->
                        row.tilSak()
                    }.asList,
                )
            saker.forEach { sak ->
                this.leggTilSak(sak)
            }
        }
    }

    private fun NySak.finnBehandlinger() {
        sessionOf(dataSource).use { session ->
            val behandlinger: List<NyBehandling> =
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            SELECT    beha.id AS behandling_id
                                    , beha.behandling_type
                                    , beha.opprettet
                                    , oppg.id AS oppgave_id
                            FROM      behandling_v2 beha
                            LEFT JOIN oppgave_v1    oppg ON oppg.behandling_id = beha.id
                            WHERE     beha.sak_id = :sak_id
                            """.trimIndent(),
                        paramMap =
                            mapOf(
                                "sak_id" to this.sakId,
                            ),
                    ).map { row ->
                        row.tilBehandling()
                    }.asList,
                )
            behandlinger.forEach { behandling ->
                this.leggTilBehandling(behandling)
            }
        }
    }

    private fun TransactionalSession.lagreSak(
        personId: UUID,
        sak: NySak,
    ) {
        run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO sak_v2
                        (id, person_id, soknad_id, opprettet) 
                    VALUES
                        (:id,:person_id, :soknad_id, :opprettet) 
                    ON CONFLICT (id) DO NOTHING 
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to sak.sakId,
                        "person_id" to personId,
                        "soknad_id" to sak.søknadId,
                        "opprettet" to sak.opprettet,
                    ),
            ).asUpdate,
        )
        this.lagreBehandlinger(sak.sakId, sak.behandlinger())
    }

    private fun TransactionalSession.lagreBehandlinger(
        sakId: UUID,
        behandlinger: List<NyBehandling>,
    ) {
        behandlinger.forEach { behandling -> this.lagreBehandling(sakId, behandling) }
    }

    private fun TransactionalSession.lagreBehandling(
        sakId: UUID,
        behandling: NyBehandling,
    ) {
        run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO behandling_v2
                        (id, behandling_type, sak_id, opprettet) 
                    VALUES
                        (:id, :behandling_type,:sak_id, :opprettet) 
                    ON CONFLICT (id) DO NOTHING 
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to behandling.behandlingId,
                        "behandling_type" to behandling.behandlingType.name,
                        "sak_id" to sakId,
                        "opprettet" to behandling.opprettet,
                    ),
            ).asUpdate,
        )
    }

    private fun Row.tilPerson(): NyPerson {
        val person =
            NyPerson(
                id = this.uuid("id"),
                ident = this.string("ident"),
                skjermesSomEgneAnsatte = this.boolean("skjermes_som_egne_ansatte"),
                adressebeskyttelseGradering = this.adresseBeskyttelseGradering(),
            ).also { person ->
                person.finnSaker()
            }

        return person
    }

    private fun Row.tilSak(): NySak {
        val sak =
            NySak(
                sakId = this.uuid("id"),
                søknadId = this.uuid("soknad_id"),
                opprettet = this.localDateTime("opprettet"),
            ).also { sak ->
                sak.finnBehandlinger()
            }

        return sak
    }

    private fun Row.tilBehandling(): NyBehandling {
        val behandling =
            NyBehandling(
                behandlingId = this.uuid("behandling_id"),
                behandlingType = BehandlingType.valueOf(this.string("behandling_type")),
                opprettet = this.localDateTime("opprettet"),
                oppgaveId = this.uuidOrNull("oppgave_id"),
            )

        return behandling
    }

    private fun Row.adresseBeskyttelseGradering(): AdressebeskyttelseGradering {
        return AdressebeskyttelseGradering.valueOf(this.string("adressebeskyttelse_gradering"))
    }

    private fun TransactionalSession.lagrePerson(person: NyPerson) {
        run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO person_v1
                        (id, ident, skjermes_som_egne_ansatte, adressebeskyttelse_gradering) 
                    VALUES
                        (:id, :ident, :skjermes_som_egne_ansatte, :adressebeskyttelse_gradering) 
                    ON CONFLICT (id) DO UPDATE SET skjermes_som_egne_ansatte = :skjermes_som_egne_ansatte , adressebeskyttelse_gradering = :adressebeskyttelse_gradering             
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to person.id,
                        "ident" to person.ident,
                        "skjermes_som_egne_ansatte" to person.skjermesSomEgneAnsatte,
                        "adressebeskyttelse_gradering" to person.adressebeskyttelseGradering.name,
                    ),
            ).asUpdate,
        )
    }
}

object InmemoryRepository : SakRepository, NyPersonRepository {
    private val saker: MutableSet<NySak> = mutableSetOf()
    private val personer: MutableSet<NyPerson> = mutableSetOf()

    fun reset() {
        personer.clear()
        saker.clear()
    }

    override fun lagre(sak: NySak) {
        saker.add(sak)
    }

    override fun hent(sakId: UUID): NySak {
        return saker.single { it.sakId == sakId }
    }

    override fun lagre(person: NyPerson) {
        personer.add(person)
    }

    override fun hent(ident: String): NyPerson {
        return personer.single { it.ident == ident }
    }

    override fun finn(ident: String): NyPerson? {
        return personer.find { it.ident == ident }
    }

    override fun finnAlle(): Set<NySak> {
        return saker
    }
}
