package no.nav.dagpenger.saksbehandling.db.sak

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.NyBehandling
import no.nav.dagpenger.saksbehandling.NySak
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import java.util.UUID
import javax.sql.DataSource

interface SakRepository {
    fun lagre(sakHistorikk: SakHistorikk)

    fun hentSakHistorikk(ident: String): SakHistorikk

    fun finn(ident: String): SakHistorikk?

    fun hentSakIdForBehandlingId(behandlingId: UUID): UUID
}

class PostgresRepository(
    private val dataSource: DataSource,
) : SakRepository {
    override fun lagre(sakHistorikk: SakHistorikk) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagreSakHistorikk(
                    personId = sakHistorikk.person.id,
                    saker = sakHistorikk.saker(),
                )
            }
        }
    }

    override fun hentSakHistorikk(ident: String): SakHistorikk {
        return finn(ident) ?: throw DataNotFoundException("Kan ikke finne sakHistorikk for ident $ident")
    }

    override fun finn(ident: String): SakHistorikk? {
        val person = mutableListOf<SakHistorikk>()

        sessionOf(dataSource).use { session ->
            return session.run(
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
                            sak.soknad_id AS sak_soknad_id,
                            sak.opprettet AS sak_opprettet,
                            beh.id AS behandling_id,
                            beh.behandling_type AS behandling_type,
                            beh.opprettet AS behandling_opprettet,
                            opp.id AS oppgave_id
                        FROM person_v1 per
                        LEFT JOIN sak_v2 sak ON sak.person_id = per.id
                        LEFT JOIN behandling_v1 beh ON beh.sak_id = sak.id
                        LEFT JOIN oppgave_v1 opp ON opp.behandling_id = beh.id
                        WHERE per.ident = :ident
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                        ),
                ).map { row ->
                    row.tilPerson(person)
                }.asSingle,
            )
        }
    }

    override fun hentSakIdForBehandlingId(behandlingId: UUID): UUID {
        return sessionOf(dataSource).use { session ->
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
    }

    private fun TransactionalSession.lagreSakHistorikk(
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
                        "id" to sak.sakId,
                        "person_id" to personId,
                        "soknad_id" to sak.søknadId,
                        "opprettet" to sak.opprettet,
                    ),
            ).asUpdate,
        )
        this.lagreBehandlinger(
            sakId = sak.sakId,
            personId = personId,
            behandlinger = sak.behandlinger(),
        )
    }

    private fun TransactionalSession.lagreBehandlinger(
        personId: UUID,
        sakId: UUID,
        behandlinger: List<NyBehandling>,
    ) {
        behandlinger.forEach { behandling ->
            this.lagreBehandling(
                sakId = sakId,
                personId = personId,
                behandling = behandling,
            )
        }
    }

    private fun TransactionalSession.lagreBehandling(
        personId: UUID,
        sakId: UUID,
        behandling: NyBehandling,
    ) {
        run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO behandling_v1
                        (id, person_id, behandling_type, sak_id, opprettet) 
                    VALUES
                        (:id, :person_id, :behandling_type, :sak_id, :opprettet) 
                    ON CONFLICT (id) DO NOTHING 
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to behandling.behandlingId,
                        "person_id" to personId,
                        "behandling_type" to behandling.behandlingType.name,
                        "sak_id" to sakId,
                        "opprettet" to behandling.opprettet,
                    ),
            ).asUpdate,
        )
    }

    private fun Row.tilPerson(sakHistorikkListe: MutableList<SakHistorikk>): SakHistorikk {
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
                sakHistorikk.saker().singleOrNull { it.sakId == sakId } ?: NySak(
                    sakId = sakId,
                    søknadId = this.uuid("sak_soknad_id"),
                    opprettet = this.localDateTime("sak_opprettet"),
                ).also {
                    sakHistorikk.leggTilSak(it)
                }

            // Map Behandling
            val behandlingId = this.uuidOrNull("behandling_id")
            if (behandlingId != null) {
                sak.leggTilBehandling(
                    NyBehandling(
                        behandlingId = behandlingId,
                        behandlingType = BehandlingType.valueOf(this.string("behandling_type")),
                        opprettet = this.localDateTime("behandling_opprettet"),
                        oppgaveId = this.uuidOrNull("oppgave_id"),
                    ),
                )
            }
        }
        return sakHistorikk
    }
}
