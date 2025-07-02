package no.nav.dagpenger.saksbehandling.utsending.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Avbrutt
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerArkiverbarVersjonAvBrev
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerDistribuering
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerJournalføring
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Distribuert
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.VenterPåVedtak
import no.nav.dagpenger.saksbehandling.utsending.UtsendingType
import java.util.UUID
import javax.sql.DataSource

class PostgresUtsendingRepository(private val ds: DataSource) : UtsendingRepository {
    override fun lagre(utsending: Utsending) {
        sessionOf(ds).use { session ->
            session.transaction { tx ->
                utsending.sak()?.let { tx.lagreUtsendingSak(it) }
                tx.run(
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            INSERT INTO utsending_v1
                                (id, oppgave_id, tilstand, brev, pdf_urn, journalpost_id, distribusjon_id, utsending_sak_id, type) 
                            VALUES
                                (:id, :oppgave_id, :tilstand, :brev, :pdf_urn, :journalpost_id, :distribusjon_id, :utsending_sak_id, :type) 
                            ON CONFLICT (id) DO UPDATE SET 
                                tilstand = :tilstand,
                                brev = :brev,
                                pdf_urn = :pdf_urn,
                                journalpost_id = :journalpost_id,
                                distribusjon_id = :distribusjon_id,
                                utsending_sak_id = :utsending_sak_id,
                                type = :type
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
                                "utsending_sak_id" to utsending.sak()?.id,
                                "type" to utsending.type.name,
                            ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun hent(oppgaveId: UUID): Utsending {
        return finnUtsendingFor(oppgaveId) ?: throw UtsendingIkkeFunnet("Fant ikke utsending for oppgaveId: $oppgaveId")
    }

    override fun utsendingFinnesForBehandling(behandlingId: UUID): Boolean {
        return sessionOf(ds).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT 1
                        FROM utsending_v1 uts
                        JOIN oppgave_v1 opp ON uts.oppgave_id = opp.id
                        WHERE opp.behandling_id = :behandling_id
                        """.trimIndent(),
                    paramMap = mapOf("behandling_id" to behandlingId),
                ).map { row -> row.intOrNull(1) }.asSingle,
            ) != null
        }
    }

    override fun slettUtsending(utsendingId: UUID): Int {
        return sessionOf(ds).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        DELETE FROM utsending_v1
                        WHERE id = :id
                        """.trimIndent(),
                    paramMap = mapOf("id" to utsendingId),
                ).asUpdate,
            )
        }
    }

    override fun utsendingFinnesForOppgave(oppgaveId: UUID): Boolean {
        return sessionOf(ds).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT 1
                        FROM utsending_v1
                        WHERE oppgave_id = :oppgave_id
                        """.trimIndent(),
                    paramMap = mapOf("oppgave_id" to oppgaveId),
                ).map { row -> row.intOrNull(1) }.asSingle,
            ) != null
        }
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
                                uts.type,
                                usak.id as sak_id, 
                                usak.kontekst,
                                per.ident
                        FROM utsending_v1 uts
                        JOIN oppgave_v1 opp on uts.oppgave_id = opp.id
                        JOIN behandling_v1 beh on opp.behandling_id = beh.id
                        JOIN person_v1 per on beh.person_id = per.id
                        LEFT JOIN utsending_sak_v1 usak on uts.utsending_sak_id = usak.id
                        WHERE uts.oppgave_id = :oppgave_id
                        """.trimIndent(),
                    paramMap = mapOf("oppgave_id" to oppgaveId),
                ).map { row ->
                    val tilstand = row.rehydrerUtsendingTilstand("tilstand")
                    val utsendingSak: UtsendingSak? =
                        row.stringOrNull("sak_id")?.let { UtsendingSak(row.string("sak_id"), row.string("kontekst")) }

                    Utsending.rehydrer(
                        id = row.uuid("utsending_id"),
                        oppgaveId = row.uuid("oppgave_id"),
                        ident = row.string("ident"),
                        tilstand = tilstand,
                        brev = row.string("brev"),
                        pdfUrn = row.stringOrNull("pdf_urn"),
                        journalpostId = row.stringOrNull("journalpost_id"),
                        distribusjonId = row.stringOrNull("distribusjon_id"),
                        type = UtsendingType.valueOf(row.string("type")),
                        utsendingSak = utsendingSak,
                    )
                }.asSingle,
            )
        }
    }

    private fun Row.rehydrerUtsendingTilstand(kolonneNavn: String): Utsending.Tilstand {
        return when (Tilstand.Type.valueOf(this.string(kolonneNavn))) {
            VenterPåVedtak -> Utsending.VenterPåVedtak
            AvventerArkiverbarVersjonAvBrev -> Utsending.AvventerArkiverbarVersjonAvBrev
            AvventerJournalføring -> Utsending.AvventerJournalføring
            AvventerDistribuering -> Utsending.AvventerDistribuering
            Distribuert -> Utsending.Distribuert
            Avbrutt -> Utsending.Avbrutt
        }
    }

    private fun hentIdentForOppgaveId(oppgaveId: UUID): String {
        sessionOf(ds).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT per.ident
                        FROM oppgave_v1 opp
                        JOIN behandling_v1 beh on opp.behandling_id = beh.id
                        JOIN person_v1 per on beh.person_id = per.id
                        WHERE opp.id = :oppgave_id
                        """.trimIndent(),
                    paramMap = mapOf("oppgave_id" to oppgaveId),
                ).map { row -> row.string("ident") }.asSingle,
            ) ?: throw IdentIkkeFunnet("Fant ikke ident for oppgaveId: $oppgaveId")
        }
    }
}

private fun Session.lagreUtsendingSak(utsendingSak: UtsendingSak) {
    this.run(
        queryOf(
            //language=PostgreSQL
            statement =
                """
                INSERT INTO utsending_sak_v1
                    (id, kontekst) 
                VALUES
                    (:id, :kontekst) 
                ON CONFLICT (id) DO NOTHING 
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to utsendingSak.id,
                    "kontekst" to utsendingSak.kontekst,
                ),
        ).asUpdate,
    )
}
