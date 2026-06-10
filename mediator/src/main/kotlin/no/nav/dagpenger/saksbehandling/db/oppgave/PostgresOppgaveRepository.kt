package no.nav.dagpenger.saksbehandling.db.oppgave

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.queryOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.Notat
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Avbrutt
import no.nav.dagpenger.saksbehandling.Oppgave.AvbruttMaskinelt
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerLåsAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.AvventerOpplåsingAvBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.FerdigBehandlet
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilKontroll
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde
import no.nav.dagpenger.saksbehandling.Oppgave.Opprettet
import no.nav.dagpenger.saksbehandling.Oppgave.PåVent
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVBRUTT_MASKINELT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING
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
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.PostgresUnitOfWork
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode.Companion.UBEGRENSET_PERIODE
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.serder.rehydrerHendelse
import no.nav.dagpenger.saksbehandling.serder.tilJson
import org.postgresql.util.PGobject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class PostgresOppgaveRepository(
    private val databaseSession: DatabaseSession,
) : OppgaveRepository {
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
    ): UUID? =
        databaseSession.transaction {
            val tillatteGraderinger = filter.adressebeskyttelseTilganger.joinToString { "'$it'" }
            val utløstAvTyperAsText = filter.utløstAvTyper.joinToString { "'${it.name}'" }
            val tilstanderAsText = filter.tilstander.joinToString { "'$it'" }
            val ekskluderEmneknaggerAsText = filter.ekskluderEmneknagger.joinToString { "'$it'" }
            val utløstAvTypeClause =
                if (filter.utløstAvTyper.isNotEmpty()) {
                    " AND beha.utlost_av IN ($utløstAvTyperAsText) "
                } else {
                    ""
                }
            val tilstandClause =
                if (filter.tilstander.isNotEmpty()) {
                    " AND oppg.tilstand IN ($tilstanderAsText) "
                } else {
                    ""
                }

            // language=SQL
            val emneknaggClause =
                when {
                    filter.emneknaggGruppertPerKategori.isNotEmpty() -> {
                        // AND mellom kategorier, OR innenfor samme kategori
                        val categoryConditions =
                            filter.emneknaggGruppertPerKategori.map { (_, emneknagger) ->
                                val emneknaggList = emneknagger.joinToString { "'$it'" }
                                """
                                AND EXISTS(
                                    SELECT 1
                                    FROM   emneknagg_v1 emne
                                    WHERE  emne.oppgave_id = oppg.id
                                    AND    emne.emneknagg IN ($emneknaggList)
                                )
                                """.trimIndent()
                            }
                        categoryConditions.joinToString(" ")
                    }

                    else -> ""
                }

            // language=SQL
            val ekskluderEmneknaggerClause =
                when {
                    filter.ekskluderEmneknagger.isNotEmpty() -> {
                        """
                        AND NOT EXISTS(
                                SELECT 1
                                FROM   emneknagg_v1 emne
                                WHERE  emne.oppgave_id = oppg.id
                                AND    emne.emneknagg IN ($ekskluderEmneknaggerAsText)
                            )
                        """.trimIndent()
                    }
                    else -> ""
                }

            //language=PostgreSQL
            val harDpSakClause =
                if (filter.harDpSak) {
                    """
                    AND EXISTS(
                        SELECT 1
                        FROM        sak_v2 sak
                        LEFT JOIN   nodbremset_person_v1 nod on nod.person_id = sak.person_id
                        WHERE       sak.person_id = pers.id
                        AND         sak.er_dp_sak
                        AND         nod.person_id IS NULL
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
                    AND      pers.adressebeskyttelse_gradering IN ($tillatteGraderinger)
                    """ + utløstAvTypeClause + tilstandClause + emneknaggClause + ekskluderEmneknaggerClause + harDpSakClause

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
                        (   SELECT logg2.id
                            FROM   oppgave_tilstand_logg_v1 logg2
                            WHERE  logg2.oppgave_id = oppg.id
                            AND    logg2.tilstand = 'UNDER_BEHANDLING'
                            AND    logg2.hendelse_type IN ('NesteOppgaveHendelse','SettOppgaveAnsvarHendelse')
                            AND    logg2.tidspunkt = 
                            (   SELECT MAX(logg3.tidspunkt)
                                FROM   oppgave_tilstand_logg_v1 logg3
                                WHERE  logg3.oppgave_id = oppg.id
                                AND    logg3.tilstand = 'UNDER_BEHANDLING'
                                AND    logg3.hendelse_type IN ('NesteOppgaveHendelse','SettOppgaveAnsvarHendelse')
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
                """ + utløstAvTypeClause + tilstandClause + emneknaggClause + ekskluderEmneknaggerClause + harDpSakClause +
                    """
                    )
                    """.trimIndent()

            // language=SQL
            val selectAlleAktuelleOppgaverOrderByOpprettet =
                """
                    SELECT *
                    FROM   alle_oppgaver alop
                    ORDER BY alop.opprettet, alop.id
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

            sikkerlogger.info { "Henter oppgaver med SQL i tildelNesteOppgave: $statement - Filter = $filter" }

            val oppgaveIdOgTilstandType: Pair<UUID, Type>? =
                session.run(
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
                lagre(
                    it.first,
                    Tilstandsendring(
                        tilstand = it.second,
                        hendelse = nesteOppgaveHendelse,
                    ),
                )
            }
            oppgaveIdOgTilstandType?.first
        }

    override fun lagre(
        oppgave: Oppgave,
        ctx: Transaksjonskontekst,
    ) {
        databaseSession.inContext(ctx) {
            lagre(oppgave)
        }
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

    override fun hentOppgaveIdFor(behandlingId: UUID): UUID? =
        databaseSession.session { session ->
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

    override fun hentOppgaveFor(behandlingId: UUID): Oppgave =
        finnOppgaveFor(behandlingId) ?: throw DataNotFoundException("Fant ikke oppgave for behandlingId $behandlingId")

    override fun finnOppgaveFor(behandlingId: UUID): Oppgave? =
        søk(
            søkeFilter =
                Søkefilter(
                    periode = UBEGRENSET_PERIODE,
                    tilstander = Type.søkbareTilstander,
                    behandlingId = behandlingId,
                ),
        ).oppgaver.singleOrNull()

    override fun personSkjermesSomEgneAnsatte(oppgaveId: UUID): Boolean? =
        databaseSession.session { session ->
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

    override fun adresseGraderingForPerson(oppgaveId: UUID): AdressebeskyttelseGradering =
        databaseSession.session { session ->
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

    override fun finnNotat(oppgaveTilstandLoggId: UUID): Notat? = finnNotat(oppgaveTilstandLoggId, databaseSession)

    override fun lagreNotatFor(oppgave: Oppgave): LocalDateTime =
        when (val notat = oppgave.tilstand().notat()) {
            null -> throw IllegalStateException("Kan ikke lagre notat for oppgave uten notat")
            else -> {
                databaseSession.transaction {
                    lagreNotat(
                        notatId = notat.notatId,
                        tilstandsendringId = oppgave.tilstandslogg.first().id,
                        tekst = notat.hentTekst(),
                        skrevetAv = notat.skrevetAv,
                    )
                }
            }
        }

    override fun slettNotatFor(oppgave: Oppgave) {
        val tilstandsloggId = oppgave.tilstandslogg.first().id

        databaseSession.transaction {
            slettNotat(tilstandsloggId)
        }
    }

    override fun finnOppgaverPåVentMedUtgåttFrist(frist: LocalDate): List<UUID> =
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  id
                        FROM    oppgave_v1
                        WHERE   tilstand = :tilstand
                        AND     utsatt_til <= :frist
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "frist" to frist,
                            "tilstand" to PAA_VENT.name,
                        ),
                ).map { row ->
                    row.uuid("id")
                }.asList,
            )
        }

    override fun oppgaveTilstandForSøknad(
        ident: String,
        søknadId: UUID,
    ): Type? =
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                        SELECT  oppg.tilstand AS tilstand
                        FROM    oppgave_v1      oppg
                        JOIN    behandling_v1   beha ON beha.id = oppg.behandling_id
                        JOIN    person_v1       pers ON pers.id = beha.person_id
                        JOIN    hendelse_v1     hend ON beha.id = hend.behandling_id
                        WHERE   pers.ident = :ident
                        AND     hend.hendelse_type = 'SøknadsbehandlingOpprettetHendelse'
                        AND     hend.hendelse_data->>'søknadId' = :soknad_id
                    """.trimMargin(),
                    mapOf(
                        "ident" to ident,
                        "soknad_id" to søknadId.toString(),
                    ),
                ).map { row ->
                    Type.valueOf(row.string("tilstand"))
                }.asSingle,
            )
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

    override fun finnOppgaverFor(
        ident: String,
        antall: Int?,
    ): List<Oppgave> =
        søk(
            søkeFilter =
                Søkefilter(
                    periode = UBEGRENSET_PERIODE,
                    tilstander = Type.søkbareTilstander,
                    saksbehandlerIdent = null,
                    personIdent = ident,
                    paginering = antall?.let { Søkefilter.Paginering(antallOppgaver = it, side = 0) },
                    sortering = Søkefilter.Sortering.DESC,
                ),
        ).oppgaver

    data class OppgaveSøkResultat(
        val oppgaver: List<Oppgave>,
        val totaltAntallOppgaver: Int,
    )

    override fun søk(søkeFilter: Søkefilter): OppgaveSøkResultat =
        databaseSession.session { session ->
            val tilstanderAsText = søkeFilter.tilstander.joinToString { "'$it'" }
            val tilstandClause =
                when (søkeFilter.tilstander.isNotEmpty()) {
                    true -> " AND oppg.tilstand IN ($tilstanderAsText) "
                    false -> ""
                }
            val utløstAvTyperAsText: String = søkeFilter.utløstAvTyper.joinToString { "'${it.name}'" }
            val utløstAvTypeClause =
                when (søkeFilter.utløstAvTyper.isNotEmpty()) {
                    true -> " AND beha.utlost_av IN ($utløstAvTyperAsText) "
                    false -> ""
                }

            val saksbehandlerClause =
                søkeFilter.saksbehandlerIdent?.let { "AND oppg.saksbehandler_ident = :saksbehandler_ident " } ?: ""

            val personIdentClause = søkeFilter.personIdent?.let { "AND pers.ident = :person_ident " } ?: ""

            val oppgaveIdClause = søkeFilter.oppgaveId?.let { "AND oppg.id = :oppgave_id " } ?: ""

            val behandlingIdClause =
                søkeFilter.behandlingId?.let { "AND oppg.behandling_id = :behandling_id " } ?: ""

            val søknadIdClause =
                søkeFilter.søknadId?.let {
                    """
                    AND hend.hendelse_type = 'SøknadsbehandlingOpprettetHendelse' 
                    AND hend.hendelse_data ->> 'søknadId' = :soknad_id 
                    """.trimIndent()
                } ?: ""

            val emneknaggClause =
                when {
                    søkeFilter.emneknaggGruppertPerKategori.isNotEmpty() -> {
                        // Benytter AND mellom kategorier, OR innenfor samme kategori
                        // language=SQL
                        val categoryConditions =
                            søkeFilter.emneknaggGruppertPerKategori.map { (_, emneknagger) ->
                                val emneknaggList = emneknagger.joinToString { "'$it'" }
                                """
                                AND EXISTS(
                                    SELECT 1
                                    FROM   emneknagg_v1 emne
                                    WHERE  emne.oppgave_id = oppg.id
                                    AND    emne.emneknagg IN ($emneknaggList)
                                )
                                """.trimIndent()
                            }
                        categoryConditions.joinToString(" ")
                    }

                    else -> ""
                }

            val ekskluderEmneknaggerAsText = søkeFilter.ekskluderEmneknagger.joinToString { "'$it'" }
            // language=PostgreSQL
            val ekskluderEmneknaggerClause =
                when {
                    søkeFilter.ekskluderEmneknagger.isNotEmpty() -> {
                        """
                        AND NOT EXISTS(
                                SELECT 1
                                FROM   emneknagg_v1 emne
                                WHERE  emne.oppgave_id = oppg.id
                                AND    emne.emneknagg IN ($ekskluderEmneknaggerAsText)
                            )
                        """.trimIndent()
                    }
                    else -> ""
                }

            //language=PostgreSQL
            val harDpSakClause =
                if (søkeFilter.harDpSak) {
                    """
                    AND EXISTS(
                        SELECT 1
                        FROM        sak_v2 sak
                        LEFT JOIN   nodbremset_person_v1 nod ON nod.person_id = sak.person_id
                        WHERE       sak.person_id = pers.id
                        AND         sak.er_dp_sak
                        AND         nod.person_id IS NULL
                    )
                    """.trimIndent()
                } else {
                    ""
                }

            val orderByClause = søkeFilter.sorteringsfelt.orderByClause(søkeFilter.sortering)

            val limitAndOffsetClause =
                søkeFilter.paginering?.let {
                    """ LIMIT ${it.antallOppgaver} OFFSET ${it.side * it.antallOppgaver} """
                } ?: ""

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
                        oppg.melding_om_vedtak_kilde,
                        oppg.kontrollert_brev,
                        beha.opprettet AS behandling_opprettet,
                        beha.utlost_av,
                        hend.hendelse_type AS hendelse_type,
                        hend.hendelse_data AS hendelse_data
                """.trimIndent()

            val antallSelect =
                """
                SELECT COUNT(*) as total_count
                """.trimIndent()
            val fromJoinAndWhereClause =
                StringBuilder(
                    """
                    FROM      oppgave_v1    oppg
                    JOIN      behandling_v1 beha ON beha.id = oppg.behandling_id
                    JOIN      person_v1     pers ON pers.id = beha.person_id
                    LEFT JOIN hendelse_v1      hend ON hend.behandling_id = beha.id
                    WHERE     oppg.opprettet >= :fom
                    AND       oppg.opprettet <  :tom_pluss_1_dag
                    """.trimIndent(),
                ).append(
                    tilstandClause,
                    utløstAvTypeClause,
                    saksbehandlerClause,
                    personIdentClause,
                    oppgaveIdClause,
                    behandlingIdClause,
                    emneknaggClause,
                    ekskluderEmneknaggerClause,
                    harDpSakClause,
                    søknadIdClause,
                ).toString()

            //language=PostgreSQL
            val oppgaverQuery =
                """
                $oppgaveSelect
                $fromJoinAndWhereClause
                $orderByClause
                $limitAndOffsetClause   
                """.trimIndent()

            val antallOppgaverQuery =
                """
                $antallSelect
                $fromJoinAndWhereClause
                """.trimIndent()

            val paramMap =
                mapOf(
                    "fom" to søkeFilter.periode.fom,
                    "tom_pluss_1_dag" to søkeFilter.periode.tom.plusDays(1),
                    "saksbehandler_ident" to søkeFilter.saksbehandlerIdent,
                    "person_ident" to søkeFilter.personIdent,
                    "oppgave_id" to søkeFilter.oppgaveId,
                    "behandling_id" to søkeFilter.behandlingId,
                    "soknad_id" to søkeFilter.søknadId?.toString(),
                )
            sikkerlogger.info { "Søker etter antall oppgaver med følgende SQL: $antallOppgaverQuery" }
            sikkerlogger.info { "Henter oppgaver med følgende SQL: $oppgaverQuery" }
            val antallOppgaver: Int =
                session.run(
                    queryOf(
                        statement = antallOppgaverQuery,
                        paramMap = paramMap,
                    ).map { row -> row.int("total_count") }.asSingle,
                ) ?: throw DataNotFoundException("Query for å telle antall oppgaver feilet")

            val oppgaver =
                session.run(
                    queryOf(statement = oppgaverQuery, paramMap = paramMap)
                        .map { row ->
                            row.rehydrerOppgave(databaseSession)
                        }.asList,
                )
            OppgaveSøkResultat(oppgaver = oppgaver, totaltAntallOppgaver = antallOppgaver)
        }

    override fun hentDistinkteEmneknagger(): Set<String> =
        databaseSession.session { session ->
            session
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT DISTINCT emneknagg
                        FROM emneknagg_v1
                        """.trimIndent(),
                    ).map { row ->
                        row.string("emneknagg")
                    }.asList,
                ).toSet()
        }
}

private fun rehydrerTilstandsendringHendelse(
    hendelseType: String,
    hendelseJson: String,
): Hendelse = rehydrerHendelse(hendelseType, hendelseJson)

private fun hentEmneknaggerForOppgave(
    oppgaveId: UUID,
    databaseSession: DatabaseSession,
): Set<String> =
    databaseSession
        .session { session ->
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

private fun finnNotat(
    oppgaveTilstandLoggId: UUID,
    databaseSession: DatabaseSession,
): Notat? =
    databaseSession.session { session ->
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

private fun hentTilstandsloggForOppgave(
    oppgaveId: UUID,
    databaseSession: DatabaseSession,
): OppgaveTilstandslogg =
    databaseSession.session { session ->
        session
            .run(
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
                    Tilstandsendring<Type>(
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
            ).let { OppgaveTilstandslogg(it) }
    }

private fun PostgresUnitOfWork.lagre(oppgave: Oppgave) {
    session.run(
        queryOf(
            //language=PostgreSQL
            statement =
                """
                INSERT INTO oppgave_v1
                    (
                        id,
                        behandling_id,
                        tilstand,
                        opprettet,
                        saksbehandler_ident,
                        utsatt_til,
                        melding_om_vedtak_kilde,
                        kontrollert_brev
                    )
                VALUES
                    (
                        :id,
                        :behandling_id,
                        :tilstand,
                        :opprettet,
                        :saksbehandler_ident,
                        :utsatt_til,
                        :melding_om_vedtak_kilde,
                        :kontrollert_brev
                    ) 
                ON CONFLICT(id) DO UPDATE SET
                 tilstand = :tilstand,
                 saksbehandler_ident = :saksbehandler_ident,
                 utsatt_til = :utsatt_til,
                 melding_om_vedtak_kilde = :melding_om_vedtak_kilde,
                 kontrollert_brev = :kontrollert_brev
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to oppgave.oppgaveId,
                    "behandling_id" to oppgave.behandling.behandlingId,
                    "tilstand" to oppgave.tilstand().type.name,
                    "opprettet" to oppgave.opprettet,
                    "saksbehandler_ident" to oppgave.behandlerIdent,
                    "utsatt_til" to oppgave.utsattTil(),
                    "melding_om_vedtak_kilde" to oppgave.meldingOmVedtakKilde().name,
                    "kontrollert_brev" to oppgave.kontrollertBrev().name,
                ),
        ).asUpdate,
    )
    this.lagre(oppgave.oppgaveId, oppgave.emneknagger)
    this.lagre(oppgave.oppgaveId, oppgave.tilstandslogg)
    oppgave.tilstand().notat()?.let {
        lagreNotat(
            notatId = it.notatId,
            tilstandsendringId = oppgave.tilstandslogg.first().id,
            tekst = it.hentTekst(),
            skrevetAv = it.skrevetAv,
        )
    }
}

private fun PostgresUnitOfWork.lagreNotat(
    notatId: UUID,
    tilstandsendringId: UUID,
    tekst: String,
    skrevetAv: String,
): LocalDateTime =
    session.run(
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

private fun PostgresUnitOfWork.slettNotat(tilstandsloggId: UUID) {
    session.run(
        queryOf(
            //language=PostgreSQL
            statement = "DELETE FROM notat_v1 WHERE oppgave_tilstand_logg_id = :oppgave_tilstand_logg_id",
            paramMap = mapOf("oppgave_tilstand_logg_id" to tilstandsloggId),
        ).asUpdate,
    )
}

private fun PostgresUnitOfWork.lagre(
    oppgaveId: UUID,
    emneknagger: Set<String>,
) {
    session.run(
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
        session.run(
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

private fun PostgresUnitOfWork.lagre(
    oppgaveId: UUID,
    tilstandsendring: Tilstandsendring<Type>,
) {
    session.run(
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

private fun PostgresUnitOfWork.lagre(
    oppgaveId: UUID,
    tilstandslogg: OppgaveTilstandslogg,
) {
    tilstandslogg.forEach { tilstandsendring ->
        this.lagre(oppgaveId, tilstandsendring)
    }
}

private fun Row.rehydrerOppgave(databaseSession: DatabaseSession): Oppgave {
    val oppgaveId = this.uuid("oppgave_id")
    val person =
        Person(
            id = this.uuid("person_id"),
            ident = this.string("person_ident"),
            skjermesSomEgneAnsatte = this.boolean("skjermes_som_egne_ansatte"),
            adressebeskyttelseGradering = this.adresseBeskyttelseGradering(),
        )
    val tilstandslogg = hentTilstandsloggForOppgave(oppgaveId, databaseSession)

    val tilstand =
        runCatching {
            when (Type.valueOf(value = string("tilstand"))) {
                OPPRETTET -> Opprettet
                KLAR_TIL_BEHANDLING -> KlarTilBehandling
                UNDER_BEHANDLING -> UnderBehandling
                PAA_VENT -> PåVent
                AVVENTER_LÅS_AV_BEHANDLING -> AvventerLåsAvBehandling
                KLAR_TIL_KONTROLL -> KlarTilKontroll
                UNDER_KONTROLL -> UnderKontroll(finnNotat(tilstandslogg.first().id, databaseSession))
                AVVENTER_OPPLÅSING_AV_BEHANDLING -> AvventerOpplåsingAvBehandling
                FERDIG_BEHANDLET -> FerdigBehandlet
                AVBRUTT -> Avbrutt
                AVBRUTT_MASKINELT -> AvbruttMaskinelt
            }
        }.getOrElse { t ->
            throw UgyldigTilstandException("Kunne ikke rehydrere oppgave til tilstand: ${string("tilstand")} ${t.message}")
        }

    return Oppgave.rehydrer(
        oppgaveId = oppgaveId,
        behandlerIdent = this.stringOrNull("saksbehandler_ident"),
        opprettet = this.localDateTime("oppgave_opprettet"),
        emneknagger = hentEmneknaggerForOppgave(oppgaveId, databaseSession),
        tilstand = tilstand,
        utsattTil = this.localDateOrNull("utsatt_til"),
        tilstandslogg = tilstandslogg,
        behandling =
            Behandling.rehydrer(
                behandlingId = this.uuid("behandling_id"),
                opprettet = this.localDateTime("behandling_opprettet"),
                hendelse = this.rehydrerHendelse(),
                utløstAv = HendelseBehandler.valueOf(this.string("utlost_av")),
            ),
        person = person,
        meldingOmVedtak =
            Oppgave.MeldingOmVedtak(
                kilde = MeldingOmVedtakKilde.valueOf(this.string("melding_om_vedtak_kilde")),
                kontrollertGosysBrev = Oppgave.KontrollertBrev.valueOf(this.string("kontrollert_brev")),
            ),
    )
}

private fun Row.adresseBeskyttelseGradering(): AdressebeskyttelseGradering =
    AdressebeskyttelseGradering.valueOf(this.string("adressebeskyttelse_gradering"))

private fun Row.rehydrerHendelse(): Hendelse =
    rehydrerHendelse(
        hendelseType = this.stringOrNull("hendelse_type"),
        hendelseJson = this.stringOrNull("hendelse_data") ?: "{}",
    )

private fun Søkefilter.Sorteringsfelt.orderByClause(sortering: Søkefilter.Sortering): String =
    when (this) {
        Søkefilter.Sorteringsfelt.OPPRETTET ->
            """ ORDER BY oppg.opprettet ${sortering.name}, oppg.id ${sortering.name} """

        Søkefilter.Sorteringsfelt.UTLOST_AV ->
            """ ORDER BY beha.utlost_av ${sortering.name}, oppg.id ${sortering.name} """

        Søkefilter.Sorteringsfelt.STATUS ->
            """ ORDER BY oppg.tilstand ${sortering.name}, oppg.id ${sortering.name} """

        Søkefilter.Sorteringsfelt.SAKSBEHANDLER ->
            """ ORDER BY oppg.saksbehandler_ident ${sortering.name} NULLS LAST, oppg.id ${sortering.name} """
    }

class DataNotFoundException(
    message: String,
) : RuntimeException(message)

class KanIkkeLagreNotatException(
    message: String,
) : RuntimeException(message)
