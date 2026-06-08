package no.nav.dagpenger.saksbehandling.utsending.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.TestHelper.lagUtsending
import no.nav.dagpenger.saksbehandling.TestHelper.testPerson
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.withBehandling
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Transaksjoner
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
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
            utløstAv = HendelseBehandler.DpBehandling.Søknad,
            opprettet = LocalDateTime.now(),
            hendelse = TomHendelse,
        )

    @Test
    fun `lagring og henting av utsending`() {
        withBehandling(behandling = behandling, person = testPerson) { ds ->

            val brev = "vedtaksbrev.html"
            val utsendingSak = UtsendingSak("id", "fagsystem")

            val repository = PostgresUtsendingRepository(DatabaseSession(ds))
            val distribusjonId = "distribusjonId"
            val utsending =
                Utsending(
                    behandlingId = behandling.behandlingId,
                    brev = brev,
                    utsendingSak = utsendingSak,
                    ident = testPerson.ident,
                    distribusjonId = distribusjonId,
                    type = UtsendingType.KLAGE_OVERSENDELSE,
                )
            repository.lagre(utsending)

            repository.hentUtsendingForBehandlingId(behandling.behandlingId) shouldBe utsending

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
            val repository = PostgresUtsendingRepository(DatabaseSession(ds))
            val utsending =
                Utsending(
                    behandlingId = behandling.behandlingId,
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
            val repository = PostgresUtsendingRepository(DatabaseSession(ds))
            val utsending =
                Utsending(
                    behandlingId = behandling.behandlingId,
                    brev = "brev",
                    utsendingSak = UtsendingSak("id", "fagsystem"),
                    ident = testPerson.ident,
                    distribusjonId = "distribusjonId",
                )
            repository.lagre(utsending)

            repository.utsendingFinnesForBehandling(behandling.behandlingId) shouldBe true

            repository.utsendingFinnesForBehandling(UUIDv7.ny()) shouldBe false
        }
    }

    @Test
    fun `Skal ikke kunne lagre flere utsendinger for samme behandling`() {
        withBehandling(behandling = behandling) { ds ->
            val repository = PostgresUtsendingRepository(DatabaseSession(ds))
            repository.lagre(lagUtsending(tilstand = Utsending.VenterPåVedtak, behandlingId = behandling.behandlingId))
            shouldThrow<PSQLException> {
                repository.lagre(
                    lagUtsending(
                        tilstand = Utsending.VenterPåVedtak,
                        behandlingId = behandling.behandlingId,
                    ),
                )
            }.message shouldStartWith """ERROR: duplicate key value violates unique constraint "behandling_id_unique""""
        }
    }

    @Test
    fun `slettUtsending med aktiv transaksjonskontekst sletter raden ved commit`() {
        withBehandling(behandling = behandling, person = testPerson) { ds ->
            val databaseSession = DatabaseSession(ds)
            val repository = PostgresUtsendingRepository(databaseSession)
            val utsending =
                Utsending(
                    behandlingId = behandling.behandlingId,
                    brev = null,
                    ident = testPerson.ident,
                )
            repository.lagre(utsending)
            repository.finnUtsendingForBehandlingId(behandling.behandlingId) shouldBe utsending

            Transaksjoner(databaseSession).transaksjon { ctx ->
                repository.slettUtsending(utsendingId = utsending.id, kontekst = ctx)
            }

            repository.finnUtsendingForBehandlingId(behandling.behandlingId) shouldBe null
        }
    }

    @Test
    fun `slettUtsending deltar i kallerens transaksjon og rulles tilbake sammen med den`() {
        withBehandling(behandling = behandling, person = testPerson) { ds ->
            val databaseSession = DatabaseSession(ds)
            val repository = PostgresUtsendingRepository(databaseSession)
            val utsending =
                Utsending(
                    behandlingId = behandling.behandlingId,
                    brev = null,
                    ident = testPerson.ident,
                )
            repository.lagre(utsending)

            shouldThrow<RuntimeException> {
                Transaksjoner(databaseSession).transaksjon { ctx ->
                    repository.slettUtsending(utsendingId = utsending.id, kontekst = ctx)
                    throw RuntimeException("Tvinger rollback etter sletting")
                }
            }

            repository.finnUtsendingForBehandlingId(behandling.behandlingId) shouldBe utsending
        }
    }
}
