package no.nav.dagpenger.saksbehandling.utsending.db

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerArkiverbarVersjonAvBrev
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerDistribuering
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerJournalføring
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Distribuert
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Opprettet
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.VenterPåVedtak
import java.util.UUID
import javax.sql.DataSource

class PostgresUtsendingRepository(private val ds: DataSource) : UtsendingRepository {
    override fun lagre(utsending: Utsending) {
        sessionOf(ds).use { session ->
            session.transaction { tx ->
                utsending.sak()?.let { tx.lagreSak(it) }
                tx.run(
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            INSERT INTO utsending_v1
                                (id, oppgave_id, tilstand, brev, pdf_urn, journalpost_id, distribusjon_id, sak_id) 
                            VALUES
                                (:id, :oppgave_id, :tilstand, :brev, :pdf_urn, :journalpost_id, :distribusjon_id, :sak_id) 
                            ON CONFLICT (id) DO UPDATE SET 
                                tilstand = :tilstand,
                                brev = :brev,
                                pdf_urn = :pdf_urn,
                                journalpost_id = :journalpost_id,
                                distribusjon_id = :distribusjon_id,
                                sak_id = :sak_id
                            """.trimIndent(),
                        paramMap =
                            mapOf(
                                "id" to utsending.id,
                                "oppgave_id" to utsending.oppgaveId,
                                "tilstand" to utsending.tilstand().type.name,
                                "brev" to utsending.brev(),
                                "pdf_urn" to utsending.pdfUrn()?.toString(),
                                "journalpost_id" to utsending.journalpostId(),
                                "distribusjon_id" to utsending.distribusjonId(),
                                "sak_id" to utsending.sak()?.id,
                            ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun hent(oppgaveId: UUID): Utsending {
        return finnUtsendingFor(oppgaveId) ?: throw UtsendingIkkeFunnet("Fant ikke utsending for oppgaveId: $oppgaveId")
    }

    override fun finnUtsendingFor(oppgaveId: UUID): Utsending? {
        sessionOf(ds).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  uts.id as utsending_id, 
                                uts.oppgave_id, 
                                uts.tilstand, 
                                uts.brev, 
                                uts.pdf_urn, 
                                uts.journalpost_id,
                                uts.distribusjon_id,
                                sak.id as sak_id, 
                                sak.kontekst
                        FROM utsending_v1 uts
                        LEFT JOIN sak_v1 sak on uts.sak_id = sak.id
                        WHERE uts.oppgave_id = :oppgave_id
                        """.trimIndent(),
                    paramMap = mapOf("oppgave_id" to oppgaveId),
                ).map { row ->
                    val tilstand =
                        when (Tilstand.Type.valueOf(row.string("tilstand"))) {
                            Opprettet -> Utsending.Opprettet
                            VenterPåVedtak -> Utsending.VenterPåVedtak
                            AvventerArkiverbarVersjonAvBrev -> Utsending.AvventerArkiverbarVersjonAvBrev
                            AvventerJournalføring -> Utsending.AvventerJournalføring
                            AvventerDistribuering -> Utsending.AvventerDistribuering
                            Distribuert -> Utsending.Distribuert
                        }
                    val sak: Sak? =
                        row.stringOrNull("sak_id")?.let { Sak(row.string("sak_id"), row.string("kontekst")) }

                    Utsending.rehydrer(
                        id = row.uuid("utsending_id"),
                        oppgaveId = row.uuid("oppgave_id"),
                        tilstand = tilstand,
                        brev = row.stringOrNull("brev"),
                        pdfUrn = row.stringOrNull("pdf_urn"),
                        journalpostId = row.stringOrNull("journalpost_id"),
                        distribusjonId = row.stringOrNull("distribusjon_id"),
                        sak = sak,
                    )
                }.asSingle,
            )
        }
    }
}

private fun Session.lagreSak(sak: Sak) {
    this.run(
        queryOf(
            //language=PostgreSQL
            statement =
                """
                INSERT INTO sak_v1
                    (id, kontekst) 
                VALUES
                    (:id, :kontekst) 
                ON CONFLICT (id) DO NOTHING 
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to sak.id,
                    "kontekst" to sak.kontekst,
                ),
        ).asUpdate,
    )
}
