package no.nav.dagpenger.saksbehandling.utsending.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.helper.lagreOppgave
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Avbrutt
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerArkiverbarVersjonAvBrev
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerDistribuering
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerJournalføring
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Distribuert
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.VenterPåVedtak
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource

class PostgresUtsendingRepositoryTest {
    @Test
    fun `lagring og henting av utsending`() {
        withMigratedDb { ds ->

            val oppgave = lagreOppgave(ds)
            val brev = "vedtaksbrev.html"
            val sak = Sak("id", "fagsystem")

            val repository = PostgresUtsendingRepository(ds)
            val distribusjonId = "distribusjonId"
            val utsending =
                Utsending(
                    oppgaveId = oppgave.oppgaveId,
                    brev = brev,
                    sak = sak,
                    ident = oppgave.behandling.person.ident,
                    distribusjonId = distribusjonId,
                )
            repository.lagre(utsending)

            repository.hent(utsending.oppgaveId) shouldBe utsending

            repository.finnUtsendingFor(UUID.randomUUID()) shouldBe null
            shouldThrow<UtsendingIkkeFunnet> {
                repository.hent(UUID.randomUUID())
            }
        }
    }

    @Test
    fun `skal kunne finne ut om en utsending finnes eller ikke for oppgaveId og behandlingId`() {
        withMigratedDb { ds ->
            val oppgave = lagreOppgave(ds)
            val repository = PostgresUtsendingRepository(ds)
            val utsending =
                Utsending(
                    oppgaveId = oppgave.oppgaveId,
                    brev = "brev",
                    sak = Sak("id", "fagsystem"),
                    ident = oppgave.behandling.person.ident,
                    distribusjonId = "distribusjonId",
                )
            repository.lagre(utsending)

            repository.utsendingFinnesForOppgave(oppgave.oppgaveId) shouldBe true
            repository.utsendingFinnesForBehandling(oppgave.behandling.behandlingId) shouldBe true

            repository.utsendingFinnesForOppgave(UUIDv7.ny()) shouldBe false
            repository.utsendingFinnesForBehandling(UUIDv7.ny()) shouldBe false
        }
    }

    private fun DataSource.lagreUtsending(tilstand: Utsending.Tilstand.Type): Utsending {
        val utsendingRepository = PostgresUtsendingRepository(this)
        val oppgave = lagreOppgave(this)
        val utsending =
            Utsending(
                oppgaveId = oppgave.oppgaveId,
                brev = "brev",
                ident = oppgave.behandling.person.ident,
            )
        utsendingRepository.lagre(utsending)
        sessionOf(this).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        UPDATE  utsending_v1 
                        SET     tilstand = :tilstand
                        WHERE   id = :id
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "id" to utsending.id,
                            "tilstand" to tilstand.name,
                        ),
                ).asUpdate,
            )
        }
        return utsending
    }

    @Test
    fun `Skal kunne hente utsendinger som ikke er ferdig distribuert`() {
        withMigratedDb { ds ->
            val repository = PostgresUtsendingRepository(ds)

            val utsendingVenterPåVedtak = ds.lagreUtsending(VenterPåVedtak)
            repository.utsendingFinnesForOppgave(utsendingVenterPåVedtak.oppgaveId) shouldBe true

            val utsendingAvventerArkiverbarVersjonAvBrev = ds.lagreUtsending(AvventerArkiverbarVersjonAvBrev)
            repository.utsendingFinnesForOppgave(utsendingAvventerArkiverbarVersjonAvBrev.oppgaveId) shouldBe true

            val utsendingAvventerJournalføring = ds.lagreUtsending(AvventerJournalføring)
            repository.utsendingFinnesForOppgave(utsendingAvventerJournalføring.oppgaveId) shouldBe true

            val utsendingAvventerDistribuering = ds.lagreUtsending(AvventerDistribuering)
            repository.utsendingFinnesForOppgave(utsendingAvventerDistribuering.oppgaveId) shouldBe true

            val utsendingDistribuert = ds.lagreUtsending(tilstand = Distribuert)
            repository.utsendingFinnesForOppgave(utsendingDistribuert.oppgaveId) shouldBe true

            val utsendingAvbrutt = ds.lagreUtsending(tilstand = Avbrutt)
            repository.utsendingFinnesForOppgave(utsendingAvbrutt.oppgaveId) shouldBe true

            val ventendeUtsendinger = repository.hentVentendeUtsendinger(intervallAntallTimer = 0)
            ventendeUtsendinger.size shouldBe 4
        }
    }
}
