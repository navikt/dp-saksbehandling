package no.nav.dagpenger.saksbehandling.utsending.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.helper.lagreOppgave
import no.nav.dagpenger.saksbehandling.lagUtsending
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.UtsendingType
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException
import java.util.UUID

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
                    type = UtsendingType.KLAGEMELDING,
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

    @Test
    fun `Skal ikke kunne lagre flere utsendinger for samme oppgave`() {
        withMigratedDb { ds ->
            val oppgave = lagreOppgave(ds)
            val repository = PostgresUtsendingRepository(ds)
            repository.lagre(lagUtsending(tilstand = Utsending.VenterPåVedtak, oppgaveId = oppgave.oppgaveId))
            shouldThrow<PSQLException> {
                repository.lagre(lagUtsending(tilstand = Utsending.VenterPåVedtak, oppgaveId = oppgave.oppgaveId))
            }.message shouldStartWith """ERROR: duplicate key value violates unique constraint "oppgave_id_unique""""
        }
    }
}
