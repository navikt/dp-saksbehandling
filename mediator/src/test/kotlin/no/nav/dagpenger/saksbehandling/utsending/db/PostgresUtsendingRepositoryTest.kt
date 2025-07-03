package no.nav.dagpenger.saksbehandling.utsending.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.withBehandling
import no.nav.dagpenger.saksbehandling.helper.lagreOppgave
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.lagUtsending
import no.nav.dagpenger.saksbehandling.testPerson
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import no.nav.dagpenger.saksbehandling.utsending.UtsendingType
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException
import java.time.LocalDateTime
import java.util.UUID

class PostgresUtsendingRepositoryTest {
    val behandling =
        Behandling(
            behandlingId = UUIDv7.ny(),
            type = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = LocalDateTime.now(),
            hendelse = TomHendelse,
        )

    @Test
    fun `lagring og henting av utsending`() {
        DBTestHelper.withBehandling(behandling = behandling, person = testPerson) { ds ->

            val oppgave = lagreOppgave(ds, behandling.behandlingId, testPerson.ident)
            val brev = "vedtaksbrev.html"
            val utsendingSak = UtsendingSak("id", "fagsystem")

            val repository = PostgresUtsendingRepository(ds)
            val distribusjonId = "distribusjonId"
            val utsending =
                Utsending(
                    oppgaveId = oppgave.oppgaveId,
                    brev = brev,
                    utsendingSak = utsendingSak,
                    ident = oppgave.personIdent(),
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
    fun `Skal kunne hente utsending for behandlingId`() {
        val testPerson = DBTestHelper.testPerson

        withBehandling(behandling = behandling, person = testPerson) { ds ->
            val repository = PostgresUtsendingRepository(ds)
            val oppgave = lagreOppgave(ds, behandling.behandlingId, testPerson.ident)
            val utsending =
                Utsending(
                    oppgaveId = oppgave.oppgaveId,
                    brev = null,
                    ident = testPerson.ident,
                )

            repository.lagre(utsending)

            val finnUtsendingForBehandlingId =
                repository.finnUtsendingForBehandlingId(behandlingId = behandling.behandlingId)
            finnUtsendingForBehandlingId shouldBe utsending
            repository.finnUtsendingForBehandlingId(behandlingId = UUID.randomUUID()) shouldBe null
        }
    }

    @Test
    fun `skal kunne finne ut om en utsending finnes eller ikke for oppgaveId og behandlingId`() {
        DBTestHelper.withBehandling(behandling = behandling, person = testPerson) { ds ->
            val oppgave = lagreOppgave(ds, behandling.behandlingId, testPerson.ident)
            val repository = PostgresUtsendingRepository(ds)
            val utsending =
                Utsending(
                    oppgaveId = oppgave.oppgaveId,
                    brev = "brev",
                    utsendingSak = UtsendingSak("id", "fagsystem"),
                    ident = oppgave.personIdent(),
                    distribusjonId = "distribusjonId",
                )
            repository.lagre(utsending)

            repository.utsendingFinnesForOppgave(oppgave.oppgaveId) shouldBe true
            repository.utsendingFinnesForBehandling(oppgave.behandlingId) shouldBe true

            repository.utsendingFinnesForOppgave(UUIDv7.ny()) shouldBe false
            repository.utsendingFinnesForBehandling(UUIDv7.ny()) shouldBe false
        }
    }

    @Test
    fun `Skal ikke kunne lagre flere utsendinger for samme oppgave`() {
        DBTestHelper.withBehandling(behandling = behandling) { ds ->
            val oppgave = lagreOppgave(ds, behandling.behandlingId)
            val repository = PostgresUtsendingRepository(ds)
            repository.lagre(lagUtsending(tilstand = Utsending.VenterPåVedtak, oppgaveId = oppgave.oppgaveId))
            shouldThrow<PSQLException> {
                repository.lagre(lagUtsending(tilstand = Utsending.VenterPåVedtak, oppgaveId = oppgave.oppgaveId))
            }.message shouldStartWith """ERROR: duplicate key value violates unique constraint "oppgave_id_unique""""
        }
    }
}
