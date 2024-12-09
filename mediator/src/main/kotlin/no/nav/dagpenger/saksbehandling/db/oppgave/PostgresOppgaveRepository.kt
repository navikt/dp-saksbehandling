package no.nav.dagpenger.saksbehandling.db.oppgave

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Notat
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerLåsAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerOpplåsingAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.BehandlesIArena
import no.nav.dagpenger.saksbehandling.Oppgave.FerdigBehandlet
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilKontroll
import no.nav.dagpenger.saksbehandling.Oppgave.Opprettet
import no.nav.dagpenger.saksbehandling.Oppgave.PåVent
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.BEHANDLES_I_ARENA
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UgyldigTilstandException
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.UnderKontroll
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.adressebeskyttelse.AdressebeskyttelseRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode.Companion.UBEGRENSET_PERIODE
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingLåstHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpplåstHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.serder.tilHendelse
import no.nav.dagpenger.saksbehandling.serder.tilJson
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingRepository
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class PostgresOppgaveRepository(private val datasource: DataSource) :
    OppgaveRepository,
    SkjermingRepository,
    AdressebeskyttelseRepository {
    override fun lagre(person: Person) {
        sessionOf(datasource).use { session ->
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
        return sessionOf(datasource).use { session ->
            session.transaction { tx ->
                val emneknagger = filter.emneknagg.joinToString { "'$it'" }
                val tillatteGraderinger = filter.adressebeskyttelseTilganger.joinToString { "'$it'" }

                // language=SQL
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

                // Oppdaterer saksbehandler_ident og tilstand avhengig av om oppgaven som hentes som neste er klar til
                // behandling eller klar til kontroll. Pålogget saksbehandler må ha rettighet til aktuell oppgave.
                // Det sjekkes derfor mot tilganger i utplukket.

                // language=SQL
                val withStatement =
                    """
                    WITH neste_oppgave AS ( WITH alle_oppgaver AS (
                    """.trimIndent()

                // language=SQL
                val selectKlarTilBehandlingOppgaver =
                    """
                            SELECT   oppg.*
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
                            """ + emneknaggClause

                // language=SQL
                val unionAll = """UNION ALL"""

                // language=SQL
                val selectKlarTilKontrollOppgaver =
                    """
                            SELECT  oppg.*
                            FROM    oppgave_v1    oppg
                            JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                            JOIN    person_v1     pers ON pers.id = beha.person_id
                            JOIN    oppgave_tilstand_logg_v1 logg ON logg.id = 
                                ( SELECT logg2.id
                                  FROM   oppgave_tilstand_logg_v1 logg2
                                  WHERE  logg2.oppgave_id = oppg.id
                                  AND    logg2.tidspunkt = 
                                    (   SELECT MAX(logg3.tidspunkt)
                                        FROM   oppgave_tilstand_logg_v1 logg3
                                        WHERE  logg3.oppgave_id = oppg.id
                                        AND    logg3.tilstand = 'UNDER_BEHANDLING'
                                    )
                                )
                            WHERE   :har_beslutter_rolle
                            AND     oppg.tilstand = 'KLAR_TIL_KONTROLL'
                            AND     oppg.saksbehandler_ident IS NULL
                            AND     oppg.opprettet >= :fom
                            AND     oppg.opprettet <  :tom_pluss_1_dag
                            AND   ( NOT pers.skjermes_som_egne_ansatte
                                 OR :har_tilgang_til_egne_ansatte )
                            AND     pers.adressebeskyttelse_gradering IN ($tillatteGraderinger) 
                            AND     logg.hendelse->'utførtAv'->>'navIdent'::text != :navIdent
                """ + emneknaggClause +
                        """
                        )
                        """.trimIndent()

                // language=SQL
                val selectAlleAktuelleOppgaverOrderByOpprettet =
                    """
                        SELECT *
                        FROM   alle_oppgaver alop
                        ORDER BY alop.opprettet
                        FETCH FIRST 1 ROWS ONLY
                    )
                    """.trimIndent()

                // language=SQL
                val updateNesteOppgave =
                    """
                UPDATE oppgave_v1 oppu
                SET    saksbehandler_ident = :saksbehandler_ident
                     , tilstand            = CASE oppu.tilstand
                                                     WHEN 'KLAR_TIL_BEHANDLING' THEN 'UNDER_BEHANDLING'
                                                     WHEN 'KLAR_TIL_KONTROLL' THEN 'UNDER_KONTROLL'
                                                  END
                FROM  neste_oppgave next
                WHERE next.id = oppu.id
                RETURNING *
                """
                val statement =
                    withStatement +
                        selectKlarTilBehandlingOppgaver +
                        unionAll +
                        selectKlarTilKontrollOppgaver +
                        selectAlleAktuelleOppgaverOrderByOpprettet +
                        updateNesteOppgave

                sikkerlogger.info { "Henter oppgaver med følgende SQL i tildelNesteOppgave: $statement" }
                val oppgaveIdOgTilstandType: Pair<UUID, Type>? =
                    tx.run(
                        queryOf(
                            statement = statement,
                            paramMap =
                                mapOf(
                                    "saksbehandler_ident" to nesteOppgaveHendelse.ansvarligIdent,
                                    "fom" to filter.periode.fom,
                                    "tom_pluss_1_dag" to filter.periode.tom.plusDays(1),
                                    "har_tilgang_til_egne_ansatte" to filter.egneAnsatteTilgang,
                                    "har_beslutter_rolle" to filter.harBeslutterRolle,
                                    "navIdent" to filter.navIdent,
                                ),
                        ).map { row ->
                            val tilstandType = Type.valueOf(row.string("tilstand"))
                            Pair(row.uuid("id"), tilstandType)
                        }.asSingle,
                    )
                oppgaveIdOgTilstandType?.let {
                    tx.lagre(
                        it.first,
                        Tilstandsendring(
                            tilstand = it.second,
                            hendelse = nesteOppgaveHendelse,
                        ),
                    )
                }
                oppgaveIdOgTilstandType?.first
            }
        }
    }

    override fun finnPerson(ident: String): Person? {
        sessionOf(datasource).use { session ->
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
        sessionOf(datasource).use { session ->
            session.transaction { tx ->
                tx.slettBehandling(behandlingId)
                tx.slettPersonUtenBehandlinger(behandling.person.ident)
            }
        }
    }

    override fun lagre(behandling: Behandling) {
        sessionOf(datasource).use { session ->
            session.transaction { tx ->
                tx.lagre(behandling)
            }
        }
    }

    override fun lagre(oppgave: Oppgave) {
        sessionOf(datasource).use { session ->
            session.transaction { tx ->
                tx.lagre(oppgave)
            }
        }
    }

    override fun finnBehandling(behandlingId: UUID): Behandling? {
        return sessionOf(datasource).use { session ->
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
                        hendelse = finnHendelseForBehandling(behandlingId, datasource),
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
            søkeFilter =
                Søkefilter(
                    tilstander = setOf(tilstand),
                    periode = UBEGRENSET_PERIODE,
                    saksbehandlerIdent = null,
                ),
        ).oppgaver

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
                        row.rehydrerOppgave(datasource)
                    }.asSingle,
                )
            }
        return oppgave
    }

    override fun hentOppgaveIdFor(behandlingId: UUID): UUID? {
        return sessionOf(datasource).use { session ->
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
                    tilstander = Type.values,
                    behandlingId = behandlingId,
                ),
        ).oppgaver.singleOrNull()

    override fun fjerneEmneknagg(
        behandlingId: UUID,
        ikkeRelevantEmneknagg: String,
    ): Boolean {
        return sessionOf(datasource).use { session ->
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
        return sessionOf(datasource).use { session ->
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
        return sessionOf(datasource).use { session ->
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

    override fun finnNotat(oppgaveTilstandLoggId: UUID): Notat? = finnNotat(oppgaveTilstandLoggId, datasource)

    override fun lagreNotatFor(oppgave: Oppgave): LocalDateTime {
        return when (val notat = oppgave.tilstand().notat()) {
            null -> throw IllegalStateException("Kan ikke lagre notat for oppgave uten notat")
            else -> {
                sessionOf(datasource).use { session ->
                    session.lagreNotat(
                        notatId = notat.notatId,
                        tilstandsendringId = oppgave.tilstandslogg.first().id,
                        tekst = notat.hentTekst(),
                        skrevetAv = notat.skrevetAv,
                    )
                }
            }
        }
    }

    //language=PostgreSQL
    override fun hentOppgave(oppgaveId: UUID): Oppgave =
        søk(
            Søkefilter(
                periode = UBEGRENSET_PERIODE,
                tilstander = Type.values,
                oppgaveId = oppgaveId,
            ),
        ).oppgaver.singleOrNull() ?: throw DataNotFoundException("Fant ikke oppgave med id $oppgaveId")

    override fun finnOppgaverFor(ident: String): List<Oppgave> =
        søk(
            søkeFilter =
                Søkefilter(
                    periode = UBEGRENSET_PERIODE,
                    tilstander = Type.søkbareTilstander,
                    saksbehandlerIdent = null,
                    personIdent = ident,
                ),
        ).oppgaver

    data class OppgaveSøkResultat(
        val oppgaver: List<Oppgave>,
        val totaltAntallOppgaver: Int,
    )

    override fun søk(søkeFilter: Søkefilter): OppgaveSøkResultat {
        return sessionOf(datasource).use { session ->
            val tilstander = søkeFilter.tilstander.joinToString { "'$it'" }
            val tilstandClause =
                if (søkeFilter.tilstander.isEmpty()) {
                    " AND oppg.tilstand != 'OPPRETTET' "
                } else {
                    " AND oppg.tilstand IN ($tilstander) "
                }

            val saksbehandlerClause =
                søkeFilter.saksbehandlerIdent?.let { "AND oppg.saksbehandler_ident = :saksbehandler_ident " } ?: ""

            val personIdentClause = søkeFilter.personIdent?.let { "AND pers.ident = :person_ident " } ?: ""

            val oppgaveIdClause = søkeFilter.oppgaveId?.let { "AND oppg.id = :oppgave_id " } ?: ""

            val behandlingIdClause =
                søkeFilter.behandlingId?.let { "AND oppg.behandling_id = :behandling_id " } ?: ""

            val emneknaggerAsText: String = søkeFilter.emneknagger.joinToString { "'$it'" }
            val emneknaggClause =
                if (søkeFilter.emneknagger.isNotEmpty()) {
                    """
                    AND EXISTS(
                        SELECT 1
                        FROM   emneknagg_v1 emne
                        WHERE  emne.oppgave_id = oppg.id
                        AND    emne.emneknagg IN ($emneknaggerAsText)
                    )
                    """.trimIndent()
                } else {
                    ""
                }
            val orderByOpprettetClause = """ ORDER BY oppg.opprettet """

            val limitAndOffsetClause =
                søkeFilter.paginering?.let {
                    """ LIMIT ${it.antallOppgaver} OFFSET ${it.side * it.antallOppgaver} """
                } ?: ""

            // TODO: sjekk på tilstand OPPRETTET bør erstattes med noe logikk for ikke-søkbare-tilstander

            // OBS: På grunn av at vi sammenligner "opprettet" (som er en timestamp) med fom- og tom-datoer (uten tidsdel),
            //     sjekker vi at "opprettet" er MINDRE enn tom-dato-pluss-1-dag.

            //language=PostgreSQL
            val oppgaveSelect =
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
                """.trimIndent()

            val antallSelect =
                """
                SELECT COUNT(*) as total_count
                """.trimIndent()
            val fromJoinAndWhereClause =
                StringBuilder(
                    """
                    FROM    oppgave_v1    oppg
                    JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                    JOIN    person_v1     pers ON pers.id = beha.person_id
                    WHERE   oppg.opprettet >= :fom
                    AND     oppg.opprettet <  :tom_pluss_1_dag
                    """.trimIndent(),
                )
                    .append(
                        tilstandClause,
                        saksbehandlerClause,
                        personIdentClause,
                        oppgaveIdClause,
                        behandlingIdClause,
                        emneknaggClause,
                    )
                    .toString()

            //language=PostgreSQL
            val oppgaverQuery =
                """
                $oppgaveSelect
                $fromJoinAndWhereClause
                $orderByOpprettetClause
                $limitAndOffsetClause   
                """.trimIndent()

            val antallOppgaverQuery =
                """
                $antallSelect
                $fromJoinAndWhereClause
                """.trimIndent()

            val paramap =
                mapOf(
                    "tilstander" to tilstander,
                    "fom" to søkeFilter.periode.fom,
                    "tom_pluss_1_dag" to søkeFilter.periode.tom.plusDays(1),
                    "saksbehandler_ident" to søkeFilter.saksbehandlerIdent,
                    "person_ident" to søkeFilter.personIdent,
                    "oppgave_id" to søkeFilter.oppgaveId,
                    "behandling_id" to søkeFilter.behandlingId,
                    "emneknagger" to emneknaggerAsText,
                )
            sikkerlogger.info { "Søker etter antall oppgaver med følgende SQL: $antallOppgaverQuery" }
            sikkerlogger.info { "Henter oppgaver med følgende SQL: $oppgaverQuery" }
            val antallOppgaver: Int =
                session.run(
                    queryOf(
                        statement = antallOppgaverQuery,
                        paramMap = paramap,
                    ).map { row -> row.int("total_count") }.asSingle,
                ) ?: throw DataNotFoundException("Query for å telle antall oppgaver feilet")

            val oppgaver =
                session.run(
                    queryOf(statement = oppgaverQuery, paramMap = paramap).map { row ->
                        row.rehydrerOppgave(datasource)
                    }.asList,
                )
            OppgaveSøkResultat(oppgaver = oppgaver, totaltAntallOppgaver = antallOppgaver)
        }
    }

    override fun oppdaterSkjermingStatus(
        fnr: String,
        skjermet: Boolean,
    ): Int {
        return sessionOf(datasource).use { session ->
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
        return sessionOf(datasource).use { session ->
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
        return sessionOf(datasource).use { session ->
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
        "SendTilKontrollHendelse" -> hendelseJson.tilHendelse<SendTilKontrollHendelse>()
        "ReturnerTilSaksbehandlingHendelse" -> hendelseJson.tilHendelse<ReturnerTilSaksbehandlingHendelse>()
        "BehandlingLåstHendelse" -> hendelseJson.tilHendelse<BehandlingLåstHendelse>()
        "BehandlingOpplåstHendelse" -> hendelseJson.tilHendelse<BehandlingOpplåstHendelse>()
        "SøknadsbehandlingOpprettetHendelse" -> hendelseJson.tilHendelse<SøknadsbehandlingOpprettetHendelse>()
        "UtsettOppgaveHendelse" -> hendelseJson.tilHendelse<UtsettOppgaveHendelse>()
        "VedtakFattetHendelse" -> hendelseJson.tilHendelse<VedtakFattetHendelse>()
        "NesteOppgaveHendelse" -> hendelseJson.tilHendelse<NesteOppgaveHendelse>()
        else -> {
            logger.error { "rehydrerTilstandsendringHendelse: Ukjent hendelse med type $hendelseType" }
            sikkerlogger.error { "rehydrerTilstandsendringHendelse: Ukjent hendelse med type $hendelseType: $hendelseJson" }
            throw IllegalArgumentException("Ukjent hendelse type $hendelseType")
        }
    }
}

private fun Row.rehydrerHendelse(): Hendelse {
    return when (val hendelseType = this.string("hendelse_type")) {
        "TomHendelse" -> return TomHendelse
        "SøknadsbehandlingOpprettetHendelse" -> SøknadsbehandlingOpprettetHendelse.fromJson(this.string("hendelse_data"))
        else -> {
            logger.error { "rehydrerHendelse: Ukjent hendelse med type $hendelseType" }
            sikkerlogger.error { "rehydrerHendelse: Ukjent hendelse med type $hendelseType: ${this.string("hendelse_data")}" }
            throw IllegalArgumentException("Ukjent hendelse type $hendelseType")
        }
    }
}

private fun finnHendelseForBehandling(
    behandlingId: UUID,
    dataSource: DataSource,
): Hendelse {
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

private fun hentEmneknaggerForOppgave(
    oppgaveId: UUID,
    dataSource: DataSource,
): Set<String> {
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

private fun finnNotat(
    oppgaveTilstandLoggId: UUID,
    dataSource: DataSource,
): Notat? {
    return sessionOf(dataSource).use { session ->
        session.run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    SELECT id,tekst,endret_tidspunkt, skrevet_av
                    FROM   notat_v1
                    WHERE  oppgave_tilstand_logg_id = :oppgaveTilstandLoggId
                    """.trimIndent(),
                paramMap = mapOf("oppgaveTilstandLoggId" to oppgaveTilstandLoggId),
            ).map { row ->
                Notat(
                    notatId = row.uuid("id"),
                    tekst = row.string("tekst"),
                    sistEndretTidspunkt = row.localDateTime("endret_tidspunkt"),
                    skrevetAv = row.string("skrevet_av"),
                )
            }.asSingle,
        )
    }
}

private fun hentTilstandsloggForOppgave(
    oppgaveId: UUID,
    dataSource: DataSource,
): Tilstandslogg {
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
                    "behandling_id" to oppgave.behandling.behandlingId,
                    "tilstand" to oppgave.tilstand().type.name,
                    "opprettet" to oppgave.opprettet,
                    "saksbehandler_ident" to oppgave.behandlerIdent,
                    "utsatt_til" to oppgave.utsattTil(),
                ),
        ).asUpdate,
    )
    lagre(oppgave.oppgaveId, oppgave.emneknagger)
    lagre(oppgave.oppgaveId, oppgave.tilstandslogg)
    oppgave.tilstand().notat()?.let {
        lagreNotat(
            notatId = it.notatId,
            tilstandsendringId = oppgave.tilstandslogg.first().id,
            tekst = it.hentTekst(),
            skrevetAv = it.skrevetAv,
        )
    }
}

private fun Session.lagreNotat(
    notatId: UUID,
    tilstandsendringId: UUID,
    tekst: String,
    skrevetAv: String,
): LocalDateTime {
    return run(
        queryOf(
            //language=PostgreSQL
            statement =
                """
                INSERT INTO notat_v1
                    (id, oppgave_tilstand_logg_id, tekst, skrevet_av) 
                VALUES
                    (:notat_id, :oppgave_tilstand_logg_id, :tekst, :skrevet_av) 
                ON CONFLICT (oppgave_tilstand_logg_id) DO UPDATE SET tekst = :tekst
                RETURNING endret_tidspunkt
                """.trimIndent(),
            paramMap =
                mapOf(
                    "notat_id" to notatId,
                    "oppgave_tilstand_logg_id" to tilstandsendringId,
                    "tekst" to tekst,
                    "skrevet_av" to skrevetAv,
                ),
        ).map { row -> row.localDateTime("endret_tidspunkt") }.asSingle,
    ) ?: throw KanIkkeLagreNotatException(
        "Kunne ikke lagre notat for tilstandsendringId: $tilstandsendringId",
    )
}

private fun TransactionalSession.lagre(
    oppgaveId: UUID,
    emneknagger: Set<String>,
) {
    run(
        queryOf(
            //language=PostgreSQL
            statement =
                """
                DELETE
                FROM  emneknagg_v1
                WHERE oppgave_id = :oppgave_id
                """.trimIndent(),
            paramMap = mapOf("oppgave_id" to oppgaveId),
        ).asUpdate,
    )
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

private fun Row.rehydrerOppgave(dataSource: DataSource): Oppgave {
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
            hendelse = finnHendelseForBehandling(behandlingId, dataSource),
        )

    val tilstandslogg = hentTilstandsloggForOppgave(oppgaveId, dataSource)

    val tilstand =
        kotlin
            .runCatching {
                when (Type.valueOf(value = string("tilstand"))) {
                    OPPRETTET -> Opprettet
                    KLAR_TIL_BEHANDLING -> KlarTilBehandling
                    UNDER_BEHANDLING -> UnderBehandling
                    PAA_VENT -> PåVent
                    AVVENTER_LÅS_AV_BEHANDLING -> AvventerLåsAvBehandling
                    KLAR_TIL_KONTROLL -> KlarTilKontroll
                    UNDER_KONTROLL -> UnderKontroll(finnNotat(tilstandslogg.first().id, dataSource))
                    AVVENTER_OPPLÅSING_AV_BEHANDLING -> AvventerOpplåsingAvBehandling
                    FERDIG_BEHANDLET -> FerdigBehandlet
                    BEHANDLES_I_ARENA -> BehandlesIArena
                }
            }.getOrElse { t ->
                throw UgyldigTilstandException("Kunne ikke rehydrere til tilstand: ${string("tilstand")} ${t.message}")
            }

    return Oppgave.rehydrer(
        oppgaveId = oppgaveId,
        behandlerIdent = this.stringOrNull("saksbehandler_ident"),
        opprettet = this.localDateTime("oppgave_opprettet"),
        emneknagger = hentEmneknaggerForOppgave(oppgaveId, dataSource),
        tilstand = tilstand,
        behandling = behandling,
        utsattTil = this.localDateOrNull("utsatt_til"),
        tilstandslogg = tilstandslogg,
    )
}

private fun Row.adresseBeskyttelseGradering(): AdressebeskyttelseGradering {
    return AdressebeskyttelseGradering.valueOf(this.string("adressebeskyttelse_gradering"))
}

class DataNotFoundException(message: String) : RuntimeException(message)

class KanIkkeLagreNotatException(message: String) : RuntimeException(message)
