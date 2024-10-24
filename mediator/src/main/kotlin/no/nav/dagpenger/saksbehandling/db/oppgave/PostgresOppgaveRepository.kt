package no.nav.dagpenger.saksbehandling.db.oppgave

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.adressebeskyttelse.AdressebeskyttelseRepository
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode.Companion.UBEGRENSET_PERIODE
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlarTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilbakeTilKlarTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilbakeTilUnderKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.serder.tilHendelse
import no.nav.dagpenger.saksbehandling.serder.tilJson
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingRepository
import org.postgresql.util.PGobject
import java.util.UUID
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

class PostgresOppgaveRepository(private val dataSource: DataSource) :
    OppgaveRepository,
    SkjermingRepository,
    AdressebeskyttelseRepository {
    override fun lagre(person: Person) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagre(person)
            }
        }
    }

    override fun tildelOgHentNesteOppgave(
        nesteOppgaveHendelse: NesteOppgaveHendelse,
        filter: TildelNesteOppgaveFilter,
    ): Oppgave? =
        tildelNesteOppgave(nesteOppgaveHendelse, filter)?.let {
            hentOppgave(it)
        }

    private fun tildelNesteOppgave(
        nesteOppgaveHendelse: NesteOppgaveHendelse,
        filter: TildelNesteOppgaveFilter,
    ): UUID? {
        return sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                val emneknagger = filter.emneknagg.joinToString { "'$it'" }
                val tillatteGraderinger = filter.harTilgangTilAdressebeskyttelser.joinToString { "'$it'" }
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
                            FROM     oppgave_v1    oppg
                            JOIN     behandling_v1 beha ON beha.id = oppg.behandling_id
                            JOIN     person_v1     pers ON pers.id = beha.person_id
                            WHERE    oppg.tilstand = 'KLAR_TIL_BEHANDLING'
                            AND      oppg.saksbehandler_ident IS NULL
                            AND      oppg.opprettet >= :fom
                            AND      oppg.opprettet <  :tom_pluss_1_dag
                            AND    ( NOT pers.skjermes_som_egne_ansatte
                                  OR :har_tilgang_til_egne_ansatte )
                            AND     pers.adressebeskyttelse_gradering IN ($tillatteGraderinger) 
                """ + emneknaggClause + orderByReturningStatement
                val oppgaveId: UUID? =
                    tx.run(
                        queryOf(
                            statement = updateStatement,
                            paramMap =
                                mapOf(
                                    "saksbehandler_ident" to nesteOppgaveHendelse.ansvarligIdent,
                                    "fom" to filter.periode.fom,
                                    "tom_pluss_1_dag" to filter.periode.tom.plusDays(1),
                                    "har_tilgang_til_egne_ansatte" to filter.harTilgangTilEgneAnsatte,
                                ),
                        ).map { row ->
                            row.uuidOrNull("id")
                        }.asSingle,
                    )
                oppgaveId?.let {
                    tx.lagre(
                        it,
                        Tilstandsendring(
                            tilstand = Oppgave.Tilstand.Type.UNDER_BEHANDLING,
                            hendelse = nesteOppgaveHendelse,
                        ),
                    )
                }
                oppgaveId
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
                        skjermesSomEgneAnsatte = row.boolean("skjermes_som_egne_ansatte"),
                        adressebeskyttelseGradering = row.adresseBeskyttelseGradering(),
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
                        hendelse = finnHendelseForBehandling(behandlingId),
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

    private fun hentOppgave(
        transactionalSession: TransactionalSession,
        oppgaveId: UUID,
    ): Oppgave? {
        val oppgave =
            transactionalSession.transaction { tx ->
                tx.run(
                    queryOf(
                        //language=PostgreSQL
                        statement = "SELECT * FROM oppgave_v1 WHERE id = :oppgave_id",
                        paramMap = mapOf("oppgave_id" to oppgaveId),
                    ).map { row ->
                        row.rehydrerOppgave()
                    }.asSingle,
                )
            }
        return oppgave
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
                    tilstand = Type.values,
                    behandlingId = behandlingId,
                ),
        ).singleOrNull()

    override fun fjerneEmneknagg(
        behandlingId: UUID,
        ikkeRelevantEmneknagg: String,
    ): Boolean {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        DELETE FROM emneknagg_v1
                        WHERE  oppgave_id = (SELECT oppg.id
                                             FROM   oppgave_v1 oppg
                                             WHERE  oppg.behandling_id = :behandling_id)
                        AND    emneknagg = :emneknagg
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "behandling_id" to behandlingId,
                            "emneknagg" to ikkeRelevantEmneknagg,
                        ),
                ).asUpdate,
            ) > 0
        }
    }

    override fun personSkjermesSomEgneAnsatte(oppgaveId: UUID): Boolean? {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT pers.skjermes_som_egne_ansatte
                    FROM   person_v1     pers
                    JOIN   behandling_v1 beha ON beha.person_id = pers.id
                    JOIN   oppgave_v1    oppg ON oppg.behandling_id = beha.id
                    WHERE  oppg.id = :oppgave_id
                    """.trimMargin(),
                    mapOf("oppgave_id" to oppgaveId),
                ).map { row ->
                    row.boolean("skjermes_som_egne_ansatte")
                }.asSingle,
            )
        }
    }

    override fun adresseGraderingForPerson(oppgaveId: UUID): AdressebeskyttelseGradering {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT pers.adressebeskyttelse_gradering
                    FROM   person_v1     pers
                    JOIN   behandling_v1 beha ON beha.person_id = pers.id
                    JOIN   oppgave_v1    oppg ON oppg.behandling_id = beha.id
                    WHERE  oppg.id = :oppgave_id
                    """.trimMargin(),
                    mapOf("oppgave_id" to oppgaveId),
                ).map { row ->
                    AdressebeskyttelseGradering.valueOf(row.string("adressebeskyttelse_gradering"))
                }.asSingle,
            )
        } ?: throw DataNotFoundException("Fant ikke person for oppgave med id $oppgaveId")
    }

    //language=PostgreSQL
    override fun hentOppgave(oppgaveId: UUID): Oppgave =
        søk(
            Søkefilter(
                periode = UBEGRENSET_PERIODE,
                tilstand = Type.values,
                oppgaveId = oppgaveId,
            ),
        ).singleOrNull() ?: throw DataNotFoundException("Fant ikke oppgave med id $oppgaveId")

    override fun finnOppgaverFor(ident: String): List<Oppgave> =
        søk(
            Søkefilter(
                periode = UBEGRENSET_PERIODE,
                tilstand = Type.søkbareTyper,
                saksbehandlerIdent = null,
                personIdent = ident,
            ),
        )

    override fun søk(søkeFilter: Søkefilter): List<Oppgave> {
        var oppgaver: List<Oppgave>

        oppgaver =
            sessionOf(dataSource).use { session ->
                val tilstander = søkeFilter.tilstand.joinToString { "'$it'" }

                val saksBehandlerClause =
                    søkeFilter.saksbehandlerIdent?.let { "AND oppg.saksbehandler_ident = :saksbehandler_ident" }
                        ?: ""

                val personIdentClause = søkeFilter.personIdent?.let { "AND pers.ident = :person_ident" } ?: ""

                val oppgaveIdClause = søkeFilter.oppgaveId?.let { "AND oppg.id = :oppgave_id" } ?: ""

                val behandlingIdClause =
                    søkeFilter.behandlingId?.let { "AND oppg.behandling_id = :behandling_id" } ?: ""

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
                                    pers.skjermes_som_egne_ansatte,
                                    pers.adressebeskyttelse_gradering,
                                    oppg.id AS oppgave_id, 
                                    oppg.tilstand, 
                                    oppg.opprettet AS oppgave_opprettet, 
                                    oppg.behandling_id, 
                                    oppg.saksbehandler_ident,
                                    oppg.utsatt_til,
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
                        .append(
                            saksBehandlerClause,
                            personIdentClause,
                            oppgaveIdClause,
                            behandlingIdClause,
                            emneknaggClause,
                        )
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
        return oppgaver
    }

    override fun oppdaterSkjermingStatus(
        fnr: String,
        skjermet: Boolean,
    ): Int {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        UPDATE person_v1
                        SET    skjermes_som_egne_ansatte = :skjermet
                        WHERE  ident = :fnr
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "fnr" to fnr,
                            "skjermet" to skjermet,
                        ),
                ).asUpdate,
            )
        }
    }

    override fun oppdaterAdressebeskyttetStatus(
        fnr: String,
        adresseBeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        UPDATE person_v1
                        SET    adressebeskyttelse_gradering = :adresseBeskyttelseGradering
                        WHERE  ident = :fnr
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "fnr" to fnr,
                            "adresseBeskyttelseGradering" to adresseBeskyttelseGradering.name,
                        ),
                ).asUpdate,
            )
        }
    }

    override fun eksistererIDPsystem(fnrs: Set<String>): Set<String> {
        val identer = fnrs.joinToString { "'$it'" }
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT ident
                        FROM   person_v1
                        WHERE  ident IN ($identer)
                        """.trimIndent(),
                ).map { row ->
                    row.string("ident")
                }.asList,
            ).toSet()
        }
    }
}

private fun TransactionalSession.lagre(person: Person) {
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

private fun rehydrerTilstandsendringHendelse(
    hendelseType: String,
    hendelseJson: String,
): Hendelse {
    return when (hendelseType) {
        "SettOppgaveAnsvarHendelse" -> hendelseJson.tilHendelse<SettOppgaveAnsvarHendelse>()
        "BehandlingAvbruttHendelse" -> hendelseJson.tilHendelse<BehandlingAvbruttHendelse>()
        "FjernOppgaveAnsvarHendelse" -> hendelseJson.tilHendelse<FjernOppgaveAnsvarHendelse>()
        "ForslagTilVedtakHendelse" -> hendelseJson.tilHendelse<ForslagTilVedtakHendelse>()
        "GodkjennBehandlingMedBrevIArena" -> hendelseJson.tilHendelse<GodkjennBehandlingMedBrevIArena>()
        "GodkjentBehandlingHendelse" -> hendelseJson.tilHendelse<GodkjentBehandlingHendelse>()
        "KlarTilKontrollHendelse" -> hendelseJson.tilHendelse<KlarTilKontrollHendelse>()
        "SøknadsbehandlingOpprettetHendelse" -> hendelseJson.tilHendelse<SøknadsbehandlingOpprettetHendelse>()
        "TilbakeTilKontrollHendelse" -> hendelseJson.tilHendelse<TilbakeTilKlarTilKontrollHendelse>()
        "TilbakeTilUnderKontrollHendelse" -> hendelseJson.tilHendelse<TilbakeTilUnderKontrollHendelse>()
        "TomHendelse" -> hendelseJson.tilHendelse<TomHendelse>()
        "UtsettOppgaveHendelse" -> hendelseJson.tilHendelse<UtsettOppgaveHendelse>()
        "VedtakFattetHendelse" -> hendelseJson.tilHendelse<VedtakFattetHendelse>()
        "NesteOppgaveHendelse" -> hendelseJson.tilHendelse<NesteOppgaveHendelse>()
        else -> throw IllegalArgumentException("Ukjent hendelse type $hendelseType")
    }
}

private fun Row.rehydrerHendelse(): Hendelse {
    return when (val hendelseType = this.string("hendelse_type")) {
        "TomHendelse" -> return TomHendelse
        "SøknadsbehandlingOpprettetHendelse" -> SøknadsbehandlingOpprettetHendelse.fromJson(this.string("hendelse_data"))
        else -> throw IllegalArgumentException("Ukjent hendelse type $hendelseType")
    }
}

private fun finnHendelseForBehandling(behandlingId: UUID): Hendelse {
    return sessionOf(dataSource).use { session ->
        session.run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    SELECT hendelse_type, hendelse_data
                    FROM   hendelse_v1
                    WHERE  behandling_id = :behandling_id
                    """.trimIndent(),
                paramMap = mapOf("behandling_id" to behandlingId),
            ).map { row ->
                row.rehydrerHendelse()
            }.asSingle,
        ) ?: TomHendelse
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

private fun hentTilstandsloggForOppgave(oppgaveId: UUID): Tilstandslogg {
    return sessionOf(dataSource).use { session ->
        session.run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    SELECT id, oppgave_id, tilstand,hendelse_type, hendelse, tidspunkt
                    FROM   oppgave_tilstand_logg_v1
                    WHERE  oppgave_id = :oppgave_id
                    """.trimIndent(),
                paramMap = mapOf("oppgave_id" to oppgaveId),
            ).map { row ->
                Tilstandsendring(
                    id = row.uuid("id"),
                    tilstand = Type.valueOf(row.string("tilstand")),
                    hendelse =
                        rehydrerTilstandsendringHendelse(
                            hendelseType = row.string("hendelse_type"),
                            hendelseJson = row.string("hendelse"),
                        ),
                    tidspunkt = row.localDateTime("tidspunkt"),
                )
            }.asList,
        ).let { Tilstandslogg.rehydrer(it) }
    }
}

private fun TransactionalSession.lagreHendelse(
    behandlingId: UUID,
    hendelse: Hendelse,
) {
    run(
        queryOf(
            //language=PostgreSQL
            statement =
                """
                INSERT INTO hendelse_v1
                    (behandling_id, hendelse_type, hendelse_data)
                VALUES
                    (:behandling_id, :hendelse_type, :hendelse_data) 
                ON CONFLICT DO NOTHING
                """.trimIndent(),
            paramMap =
                mapOf(
                    "behandling_id" to behandlingId,
                    "hendelse_type" to hendelse.javaClass.simpleName,
                    "hendelse_data" to
                        PGobject().also {
                            it.type = "JSONB"
                            it.value = hendelse.tilJson()
                        },
                ),
        ).asUpdate,
    )
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
    this.lagreHendelse(behandling.behandlingId, behandling.hendelse)
}

private fun TransactionalSession.lagre(oppgave: Oppgave) {
    this.lagre(oppgave.behandling)
    run(
        queryOf(
            //language=PostgreSQL
            statement =
                """
                INSERT INTO oppgave_v1
                    (id, behandling_id, tilstand, opprettet, saksbehandler_ident, utsatt_til)
                VALUES
                    (:id, :behandling_id, :tilstand, :opprettet, :saksbehandler_ident, :utsatt_til) 
                ON CONFLICT(id) DO UPDATE SET
                 tilstand = :tilstand,
                 saksbehandler_ident = :saksbehandler_ident,
                 utsatt_til = :utsatt_til
                
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to oppgave.oppgaveId,
                    "behandling_id" to oppgave.behandlingId,
                    "tilstand" to oppgave.tilstand().type.name,
                    "opprettet" to oppgave.opprettet,
                    "saksbehandler_ident" to oppgave.behandlerIdent,
                    "utsatt_til" to oppgave.utsattTil(),
                ),
        ).asUpdate,
    )
    lagre(oppgave.oppgaveId, oppgave.emneknagger)
    lagre(oppgave.oppgaveId, oppgave.tilstandslogg)
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

private fun TransactionalSession.lagre(
    oppgaveId: UUID,
    tilstandsendring: Tilstandsendring,
) {
    this.run(
        queryOf(
            //language=PostgreSQL
            statement =
                """
                INSERT INTO oppgave_tilstand_logg_v1
                    (id, oppgave_id, tilstand, hendelse_type, hendelse, tidspunkt)
                VALUES
                    (:id, :oppgave_id, :tilstand,:hendelse_type, :hendelse, :tidspunkt)
                ON CONFLICT DO NOTHING
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to tilstandsendring.id,
                    "oppgave_id" to oppgaveId,
                    "tilstand" to tilstandsendring.tilstand.name,
                    "hendelse_type" to tilstandsendring.hendelse.javaClass.simpleName,
                    "hendelse" to
                        PGobject().also {
                            it.type = "JSONB"
                            it.value = tilstandsendring.hendelse.tilJson()
                        },
                    "tidspunkt" to tilstandsendring.tidspunkt,
                ),
        ).asUpdate,
    )
}

private fun TransactionalSession.lagre(
    oppgaveId: UUID,
    tilstandslogg: Tilstandslogg,
) {
    tilstandslogg.forEach { tilstandsendring ->
        this.lagre(oppgaveId, tilstandsendring)
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
                    skjermesSomEgneAnsatte = this.boolean("skjermes_som_egne_ansatte"),
                    adressebeskyttelseGradering = this.adresseBeskyttelseGradering(),
                ),
            opprettet = this.localDateTime("behandling_opprettet"),
            hendelse = finnHendelseForBehandling(behandlingId),
        )

    val tilstand =
        Oppgave.Tilstand.fra(
            type = this.string("tilstand"),
        )

    return Oppgave.rehydrer(
        oppgaveId = oppgaveId,
        ident = this.string("person_ident"),
        behandlerIdent = this.stringOrNull("saksbehandler_ident"),
        behandlingId = behandlingId,
        opprettet = this.localDateTime("oppgave_opprettet"),
        emneknagger = hentEmneknaggerForOppgave(oppgaveId),
        tilstand = tilstand,
        behandling = behandling,
        utsattTil = this.localDateOrNull("utsatt_til"),
        tilstandslogg = hentTilstandsloggForOppgave(oppgaveId),
    )
}

private fun Row.adresseBeskyttelseGradering(): AdressebeskyttelseGradering {
    return AdressebeskyttelseGradering.valueOf(this.string("adressebeskyttelse_gradering"))
}

class DataNotFoundException(message: String) : RuntimeException(message)
