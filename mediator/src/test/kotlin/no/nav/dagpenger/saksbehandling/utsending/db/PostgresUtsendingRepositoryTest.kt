package no.nav.dagpenger.saksbehandling.utsending.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
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
            utløstAv = UtløstAvType.SØKNAD,
            opprettet = LocalDateTime.now(),
            hendelse = TomHendelse,
        )

    @Test
    fun `lagring og henting av utsending`() {
        withBehandling(behandling = behandling, person = testPerson) { ds ->

            val oppgave = lagreOppgave(ds, behandling.behandlingId, testPerson.ident)
            val brev = "vedtaksbrev.html"
            val utsendingSak = UtsendingSak("id", "fagsystem")

            val repository = PostgresUtsendingRepository(ds)
            val distribusjonId = "distribusjonId"
            val utsending =
                Utsending(
                    behandlingId = oppgave.behandlingId,
                    brev = brev,
                    utsendingSak = utsendingSak,
                    ident = oppgave.personIdent(),
                    distribusjonId = distribusjonId,
                    type = UtsendingType.KLAGEMELDING,
                )
            repository.lagre(utsending)

            repository.hentUtsendingForBehandlingId(oppgave.behandlingId) shouldBe utsending

            repository.finnUtsendingForBehandlingId(UUID.randomUUID()) shouldBe null
            shouldThrow<UtsendingIkkeFunnet> {
                repository.hentUtsendingForBehandlingId(UUID.randomUUID())
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
                    behandlingId = oppgave.behandlingId,
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
        withBehandling(behandling = behandling, person = testPerson) { ds ->
            val oppgave = lagreOppgave(ds, behandling.behandlingId, testPerson.ident)
            val repository = PostgresUtsendingRepository(ds)
            val utsending =
                Utsending(
                    behandlingId = oppgave.behandlingId,
                    brev = "brev",
                    utsendingSak = UtsendingSak("id", "fagsystem"),
                    ident = oppgave.personIdent(),
                    distribusjonId = "distribusjonId",
                )
            repository.lagre(utsending)

            repository.utsendingFinnesForBehandling(oppgave.behandlingId) shouldBe true

            repository.utsendingFinnesForBehandling(UUIDv7.ny()) shouldBe false
        }
    }

    @Test
    fun `Skal ikke kunne lagre flere utsendinger for samme behandling`() {
        withBehandling(behandling = behandling) { ds ->
            val oppgave = lagreOppgave(ds, behandling.behandlingId)
            val repository = PostgresUtsendingRepository(ds)
            repository.lagre(lagUtsending(tilstand = Utsending.VenterPåVedtak, behandlingId = oppgave.behandlingId))
            shouldThrow<PSQLException> {
                repository.lagre(lagUtsending(tilstand = Utsending.VenterPåVedtak, behandlingId = oppgave.behandlingId))
            }.message shouldStartWith """ERROR: duplicate key value violates unique constraint "behandling_id_unique""""
        }
    }
}
