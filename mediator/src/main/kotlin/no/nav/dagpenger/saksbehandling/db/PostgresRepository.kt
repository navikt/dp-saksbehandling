package no.nav.dagpenger.saksbehandling.db

import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.db.DBUtils.norskZonedDateTime
import java.util.UUID
import javax.sql.DataSource

class DataNotFoundException(message: String) : RuntimeException(message)
class PostgresRepository(private val dataSource: DataSource) : Repository {
    override fun lagre(person: Person) {
        sessionOf(dataSource).use { session ->
            session.lagre(person)
        }
    }

    private fun Session.lagre(person: Person) {
        run(
            queryOf(
                //language=PostgreSQL
                statement = """
                     INSERT INTO person_v1 (id, ident) 
                     VALUES (:id, :ident) 
                     ON CONFLICT (id) DO UPDATE SET ident = :ident
                """.trimIndent(),
                paramMap = mapOf(
                    "id" to person.id,
                    "ident" to person.ident,
                ),
            ).asUpdate,
        )
    }

    override fun hentPerson(ident: String): Person {
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                    SELECT * FROM person_v1 WHERE ident = :ident
                    """.trimIndent(),
                    paramMap = mapOf(
                        "ident" to ident,
                    ),
                ).map { row ->
                    Person(
                        id = row.uuid("id"),
                        ident = row.string("ident"),
                    )
                }.asSingle,
            ) ?: throw DataNotFoundException("Fant ikke person med ident $ident")
        }
    }

    override fun lagre(behandling: Behandling) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagre(behandling.person)
                tx.lagre(behandling)
                tx.lagre(behandling.oppgaver)
            }
        }
    }

    override fun hentBehandling(behandlingId: UUID): Behandling {
        return sessionOf(dataSource).run(
            queryOf(
                //language=PostgreSQL
                statement = """
                    SELECT beha.id behandling_id, beha.opprettet, pers.id person_id, pers.ident
                    FROM behandling_v1 beha
                    JOIN person_v1 pers ON pers.id = beha.person_id
                    WHERE beha.id = :behandling_id
                """.trimIndent(),
                paramMap = mapOf("behandling_id" to behandlingId),
            ).map { row ->
                val ident = row.string("ident")
                Behandling.rehydrer(
                    behandlingId = behandlingId,
                    person = hentPerson(ident),
                    opprettet = row.norskZonedDateTime("opprettet"),
                    oppgaver = hentOppgaverFraBehandling(behandlingId, ident),
                )
            }.asSingle,
        ) ?: throw DataNotFoundException("Kunne ikke finne behandling med id: $behandlingId")
    }

    private fun hentOppgaverFraBehandling(behandlingId: UUID, ident: String): List<Oppgave> {
        return sessionOf(dataSource).run(
            queryOf(
                //language=PostgreSQL
                statement = """
                    SELECT id, tilstand, opprettet
                    FROM oppgave_v1
                    WHERE behandling_id = :behandling_id
                """.trimIndent(),
                paramMap = mapOf("behandling_id" to behandlingId),
            ).map { row ->
                val oppgaveId = row.uuid("id")
                Oppgave.rehydrer(
                    oppgaveId = oppgaveId,
                    ident = ident,
                    behandlingId = behandlingId,
                    opprettet = row.norskZonedDateTime("opprettet"),
                    emneknagger = hentEmneknaggerForOppgave(oppgaveId),
                    tilstand = row.string("tilstand").let { Oppgave.Tilstand.Type.valueOf(it) },
                )
            }.asList,
        )
    }

    private fun hentEmneknaggerForOppgave(oppgaveId: UUID): Set<String> {
        return sessionOf(dataSource).run(
            queryOf(
                //language=PostgreSQL
                statement = """
                    SELECT emneknagg
                    FROM emneknagg_v1
                    WHERE oppgave_id = :oppgave_id
                """.trimIndent(),
                paramMap = mapOf("oppgave_id" to oppgaveId),
            ).map { row ->
                row.string("emneknagg")
            }.asList,
        ).toSet()
    }

    private fun Session.lagre(behandling: Behandling) {
        run(
            queryOf(
                //language=PostgreSQL
                statement = """
                     INSERT INTO behandling_v1 (id, person_id, opprettet) VALUES (:id, :person_id, :opprettet) 
                """.trimIndent(),
                paramMap = mapOf(
                    "id" to behandling.behandlingId,
                    "person_id" to behandling.person.id,
                    "opprettet" to behandling.opprettet,
                ),
            ).asUpdate,
        )
    }

    private fun TransactionalSession.lagre(oppgaver: List<Oppgave>) {
        oppgaver.forEach { oppgave ->
            lagre(oppgave)
        }
    }

    private fun TransactionalSession.lagre(oppgave: Oppgave) {
        run(
            queryOf(
                //language=PostgreSQL
                statement = """
                     INSERT INTO oppgave_v1 (id, behandling_id, tilstand, opprettet) VALUES (:id, :behandling_id, :tilstand, :opprettet) 
                """.trimIndent(),
                paramMap = mapOf(
                    "id" to oppgave.oppgaveId,
                    "behandling_id" to oppgave.behandlingId,
                    "tilstand" to oppgave.tilstand.name,
                    "opprettet" to oppgave.opprettet,
                ),
            ).asUpdate,
        )
        lagre(oppgave.oppgaveId, oppgave.emneknagger)
    }

    private fun TransactionalSession.lagre(oppgaveId: UUID, emneknagger: Set<String>) {
        emneknagger.forEach { emneknagg ->
            run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        INSERT INTO emneknagg_v1(oppgave_id, emneknagg) 
                        VALUES (:oppgave_id, :emneknagg)
                    """.trimIndent(),
                    paramMap = mapOf("oppgave_id" to oppgaveId, "emneknagg" to emneknagg),
                ).asUpdate,
            )
        }
    }

    override fun hentBehandlingFra(oppgaveId: UUID): Behandling {
        return sessionOf(dataSource).run(
            queryOf(
                //language=PostgreSQL
                statement = """
                    SELECT beha.id behandling_id, beha.opprettet, pers.id person_id, pers.ident
                    FROM behandling_v1 beha
                    JOIN person_v1 pers ON pers.id = beha.person_id
                    JOIN oppgave_v1 oppg ON oppg.behandling_id = beha.id
                    WHERE oppg.id = :oppgave_id
                """.trimIndent(),
                paramMap = mapOf("oppgave_id" to oppgaveId),
            ).map { row ->
                val ident = row.string("ident")
                Behandling.rehydrer(
                    behandlingId = row.uuid("behandling_id"),
                    person = hentPerson(ident),
                    opprettet = row.norskZonedDateTime("opprettet"),
                    oppgaver = hentOppgaverFraBehandling(row.uuid("behandling_id"), ident),
                )
            }.asSingle,
        ) ?: throw DataNotFoundException("Kunne ikke finne behandling med for oppgave-id: $oppgaveId")
    }

    override fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave> {
        return sessionOf(dataSource).run(
            queryOf(
                //language=PostgreSQL
                statement = """
                    SELECT pers.ident, oppg.tilstand, oppg.opprettet, oppg.behandling_id, oppg.id
                    FROM oppgave_v1 oppg
                    JOIN behandling_v1 beha ON beha.id = oppg.behandling_id
                    JOIN person_v1 pers ON pers.id = beha.person_id
                    WHERE oppg.tilstand = :tilstand
                """.trimIndent(),
                paramMap = mapOf(
                    "tilstand" to tilstand.name,
                ),
            ).map {
                Oppgave.rehydrer(
                    oppgaveId = it.uuid("id"),
                    ident = it.string("ident"),
                    behandlingId = it.uuid("behandling_id"),
                    opprettet = it.norskZonedDateTime("opprettet"),
                    emneknagger = hentEmneknaggerForOppgave(it.uuid("id")),
                    tilstand = it.string("tilstand").let { Oppgave.Tilstand.Type.valueOf(it) },
                )
            }.asList,
        )
    }

    override fun hentOppgave(oppgaveId: UUID): Oppgave =
        sessionOf(dataSource).run(
            queryOf(
                //language=PostgreSQL
                statement = """
                    SELECT pers.ident, oppg.tilstand, oppg.opprettet, oppg.behandling_id
                    FROM oppgave_v1 oppg
                    JOIN behandling_v1 beha ON beha.id = oppg.behandling_id
                    JOIN person_v1 pers ON pers.id = beha.person_id
                    WHERE oppg.id = :oppgave_id
                """.trimIndent(),
                paramMap = mapOf("oppgave_id" to oppgaveId),
            ).map { row ->

                Oppgave.rehydrer(
                    oppgaveId = oppgaveId,
                    ident = row.string("ident"),
                    behandlingId = row.uuid("behandling_id"),
                    opprettet = row.norskZonedDateTime("opprettet"),
                    emneknagger = hentEmneknaggerForOppgave(oppgaveId),
                    tilstand = row.string("tilstand").let { Oppgave.Tilstand.Type.valueOf(it) },
                )
            }.asSingle,
        ) ?: throw DataNotFoundException("Fant ikke oppgave med id $oppgaveId")

    override fun finnOppgaverFor(ident: String): List<Oppgave> {
        return sessionOf(dataSource).run(
            queryOf(
                //language=PostgreSQL
                statement = """
                    SELECT pers.ident, oppg.tilstand, oppg.opprettet, oppg.behandling_id, oppg.id
                    FROM oppgave_v1 oppg
                    JOIN behandling_v1 beha ON beha.id = oppg.behandling_id
                    JOIN person_v1 pers ON pers.id = beha.person_id
                    WHERE pers.ident = :ident
                """.trimIndent(),
                paramMap = mapOf(
                    "ident" to ident,
                ),
            ).map {
                Oppgave.rehydrer(
                    oppgaveId = it.uuid("id"),
                    ident = it.string("ident"),
                    behandlingId = it.uuid("behandling_id"),
                    opprettet = it.norskZonedDateTime("opprettet"),
                    emneknagger = hentEmneknaggerForOppgave(it.uuid("id")),
                    tilstand = it.string("tilstand").let { Oppgave.Tilstand.Type.valueOf(it) },
                )
            }.asList,
        )
    }
}
