package no.nav.dagpenger.saksbehandling.utsending

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.helper.lagreOppgave
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Avbrutt
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerArkiverbarVersjonAvBrev
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerDistribuering
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerJournalføring
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Distribuert
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.VenterPåVedtak
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import javax.sql.DataSource

class UtsendingAlarmRepositoryTest {
    @Test
    fun `Skal kunne hente utsendinger som ikke er ferdig distribuert`() {
        val nå = LocalDateTime.now()
        val tjueFireTimerSiden = nå.minusHours(24)
        withMigratedDb { ds ->
            val repository = UtsendingAlarmRepository(ds)
            val utsendingVenterPåVedtak = ds.lagreUtsending(VenterPåVedtak, tjueFireTimerSiden)
            val utsendingAvventerArkiverbarVersjonAvBrev =
                ds.lagreUtsending(AvventerArkiverbarVersjonAvBrev, tjueFireTimerSiden)
            val utsendingAvventerJournalføring = ds.lagreUtsending(AvventerJournalføring, tjueFireTimerSiden)
            val utsendingAvventerDistribuering = ds.lagreUtsending(AvventerDistribuering, tjueFireTimerSiden)
            val utsendingDistribuert = ds.lagreUtsending(tilstand = Distribuert, tjueFireTimerSiden)
            val utsendingAvbrutt = ds.lagreUtsending(tilstand = Avbrutt, tjueFireTimerSiden)

            val ventendeUtsendinger = repository.hentVentendeUtsendinger(intervallAntallTimer = 23)
            ventendeUtsendinger.size shouldBe 4
            ventendeUtsendinger.map { it.tilstand } shouldBe
                listOf(
                    VenterPåVedtak,
                    AvventerArkiverbarVersjonAvBrev,
                    AvventerJournalføring,
                    AvventerDistribuering,
                )
        }
    }

    private fun DataSource.lagreUtsending(
        tilstand: Utsending.Tilstand.Type,
        tidspunkt: LocalDateTime = LocalDateTime.now(),
    ): Utsending {
        val oppgave = lagreOppgave(this)
        val utsending =
            Utsending(
                oppgaveId = oppgave.oppgaveId,
                brev = "brev",
                ident = oppgave.behandling.person.ident,
            )
        sessionOf(this).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO utsending_v1
                         ( id, oppgave_id, tilstand, brev, pdf_urn, 
                           journalpost_id, distribusjon_id, sak_id,
                           registrert_tidspunkt, endret_tidspunkt)
                        VALUES (:id, :oppgave_id, :tilstand, null, null, null, null, null, :tidspunkt, :tidspunkt);
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "id" to utsending.id,
                            "oppgave_id" to utsending.oppgaveId,
                            "tilstand" to tilstand.name,
                            "tidspunkt" to tidspunkt,
                        ),
                ).asUpdate,
            )
        }
        return utsending
    }
}
