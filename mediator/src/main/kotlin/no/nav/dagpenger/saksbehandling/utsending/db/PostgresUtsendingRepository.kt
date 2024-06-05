package no.nav.dagpenger.saksbehandling.utsending.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.db.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerArkiverbarVersjonAvBrev
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerDistribuering
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerJournalføring
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerMidlertidigJournalføring
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Distribuert
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Opprettet
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.VenterPåVedtak
import java.util.UUID
import javax.sql.DataSource

class PostgresUtsendingRepository(private val ds: DataSource) : UtsendingRepository {
    private val oppgaveRepository = PostgresOppgaveRepository(ds)

    override fun lagre(utsending: Utsending) {
        sessionOf(ds).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO utsending_v1
                            (id, oppgave_id, tilstand, brev, pdf_urn, journalpost_id) 
                        VALUES
                            (:id, :oppgave_id, :tilstand, :brev, :pdf_urn, :journalpost_id) 
                        ON CONFLICT (id) DO UPDATE SET 
                            tilstand = :tilstand,
                            brev = :brev,
                            pdf_urn = :pdf_urn,
                            journalpost_id = :journalpost_id
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "id" to utsending.id,
                            "oppgave_id" to utsending.oppgaveId,
                            "tilstand" to utsending.tilstand().type.name,
                            "brev" to utsending.brev(),
                            "pdf_urn" to utsending.pdfUrn(),
                            "journalpost_id" to utsending.journalpostId(),
                        ),
                ).asUpdate,
            )
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
                        SELECT id, oppgave_id, tilstand, brev, pdf_urn, journalpost_id
                        FROM utsending_v1
                        WHERE oppgave_id = :oppgave_id
                        """.trimIndent(),
                    paramMap = mapOf("oppgave_id" to oppgaveId),
                ).map { row ->
                    val tilstand =
                        when (Tilstand.Type.valueOf(row.string("tilstand"))) {
                            Opprettet -> Utsending.Opprettet
                            VenterPåVedtak -> Utsending.VenterPåVedtak
                            AvventerArkiverbarVersjonAvBrev -> Utsending.AvventerArkiverbarVersjonAvBrev
                            AvventerMidlertidigJournalføring -> Utsending.AvventerMidlertidigJournalføring
                            AvventerJournalføring -> Utsending.AvventerJournalføring
                            AvventerDistribuering -> Utsending.AvventerDistribuering
                            Distribuert -> Utsending.Distribuert
                        }

                    Utsending.rehydrer(
                        id = row.uuid("id"),
                        oppgaveId = row.uuid("oppgave_id"),
                        tilstand = tilstand,
                        brev = row.stringOrNull("brev"),
                        pdfUrn = row.stringOrNull("pdf_urn"),
                        journalpostId = row.stringOrNull("journalpost_id"),
                    )
                }.asSingle,
            )
        }
    }

    override fun hentUtsendingFor(behandlingId: UUID): Utsending {
        return oppgaveRepository.finnOppgaveFor(behandlingId)?.let { oppgave ->
            finnUtsendingFor(oppgave.oppgaveId)
        } ?: throw UtsendingIkkeFunnet("Fant ikke utsending for behandlingId: $behandlingId")
    }
}
