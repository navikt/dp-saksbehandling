package no.nav.dagpenger.saksbehandling.db.klage

import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import java.util.UUID
import javax.sql.DataSource

class KlagePostgresRepository(private val ds: DataSource) : KlageRepository {
    override fun hentKlageBehandling(behandlingId: UUID): KlageBehandling {
        TODO("Not yet implemented")
    }

    override fun lagre(klageBehandling: KlageBehandling) {
        val hubba: List<Map<String, Any>> =
            klageBehandling.alleOpplysninger().map {
                mapOf(
                    "id" to it.id,
                    "behandling_id" to klageBehandling.behandlingId,
                )
            }

        sessionOf(ds).use { session ->
            session.transaction { tx ->
                tx.batchPreparedNamedStatement(
                    statement =
                        """
                        INSERT INTO klage_opplysning_v1 (id, behandling_id )
                        VALUES (:id, :behandling_id)
                        """.trimIndent(),
                    params = hubba,
                )
            }
        }
    }
}
