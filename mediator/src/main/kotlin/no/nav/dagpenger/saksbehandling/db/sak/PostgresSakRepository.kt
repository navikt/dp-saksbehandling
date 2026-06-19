package no.nav.dagpenger.saksbehandling.db.sak

import kotliquery.Row
import kotliquery.queryOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.PostgresUnitOfWork
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.serder.tilJson
import org.postgresql.util.PGobject
import java.util.UUID
import kotlin.collections.forEach

class PostgresSakRepository(
    private val databaseSession: DatabaseSession,
) : SakRepository {
    override fun lagre(
        sakHistorikk: SakHistorikk,
        ctx: Transaksjonskontekst,
    ) {
        databaseSession.inContext(ctx) {
            lagreSakHistorikk(
                personId = sakHistorikk.person.id,
                saker = sakHistorikk.alleSaker(),
            )
        }
    }

    override fun hentSakHistorikk(ident: String): SakHistorikk =
        finnSakHistorikk(ident) ?: throw DataNotFoundException("Kan ikke finne sakHistorikk for ident $ident")

    override fun finnSakHistorikk(ident: String): SakHistorikk? {
        val sakHistorikk = mutableListOf<SakHistorikk>()

        return databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT 
                            per.id AS person_id,
                            per.ident AS person_ident,
                            per.adressebeskyttelse_gradering AS person_adressebeskyttelse_gradering,
                            per.skjermes_som_egne_ansatte AS person_skjermes_som_egne_ansatte,
                            sak.id AS sak_id,
                            sak.opprettet AS sak_opprettet,
                            beh.id AS behandling_id,
                            beh.utlost_av AS utlost_av,
                            beh.opprettet AS behandling_opprettet,
                            opp.id AS oppgave_id,
                            hen.hendelse_type AS hendelse_type,
                            hen.hendelse_data AS hendelse_data
                        FROM person_v1 per
                        LEFT JOIN sak_v2 sak ON sak.person_id = per.id
                        LEFT JOIN behandling_v1 beh ON beh.sak_id = sak.id
                        LEFT JOIN oppgave_v1 opp ON opp.behandling_id = beh.id
                        LEFT JOIN hendelse_v1 hen ON hen.behandling_id = beh.id
                        WHERE per.ident = :ident
                        ORDER BY sak.id DESC, beh.id DESC
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                        ),
                ).map { row ->
                    row.tilSakHistorikk(sakHistorikk)
                }.asSingle,
            )
        }
    }

    override fun finnSisteDagpengeSakId(ident: String): UUID? =
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT      sak.id
                        FROM        sak_v2 sak
                        JOIN        person_v1 per               ON sak.person_id = per.id
                        JOIN        behandling_v1 beh           ON beh.sak_id    = sak.id
                        LEFT JOIN   nodbremset_person_v1 nod    ON nod.person_id = per.id
                        WHERE       per.ident = :ident
                        AND         nod.person_id IS NULL
                        AND         sak.er_dp_sak
                        AND NOT EXISTS (
                            SELECT 1
                            FROM   behandling_v1 feri
                            WHERE  feri.sak_id = sak.id
                            AND    feri.utlost_av = :ferietillegg
                        )
                        AND NOT EXISTS (
                            SELECT 1
                            FROM   oppgave_v1 opp
                            WHERE  opp.behandling_id = beh.id
                            AND    opp.tilstand = 'AVBRUTT'
                        )
                        ORDER BY beh.id DESC
                        LIMIT 1
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                            "ferietillegg" to HendelseBehandler.DpBehandling.Ferietillegg.name,
                        ),
                ).map { row ->
                    row.uuid("id")
                }.asSingle,
            )
        }

    override fun finnSakIdForSøknad(
        søknadId: UUID,
        ident: String,
    ): UUID? =
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT   sak.id
                        FROM     sak_v2        sak
                        JOIN     person_v1     per ON sak.person_id     = per.id
                        JOIN     behandling_v1 beh ON beh.sak_id        = sak.id
                        JOIN     hendelse_v1   hen ON hen.behandling_id = beh.id
                        WHERE    per.ident         = :ident
                        AND      hen.hendelse_type = 'SøknadsbehandlingOpprettetHendelse'
                        AND      hen.hendelse_data->>'søknadId' = :soknad_id
                        AND      sak.er_dp_sak
                        ORDER BY beh.id DESC
                        LIMIT 1
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                            "soknad_id" to søknadId.toString(),
                        ),
                ).map { row ->
                    row.uuid("id")
                }.asSingle,
            )
        }

    override fun hentSakIdForBehandlingId(behandlingId: UUID): UUID =
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  sak.id
                        FROM    sak_v2 sak
                        JOIN    behandling_v1 beh ON beh.sak_id = sak.id
                        WHERE   beh.id = :behandling_id
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "behandling_id" to behandlingId,
                        ),
                ).map { row ->
                    row.uuid("id")
                }.asSingle,
            ) ?: throw DataNotFoundException("Kan ikke finne sak for behandlingId $behandlingId")
        }

    override fun hentDagpengerSakIdForBehandlingId(behandlingId: UUID): UUID =
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  sak.id
                        FROM    sak_v2 sak
                        JOIN    behandling_v1 beh ON beh.sak_id = sak.id
                        WHERE   beh.id = :behandling_id
                        AND     sak.er_dp_sak = true
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "behandling_id" to behandlingId,
                        ),
                ).map { row ->
                    row.uuid("id")
                }.asSingle,
            ) ?: throw DataNotFoundException("Kan ikke finne dagpenger sak for behandlingId $behandlingId")
        }

    private fun PostgresUnitOfWork.lagreSakHistorikk(
        personId: UUID,
        saker: List<Sak>,
    ) {
        saker.forEach { sak -> this.lagreSak(personId, sak) }
    }

    private fun PostgresUnitOfWork.lagreSak(
        personId: UUID,
        sak: Sak,
    ) {
        session.run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO sak_v2
                        ( id
                        , person_id
                        , opprettet
                        , er_dp_sak
                        ) 
                    VALUES
                        ( :id
                        , :person_id
                        , :opprettet
                        , CASE
                            WHEN :er_ferietillegg_sak OR EXISTS (
                                SELECT  1
                                FROM    sak_v2 sak
                                LEFT JOIN nodbremset_person_v1 nod ON nod.person_id = sak.person_id
                                WHERE   sak.person_id = :person_id
                                AND     sak.er_dp_sak = true
                                AND     nod.person_id IS NULL
                            )
                                THEN true
                                ELSE false
                            END
                        ) 
                    ON CONFLICT (id) DO NOTHING 
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to sak.sakId,
                        "person_id" to personId,
                        "opprettet" to sak.opprettet,
                        "er_ferietillegg_sak" to sak.erFerietilleggSak(),
                    ),
            ).asUpdate,
        )
        this.lagreBehandlinger(
            sakId = sak.sakId,
            personId = personId,
            behandlinger = sak.behandlinger(),
        )
    }

    private fun PostgresUnitOfWork.lagreBehandlinger(
        personId: UUID,
        sakId: UUID,
        behandlinger: List<Behandling>,
    ) {
        behandlinger.forEach { behandling ->
            this.lagreBehandling(
                sakId = sakId,
                personId = personId,
                behandling = behandling,
            )
        }
    }

    private fun PostgresUnitOfWork.lagreHendelse(
        behandlingId: UUID,
        hendelse: Hendelse,
    ) {
        session.run(
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

    override fun lagreBehandling(
        personId: UUID,
        sakId: UUID?,
        behandling: Behandling,
        ctx: Transaksjonskontekst,
    ) {
        databaseSession.inContext(ctx) {
            lagreBehandling(
                personId = personId,
                sakId = sakId,
                behandling = behandling,
            )
        }
    }

    override fun settArenaSakId(
        sakId: UUID,
        arenaSakId: String,
    ) {
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        UPDATE sak_v2
                        SET arena_sak_id = :arena_sak_id
                        WHERE id = :sak_id
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "sak_id" to sakId,
                            "arena_sak_id" to arenaSakId,
                        ),
                ).asUpdate,
            )
        }
    }

    override fun merkSakenSomDpSak(
        sakId: UUID,
        erDpSak: Boolean,
    ) {
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        UPDATE sak_v2
                        SET    er_dp_sak  = :er_dp_sak
                        WHERE  id = :sak_id
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "sak_id" to sakId,
                            "er_dp_sak" to erDpSak,
                        ),
                ).asUpdate,
            )
        }
    }

    private fun PostgresUnitOfWork.lagreBehandling(
        personId: UUID,
        sakId: UUID?,
        behandling: Behandling,
    ) {
        session.run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO behandling_v1
                        (id, person_id, utlost_av, sak_id, opprettet) 
                    VALUES
                        (:id, :person_id, :utlost_av, :sak_id, :opprettet) 
                    ON CONFLICT (id) DO NOTHING 
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to behandling.behandlingId,
                        "person_id" to personId,
                        "utlost_av" to behandling.utløstAv.name,
                        "sak_id" to sakId,
                        "opprettet" to behandling.opprettet,
                    ),
            ).asUpdate,
        )
        this.lagreHendelse(behandling.behandlingId, behandling.hendelse)
    }

    /**
     * Mapper en [Row] fra SQL-spørringen til en [SakHistorikk].
     * Hvis det ikke finnes en [SakHistorikk] for personen, opprettes en ny.
     * Hvis det finnes en sak, legges den til i [SakHistorikk].
     * Hvis det finnes en behandling, legges den til i [Sak].
     */
    private fun Row.tilSakHistorikk(sakHistorikkListe: MutableList<SakHistorikk>): SakHistorikk {
        val sakHistorikk =
            sakHistorikkListe.singleOrNull() ?: SakHistorikk(
                person =
                    Person(
                        id = this.uuid("person_id"),
                        ident = this.string("person_ident"),
                        skjermesSomEgneAnsatte = this.boolean("person_skjermes_som_egne_ansatte"),
                        adressebeskyttelseGradering =
                            AdressebeskyttelseGradering.valueOf(this.string("person_adressebeskyttelse_gradering")),
                    ),
            ).also { sakHistorikkListe.add(it) }

        // Map Sak
        val sakId = this.uuidOrNull("sak_id")
        if (sakId != null) {
            val sak =
                sakHistorikk.alleSaker().singleOrNull { it.sakId == sakId } ?: Sak(
                    sakId = sakId,
                    opprettet = this.localDateTime("sak_opprettet"),
                ).also {
                    sakHistorikk.leggTilSak(it)
                }

            // Map Behandling
            val behandlingId = this.uuidOrNull("behandling_id")
            if (behandlingId != null) {
                sak.leggTilBehandling(
                    Behandling(
                        behandlingId = behandlingId,
                        utløstAv = HendelseBehandler.valueOf(this.string("utlost_av")),
                        opprettet = this.localDateTime("behandling_opprettet"),
                        oppgaveId = this.uuidOrNull("oppgave_id"),
                        hendelse = this.rehydrerHendelse(),
                    ),
                )
            }
        }
        return sakHistorikk
    }

    private fun Row.rehydrerHendelse(): Hendelse {
        val hendelseType = this.string("hendelse_type")
        val hendelseData = this.string("hendelse_data")
        return no.nav.dagpenger.saksbehandling.serder
            .rehydrerHendelse(hendelseType, hendelseData)
    }
}
