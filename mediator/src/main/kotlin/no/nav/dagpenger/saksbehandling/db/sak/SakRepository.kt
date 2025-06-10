package no.nav.dagpenger.saksbehandling.db.sak

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.NyBehandling
import no.nav.dagpenger.saksbehandling.NyPerson
import no.nav.dagpenger.saksbehandling.NySak
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

    private fun TransactionalSession.lagreSaker(
        personId: UUID,
        saker: List<NySak>,
    ) {
        saker.forEach { sak -> this.lagreSak(personId, sak) }
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
                        "id" to sak.id,
                        "person_id" to personId,
                        "soknad_id" to sak.søknadId,
                        "opprettet" to sak.opprettet,
                    ),
            ).asUpdate,
        )
        this.lagreBehandlinger(sak.id, sak.behandlinger())
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

    override fun hent(ident: String): NyPerson {
        TODO("Not yet implemented")
    }

    override fun finn(ident: String): NyPerson? {
        TODO("Not yet implemented")
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
        return saker.single { it.id == sakId }
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
