package no.nav.dagpenger.saksbehandling.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UkjentTilstandException
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.db.DBUtils.norskZonedDateTime
import no.nav.dagpenger.saksbehandling.db.Søkefilter.Periode.Companion.UBEGRENSET_PERIODE
import no.nav.dagpenger.saksbehandling.logger
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
                statement =
                    """
                    INSERT INTO person_v1
                        (id, ident) 
                    VALUES
                        (:id, :ident) 
                    ON CONFLICT (id) DO UPDATE SET ident = :ident
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to person.id,
                        "ident" to person.ident,
                    ),
            ).asUpdate,
        )
    }

    override fun finnPerson(ident: String): Person? {
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
                    Person(
                        id = row.uuid("id"),
                        ident = row.string("ident"),
                    )
                }.asSingle,
            )
        }
    }

    override fun hentPerson(ident: String) = finnPerson(ident) ?: throw DataNotFoundException("Kan ikke finne person med ident $ident")

    override fun slettBehandling(behandlingId: UUID) {
        val behandling =
            try {
                hentBehandling(behandlingId)
            } catch (e: DataNotFoundException) {
                logger.warn("Behandling med id $behandlingId finnes ikke, sletter ikke noe")
                return
            }
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.slettBehandling(behandlingId)
                tx.slettPersonUtenBehandlinger(behandling.person.ident)
            }
        }
    }

    private fun TransactionalSession.slettPersonUtenBehandlinger(ident: String) {
        run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    DELETE FROM person_v1 pers 
                    WHERE pers.ident = :ident
                    AND NOT EXISTS(
                        SELECT 1 
                        FROM behandling_v1 beha 
                        WHERE beha.person_id = pers.id
                    )
                    """.trimMargin(),
                paramMap = mapOf("ident" to ident),
            ).asUpdate,
        )
    }

    private fun TransactionalSession.slettEmneknaggerFor(oppgaveIder: List<UUID>) {
        this.batchPreparedStatement(
            //language=PostgreSQL
            statement = "DELETE FROM emneknagg_v1 WHERE oppgave_id = ?",
            params = listOf(oppgaveIder),
        )
    }

    private fun TransactionalSession.slettOppgaver(oppgaveIder: List<UUID>) {
        this.batchPreparedStatement(
            //language=PostgreSQL
            statement = "DELETE FROM oppgave_v1 WHERE id = ?",
            params = listOf(oppgaveIder),
        )
    }

    private fun TransactionalSession.slettBehandling(behandlingId: UUID) {
        run(
            queryOf(
                //language=PostgreSQL
                statement = "DELETE  FROM behandling_v1 WHERE id = :behandling_id",
                paramMap = mapOf("behandling_id" to behandlingId),
            ).asUpdate,
        )
    }

    override fun lagre(behandling: Behandling) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagre(behandling.person)
                tx.lagre(behandling)
            }
        }
    }

    override fun lagre(oppgave: Oppgave) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagre(oppgave)
            }
        }
    }

    override fun finnBehandling(behandlingId: UUID): Behandling? {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT beha.id behandling_id, beha.opprettet, pers.id person_id, pers.ident
                        FROM   behandling_v1 beha
                        JOIN   person_v1     pers ON pers.id = beha.person_id
                        WHERE  beha.id = :behandling_id
                        """.trimIndent(),
                    paramMap = mapOf("behandling_id" to behandlingId),
                ).map { row ->
                    val ident = row.string("ident")
                    Behandling.rehydrer(
                        behandlingId = behandlingId,
                        person = finnPerson(ident)!!,
                        opprettet = row.norskZonedDateTime("opprettet"),
                    )
                }.asSingle,
            )
        }
    }

    override fun hentBehandling(behandlingId: UUID): Behandling {
        return finnBehandling(behandlingId)
            ?: throw DataNotFoundException("Kunne ikke finne behandling med id: $behandlingId")
    }

    private fun hentOppgaverFraBehandling(
        behandlingId: UUID,
        ident: String,
    ): List<Oppgave> {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        $hubba
                        WHERE  behandling_id = :behandling_id
                        """.trimIndent(),
                    paramMap = mapOf("behandling_id" to behandlingId),
                ).map { row -> row.rehydrerOppgave() }.asList,
            )
        }
    }

    private fun hentEmneknaggerForOppgave(oppgaveId: UUID): Set<String> {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT emneknagg
                        FROM   emneknagg_v1
                        WHERE  oppgave_id = :oppgave_id
                        """.trimIndent(),
                    paramMap = mapOf("oppgave_id" to oppgaveId),
                ).map { row ->
                    row.string("emneknagg")
                }.asList,
            )
        }.toSet()
    }

    private fun Session.lagre(behandling: Behandling) {
        run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO behandling_v1
                        (id, person_id, opprettet)
                    VALUES
                        (:id, :person_id, :opprettet) 
                    ON CONFLICT DO NOTHING
                    """.trimIndent(),
                paramMap =
                    mapOf(
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
                statement =
                    """
                    INSERT INTO oppgave_v1
                        (id, behandling_id, tilstand, opprettet, saksbehandler_ident)
                    VALUES
                        (:id, :behandling_id, :tilstand, :opprettet, :saksbehandler_ident) 
                    ON CONFLICT(id) DO UPDATE SET
                     tilstand = :tilstand,
                     saksbehandler_ident = :saksbehandler_ident
                    
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to oppgave.oppgaveId,
                        "behandling_id" to oppgave.behandlingId,
                        "tilstand" to oppgave.tilstand().name,
                        "opprettet" to oppgave.opprettet,
                        "saksbehandler_ident" to oppgave.saksbehandlerIdent,
                    ),
            ).asUpdate,
        )
        lagre(oppgave.oppgaveId, oppgave.emneknagger)
    }

    private fun TransactionalSession.lagre(
        oppgaveId: UUID,
        emneknagger: Set<String>,
    ) {
        emneknagger.forEach { emneknagg ->
            run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO emneknagg_v1
                            (oppgave_id, emneknagg) 
                        VALUES
                            (:oppgave_id, :emneknagg)
                        ON CONFLICT ON CONSTRAINT emneknagg_oppgave_unique DO NOTHING
                        """.trimIndent(),
                    paramMap = mapOf("oppgave_id" to oppgaveId, "emneknagg" to emneknagg),
                ).asUpdate,
            )
        }
    }

    override fun hentBehandlingFra(oppgaveId: UUID): Behandling {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT beha.id behandling_id, beha.opprettet, pers.id person_id, pers.ident
                        FROM   behandling_v1 beha
                        JOIN   person_v1     pers ON pers.id = beha.person_id
                        JOIN   oppgave_v1    oppg ON oppg.behandling_id = beha.id
                        WHERE  oppg.id = :oppgave_id
                        """.trimIndent(),
                    paramMap = mapOf("oppgave_id" to oppgaveId),
                ).map { row ->
                    val ident = row.string("ident")
                    Behandling.rehydrer(
                        behandlingId = row.uuid("behandling_id"),
                        person = hentPerson(ident),
                        opprettet = row.norskZonedDateTime("opprettet"),
                    )
                }.asSingle,
            )
        } ?: throw DataNotFoundException("Kunne ikke finne behandling med for oppgave-id: $oppgaveId")
    }

    override fun hentAlleOppgaverMedTilstand(tilstand: Type): List<Oppgave> =
        søk(
            Søkefilter(
                tilstand = setOf(tilstand),
                periode = UBEGRENSET_PERIODE,
                saksbehandlerIdent = null,
            ),
        )

    override fun hentNesteOppgavenTil(saksbehandlerIdent: String): Oppgave? {
        sessionOf(dataSource).use { session ->
            val oppgaveId =
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            UPDATE oppgave_v1
                            SET saksbehandler_ident = :saksbehandler_ident
                              , tilstand            = 'UNDER_BEHANDLING'
                            WHERE id = (SELECT id
                                        FROM  oppgave_v1
                                        WHERE tilstand = 'KLAR_TIL_BEHANDLING'
                                        AND   saksbehandler_ident IS NULL
                                        ORDER BY opprettet
                                            FETCH FIRST 1 ROWS ONLY)
                            RETURNING *;
                            
                            """.trimIndent(),
                        paramMap = mapOf("saksbehandler_ident" to saksbehandlerIdent),
                    ).map { row ->
                        row.uuidOrNull("id")
                    }.asSingle,
                )
            return oppgaveId?.let {
                hentOppgave(it)
            }
        }
    }

    override fun hentOppgaveIdFor(behandlingId: UUID): UUID? {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT id 
                        FROM oppgave_v1 
                        WHERE behandling_id = :behandling_id
                        """.trimIndent(),
                    paramMap = mapOf("behandling_id" to behandlingId),
                ).map { row ->
                    row.uuidOrNull("id")
                }.asSingle,
            )
        }
    }

    //language=PostgreSQL
    val hubba =
        """
        SELECT  pers.id AS person_id, pers.ident AS person_ident, 
                oppg.id AS oppgave_id, oppg.tilstand, oppg.opprettet AS oppgave_opprettet, oppg.behandling_id, oppg.saksbehandler_ident,
                beha.opprettet as behandling_opprettet
        FROM    oppgave_v1    oppg
        JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
        JOIN    person_v1     pers ON pers.id = beha.person_id
        """.trimIndent()

    private fun Row.rehydrerOppgave(): Oppgave {
        val behandlingId = this.uuid("behandling_id")
        val oppgaveId = this.uuid("oppgave_id")
        val behandling =
            Behandling.rehydrer(
                behandlingId = behandlingId,
                person =
                    Person(
                        id = this.uuid("person_id"),
                        ident = this.string("person_ident"),
                    ),
                opprettet = this.norskZonedDateTime("behandling_opprettet"),
            )

        return Oppgave.rehydrer(
            oppgaveId = oppgaveId,
            ident = this.string("person_ident"),
            saksbehandlerIdent = this.stringOrNull("saksbehandler_ident"),
            behandlingId = behandlingId,
            opprettet = this.norskZonedDateTime("oppgave_opprettet"),
            emneknagger = hentEmneknaggerForOppgave(oppgaveId),
            tilstand =
                this.string("tilstand").let { tilstand ->
                    when (tilstand) {
                        OPPRETTET.name -> Oppgave.Opprettet
                        KLAR_TIL_BEHANDLING.name -> Oppgave.KlarTilBehandling
                        UNDER_BEHANDLING.name -> Oppgave.UnderBehandling
                        FERDIG_BEHANDLET.name -> Oppgave.FerdigBehandlet
                        else -> throw UkjentTilstandException("Kunne ikke rehydrere med ugyldig tilstand: $tilstand")
                    }
                },
            behandling = behandling,
        )
    }

    override fun hentOppgave(oppgaveId: UUID): Oppgave =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """ $hubba WHERE  oppg.id = :oppgave_id """,
                    paramMap = mapOf("oppgave_id" to oppgaveId),
                ).map { row -> row.rehydrerOppgave() }.asSingle,
            )
        } ?: throw DataNotFoundException("Fant ikke oppgave med id $oppgaveId")

    override fun finnOppgaverFor(ident: String): List<Oppgave> {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """ $hubba WHERE  pers.ident = :ident """,
                    paramMap =
                        mapOf(
                            "ident" to ident,
                        ),
                ).map { row -> row.rehydrerOppgave() }.asList,
            )
        }
    }

    override fun søk(søkeFilter: Søkefilter): List<Oppgave> {
        return sessionOf(dataSource).use { session ->
            val tilstander = søkeFilter.tilstand.joinToString { "'$it'" }

            val saksBehandlerClause =
                søkeFilter.saksbehandlerIdent?.let { "AND oppg.saksbehandler_ident = :saksbehandler_ident" } ?: ""

            //language=PostgreSQL
            val sql =
                """
                    $hubba
                    WHERE  oppg.tilstand IN ($tilstander)
                    AND    date_trunc('day', oppg.opprettet) <= :tom
                    AND    date_trunc('day', oppg.opprettet) >= :fom
                """ + saksBehandlerClause

            val queryOf =
                queryOf(
                    statement = sql.trimIndent(),
                    paramMap =
                        mapOf(
                            "tilstander" to tilstander,
                            "fom" to søkeFilter.periode.fom,
                            "tom" to søkeFilter.periode.tom,
                            "saksbehandler_ident" to søkeFilter.saksbehandlerIdent,
                        ),
                )
            session.run(
                queryOf.map { row ->
                    row.rehydrerOppgave()
                }.asList,
            )
        }
    }
}
