package no.nav.dagpenger.saksbehandling.utsending

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.BehandlingType.KLAGE
import no.nav.dagpenger.saksbehandling.BehandlingType.RETT_TIL_DAGPENGER
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.helper.lagreOppgave
import no.nav.dagpenger.saksbehandling.lagBehandling
import no.nav.dagpenger.saksbehandling.lagPerson
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
        val person = lagPerson()
        val behandling1 = lagBehandling(type = RETT_TIL_DAGPENGER)
        val behandling2 = lagBehandling(type = RETT_TIL_DAGPENGER)
        val behandling3 = lagBehandling(type = KLAGE)
        val behandling4 = lagBehandling(type = KLAGE)
        val behandling5 = lagBehandling(type = RETT_TIL_DAGPENGER)
        val behandling6 = lagBehandling(type = RETT_TIL_DAGPENGER)

        DBTestHelper.withBehandlinger(
            person = person,
            behandlinger = listOf(behandling1, behandling2, behandling3, behandling4, behandling5, behandling6),
        ) { ds ->
            val repository = UtsendingAlarmRepository(ds)
            ds.lagreUtsending(
                tilstand = VenterPåVedtak,
                tidspunkt = tjueFireTimerSiden,
                behandling = behandling1,
                person = person,
            )
            ds.lagreUtsending(
                tilstand = AvventerArkiverbarVersjonAvBrev,
                tidspunkt = tjueFireTimerSiden,
                behandling = behandling2,
                person = person,
            )
            ds.lagreUtsending(
                tilstand = AvventerJournalføring,
                tidspunkt = tjueFireTimerSiden,
                behandling = behandling3,
                person = person,
            )
            ds.lagreUtsending(
                tilstand = AvventerDistribuering,
                tidspunkt = tjueFireTimerSiden,
                behandling = behandling4,
                person = person,
            )
            ds.lagreUtsending(
                tilstand = Distribuert,
                tidspunkt = tjueFireTimerSiden,
                behandling = behandling5,
            )
            ds.lagreUtsending(
                tilstand = Avbrutt,
                tidspunkt = tjueFireTimerSiden,
                behandling = behandling6,
            )

            val ventendeUtsendinger = repository.hentVentendeUtsendinger(intervallAntallTimer = 23)
            ventendeUtsendinger.size shouldBe 4
            ventendeUtsendinger.map { it.tilstand } shouldBe
                listOf(
                    VenterPåVedtak.name,
                    AvventerArkiverbarVersjonAvBrev.name,
                    AvventerJournalføring.name,
                    AvventerDistribuering.name,
                )
        }
    }

    private fun DataSource.lagreUtsending(
        tilstand: Utsending.Tilstand.Type,
        tidspunkt: LocalDateTime = LocalDateTime.now(),
        behandling: Behandling = lagBehandling(type = RETT_TIL_DAGPENGER),
        person: Person = lagPerson(),
    ): Utsending {
        val oppgave =
            lagreOppgave(dataSource = this, behandlingId = behandling.behandlingId, personIdent = person.ident)
        val utsending =
            Utsending(
                oppgaveId = oppgave.oppgaveId,
                brev = "brev",
                ident = oppgave.personIdent(),
            )
        sessionOf(this).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO utsending_v1
                         ( id, oppgave_id, tilstand, brev, pdf_urn, 
                           journalpost_id, distribusjon_id, utsending_sak_id,
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
