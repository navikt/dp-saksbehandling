package no.nav.dagpenger.saksbehandling.db.klage

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.db.klage.KlageOpplysningMapper.tilJson
import no.nav.dagpenger.saksbehandling.db.klage.KlageOpplysningMapper.tilKlageOpplysninger
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.serder.objectMapper
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
                            SELECT id, tilstand, opplysninger
                            FROM   klage_v1 
                            WHERE  id = :id
                            """.trimIndent(),
                        paramMap = mapOf("id" to behandlingId),
                    ).map { row ->
                        KlageBehandling(
                            behandlingId = row.uuid("id"),
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
                        (id, tilstand, opplysninger)
                    VALUES
                        (:id, :tilstand, :opplysninger) 
                    ON CONFLICT(id) DO UPDATE SET
                     tilstand = :tilstand,
                     opplysninger = :opplysninger
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to klageBehandling.behandlingId,
                        "tilstand" to "BEHANDLES",
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

private object KlageOpplysningMapper {
    fun Set<Opplysning>.tilJson(): String {
        return objectMapper.writeValueAsString(this).also {
            println(it)
        }
    }

    fun String.tilKlageOpplysninger(): Set<Opplysning> {
        return objectMapper.readValue(this, Set::class.java).map { opplysning ->
            objectMapper.convertValue(opplysning, Opplysning::class.java)
        }.toSet()
    }
}
