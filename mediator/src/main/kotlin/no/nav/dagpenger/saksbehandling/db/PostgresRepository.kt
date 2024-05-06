package no.nav.dagpenger.saksbehandling.db

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.db.Søkefilter.Periode.Companion.UBEGRENSET_PERIODE
import no.nav.dagpenger.saksbehandling.logger
import java.util.UUID
import javax.sql.DataSource

class DataNotFoundException(message: String) : RuntimeException(message)

class PostgresRepository(private val dataSource: DataSource) : Repository {
    override fun lagre(person: Person) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagre(person)
            }
        }
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

    override fun lagre(behandling: Behandling) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
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
                        WHERE  beha.id     = :behandling_id
                        """.trimIndent(),
                    paramMap = mapOf("behandling_id" to behandlingId),
                ).map { row ->
                    val ident = row.string("ident")
                    Behandling.rehydrer(
                        behandlingId = behandlingId,
                        person = finnPerson(ident)!!,
                        opprettet = row.localDateTime("opprettet"),
                    )
                }.asSingle,
            )
        }
    }

    override fun hentBehandling(behandlingId: UUID): Behandling {
        return finnBehandling(behandlingId)
            ?: throw DataNotFoundException("Kunne ikke finne behandling med id: $behandlingId")
    }

    override fun hentAlleOppgaverMedTilstand(tilstand: Type): List<Oppgave> =
        søk(
            Søkefilter(
                tilstand = setOf(tilstand),
                periode = UBEGRENSET_PERIODE,
                saksbehandlerIdent = null,
            ),
        )

    override fun tildelNesteOppgaveTil(saksbehandlerIdent: String): Oppgave? {
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
                            WHERE id = (SELECT   id
                                        FROM     oppgave_v1
                                        WHERE    tilstand = 'KLAR_TIL_BEHANDLING'
                                        AND      saksbehandler_ident IS NULL
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

    override fun tildelNesteOppgaveTil(
        saksbehandlerIdent: String,
        filter: Søkefilter,
    ): Oppgave? {
        sessionOf(dataSource).use { session ->
            val emneknagger = filter.emneknagg.joinToString { "'$it'" }
            val emneknaggClause =
                if (filter.emneknagg.isNotEmpty()) {
                    """
                    AND EXISTS(
                        SELECT 1
                        FROM   emneknagg_v1 emne
                        WHERE  emne.oppgave_id = oppg.id
                        AND    emne.emneknagg IN ($emneknagger)
                    )
                    """.trimIndent()
                } else {
                    ""
                }
            val orderByReturningStatement =
                """
                ORDER BY oppg.opprettet
                FETCH FIRST 1 ROWS ONLY)
                RETURNING *;
                """.trimIndent()

            val updateStatement =
                """
                UPDATE oppgave_v1
                SET    saksbehandler_ident = :saksbehandler_ident
                     , tilstand            = 'UNDER_BEHANDLING'
                WHERE id = (SELECT   oppg.id
                            FROM     oppgave_v1 oppg
                            WHERE    oppg.tilstand = 'KLAR_TIL_BEHANDLING'
                            AND      oppg.saksbehandler_ident IS NULL
                            AND      oppg.opprettet >= :fom
                            AND      oppg.opprettet <  :tom_pluss_1_dag
                """.trimIndent() + emneknaggClause + orderByReturningStatement

            val oppgaveId =
                session.run(
                    queryOf(
                        statement = updateStatement,
                        paramMap =
                            mapOf(
                                "saksbehandler_ident" to saksbehandlerIdent,
                                "fom" to filter.periode.fom,
                                "tom_pluss_1_dag" to filter.periode.tom.plusDays(1),
                            ),
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
                        FROM   oppgave_v1 
                        WHERE  behandling_id = :behandling_id
                        """.trimIndent(),
                    paramMap = mapOf("behandling_id" to behandlingId),
                ).map { row ->
                    row.uuidOrNull("id")
                }.asSingle,
            )
        }
    }

    override fun hentOppgaveFor(behandlingId: UUID): Oppgave =
        finnOppgaveFor(behandlingId) ?: throw DataNotFoundException("Fant ikke oppgave for behandlingId $behandlingId")

    override fun finnOppgaveFor(behandlingId: UUID): Oppgave? =
        søk(
            søkeFilter =
                Søkefilter(
                    periode = UBEGRENSET_PERIODE,
                    tilstand = Type.Companion.values,
                    behandlingId = behandlingId,
                ),
        ).singleOrNull()

    //language=PostgreSQL
    override fun hentOppgave(oppgaveId: UUID): Oppgave =
        søk(
            Søkefilter(
                periode = UBEGRENSET_PERIODE,
                tilstand = Type.Companion.values,
                oppgaveId = oppgaveId,
            ),
        ).singleOrNull() ?: throw DataNotFoundException("Fant ikke oppgave med id $oppgaveId")

    override fun finnOppgaverFor(ident: String): List<Oppgave> =
        søk(
            Søkefilter(
                periode = UBEGRENSET_PERIODE,
                tilstand = Type.Companion.values,
                saksbehandlerIdent = null,
                personIdent = ident,
            ),
        )

    override fun søk(søkeFilter: Søkefilter): List<Oppgave> {
        return sessionOf(dataSource).use { session ->
            val tilstander = søkeFilter.tilstand.joinToString { "'$it'" }

            val saksBehandlerClause =
                søkeFilter.saksbehandlerIdent?.let { "AND oppg.saksbehandler_ident = :saksbehandler_ident" } ?: ""

            val personIdentClause = søkeFilter.personIdent?.let { "AND pers.ident = :person_ident" } ?: ""

            val oppgaveIdClause = søkeFilter.oppgaveId?.let { "AND oppg.id = :oppgave_id" } ?: ""

            val behandlingIdClause = søkeFilter.behandlingId?.let { "AND oppg.behandling_id = :behandling_id" } ?: ""

            val emneknagger = søkeFilter.emneknagg.joinToString { "'$it'" }
            val emneknaggClause =
                if (søkeFilter.emneknagg.isNotEmpty()) {
                    """
                    AND EXISTS(
                        SELECT 1
                        FROM   emneknagg_v1 emne
                        WHERE  emne.oppgave_id = oppg.id
                        AND    emne.emneknagg IN ($emneknagger)
                    )
                    """.trimIndent()
                } else {
                    ""
                }

            // OBS: På grunn av at vi sammenligner "opprettet" (som er en timestamp) med fom- og tom-datoer (uten tidsdel),
            //     sjekker vi at "opprettet" er MINDRE enn tom-dato-pluss-1-dag.
            //language=PostgreSQL
            val sql =
                StringBuilder(
                    """
                    ${
                        """
                        SELECT  pers.id AS person_id, 
                                pers.ident AS person_ident, 
                                oppg.id AS oppgave_id, 
                                oppg.tilstand, 
                                oppg.opprettet AS oppgave_opprettet, 
                                oppg.behandling_id, 
                                oppg.saksbehandler_ident,
                                beha.opprettet AS behandling_opprettet
                        FROM    oppgave_v1    oppg
                        JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                        JOIN    person_v1     pers ON pers.id = beha.person_id
                        """.trimIndent()
                    }
                    WHERE  oppg.tilstand IN ($tilstander)
                    AND    oppg.opprettet >= :fom
                    AND    oppg.opprettet <  :tom_pluss_1_dag
                """,
                )
                    .append(saksBehandlerClause, personIdentClause, oppgaveIdClause, behandlingIdClause, emneknaggClause)
                    .toString()

            val queryOf =
                queryOf(
                    statement = sql,
                    paramMap =
                        mapOf(
                            "tilstander" to tilstander,
                            "fom" to søkeFilter.periode.fom,
                            "tom_pluss_1_dag" to søkeFilter.periode.tom.plusDays(1),
                            "saksbehandler_ident" to søkeFilter.saksbehandlerIdent,
                            "person_ident" to søkeFilter.personIdent,
                            "oppgave_id" to søkeFilter.oppgaveId,
                            "behandling_id" to søkeFilter.behandlingId,
                            "emneknagger" to emneknagger,
                        ),
                )
            session.run(
                queryOf.map { row ->
                    row.rehydrerOppgave()
                }.asList,
            )
        }
    }

    private fun TransactionalSession.lagre(person: Person) {
        run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO person_v1
                        (id, ident) 
                    VALUES
                        (:id, :ident) 
                    ON CONFLICT (id) DO NOTHING
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to person.id,
                        "ident" to person.ident,
                    ),
            ).asUpdate,
        )
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

    private fun TransactionalSession.slettBehandling(behandlingId: UUID) {
        run(
            queryOf(
                //language=PostgreSQL
                statement = "DELETE  FROM behandling_v1 WHERE id = :behandling_id",
                paramMap = mapOf("behandling_id" to behandlingId),
            ).asUpdate,
        )
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

    private fun TransactionalSession.lagre(behandling: Behandling) {
        this.lagre(behandling.person)
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

    private fun TransactionalSession.lagre(oppgave: Oppgave) {
        this.lagre(oppgave.behandling)
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
                opprettet = this.localDateTime("behandling_opprettet"),
            )

        return Oppgave.rehydrer(
            oppgaveId = oppgaveId,
            ident = this.string("person_ident"),
            saksbehandlerIdent = this.stringOrNull("saksbehandler_ident"),
            behandlingId = behandlingId,
            opprettet = this.localDateTime("oppgave_opprettet"),
            emneknagger = hentEmneknaggerForOppgave(oppgaveId),
            tilstand = this.string("tilstand").let { tilstand -> Oppgave.Tilstand.fra(tilstand) },
            behandling = behandling,
        )
    }
}
