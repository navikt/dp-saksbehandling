package no.nav.dagpenger.saksbehandling.db.klage

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.db.klage.KlageOpplysningerMapper.tilJson
import no.nav.dagpenger.saksbehandling.db.klage.KlageOpplysningerMapper.tilKlageOpplysninger
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import org.postgresql.util.PGobject
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
                            SELECT id, tilstand, journalpost_id, behandlende_enhet, opplysninger
                            FROM   klage_v1 
                            WHERE  id = :id
                            """.trimIndent(),
                        paramMap = mapOf("id" to behandlingId),
                    ).map { row ->
                        KlageBehandling(
                            behandlingId = row.uuid("id"),
                            tilstand = KlageBehandling.Type.valueOf(row.string("tilstand")),
                            journalpostId = row.stringOrNull("journalpost_id"),
                            behandlendeEnhet = row.stringOrNull("behandlende_enhet"),
                            opplysninger = row.string("opplysninger").tilKlageOpplysninger(),
                        )
                    }.asSingle,
                )
            }
        return klageBehandling
    }

    private fun TransactionalSession.lagre(klageBehandling: KlageBehandling) {
        run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    INSERT INTO klage_v1
                        (id, tilstand, journalpost_id, behandlende_enhet, opplysninger)
                    VALUES
                        (:id, :tilstand, :journalpost_id, :behandlende_enhet, :opplysninger)
                    ON CONFLICT(id) DO UPDATE SET
                     tilstand = :tilstand,
                     journalpost_id = :journalpost_id,
                     behandlende_enhet = :behandlende_enhet,
                     opplysninger = :opplysninger
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to klageBehandling.behandlingId,
                        "tilstand" to klageBehandling.tilstand().toString(),
                        "journalpost_id" to klageBehandling.journalpostId(),
                        "behandlende_enhet" to klageBehandling.behandlendeEnhet(),
                        "opplysninger" to
                            PGobject().also {
                                it.type = "JSONB"
                                it.value = klageBehandling.alleOpplysninger().tilJson()
                            },
                    ),
            ).asUpdate,
        )
    }
}
