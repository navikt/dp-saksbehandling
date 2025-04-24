package no.nav.dagpenger.saksbehandling.db.klage

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.klage.Verdi
import java.util.UUID
import javax.sql.DataSource

class PostgresKlageRepository(private val datasource: DataSource) : KlageRepository {
    override fun lagre(klageBehandling: KlageBehandling) {
        sessionOf(datasource).use { session ->
            session.transaction { tx ->
                tx.lagre(klageBehandling)
            }
        }
    }

    override fun hentKlageBehandling(behandlingId: UUID): KlageBehandling {
        return finnKlageBehandling(behandlingId) ?: throw DataNotFoundException("Fant ikke klage med id $behandlingId")
    }

    private fun finnKlageBehandling(behandlingId: UUID): KlageBehandling? {
        val klageBehandling =
            sessionOf(datasource).use { session ->
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            SELECT id, tilstand 
                            FROM   klage_v1 
                            WHERE  id = :id
                            """.trimIndent(),
                        paramMap = mapOf("id" to behandlingId),
                    ).map { row ->
                        row.rehydrerKlageBehandling(datasource)
                    }.asSingle,
                )
            }
        return klageBehandling
    }

    private fun Row.rehydrerKlageBehandling(dataSource: DataSource): KlageBehandling {
        val klageId = this.uuid("id")
        val opplysninger =
            hentKlageOpplysningerFor(
                klageId = klageId,
                dataSource = dataSource,
            )
        return KlageBehandling(
            behandlingId = klageId,
            opplysninger = opplysninger,
            steg = emptyList(),
        )
    }

    private fun hentKlageOpplysningerFor(
        klageId: UUID,
        dataSource: DataSource,
    ): Set<Opplysning> {
        // TODO fix type og verdi i Opplysning
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT id, type
                        FROM   klage_opplysning_v1
                        WHERE  klage_id = :klage_id
                        """.trimIndent(),
                    paramMap = mapOf("klage_id" to klageId),
                ).map { row ->
                    Opplysning(
                        id = row.uuid("id"),
                        type = OpplysningType.UTFALL,
                        verdi = Verdi.TomVerdi,
                    )
                }.asList,
            )
        }.toSet()
    }

    private fun TransactionalSession.lagre(klageBehandling: KlageBehandling) {
        run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO klage_v1
                        (id, tilstand)
                    VALUES
                        (:id, :tilstand) 
                    ON CONFLICT(id) DO UPDATE SET
                     tilstand = :tilstand
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to klageBehandling.behandlingId,
                        "tilstand" to "BEHANDLES",
                    ),
            ).asUpdate,
        )
        this.lagreOpplysninger(klageBehandling)
    }

    private fun TransactionalSession.lagreOpplysninger(klageBehandling: KlageBehandling) {
        val opplysninger: List<Map<String, Any>> =
            klageBehandling.alleOpplysninger().map {
                mapOf(
                    "id" to it.id,
                    "klage_id" to klageBehandling.behandlingId,
                    "type" to it.type.name,
//                    ,
//                    "verdi" to when (it.type.datatype) {
//                        Datatype.DATO -> (it.verdi as Verdi.Dato).toString()
//                        Datatype.TEKST -> (it.verdi as Verdi.TekstVerdi).toString()
//                        Datatype.FLERVALG -> (it.verdi as Verdi.Flervalg).toString()
//                        Datatype.BOOLSK -> (it.verdi as Verdi.Boolsk).toString()
//                        else -> throw IllegalArgumentException("Ukjent datatype ${it.type.datatype}")
//                    },
                )
            }
        this.batchPreparedNamedStatement(
            //language=PostgreSQL
            statement =
                """
                INSERT INTO klage_opplysning_v1
                    (id, klage_id, type
                    )
                VALUES
                    (:id, :klage_id, :type
                    )
                """.trimIndent(),
            params = opplysninger,
        )
    }
}
