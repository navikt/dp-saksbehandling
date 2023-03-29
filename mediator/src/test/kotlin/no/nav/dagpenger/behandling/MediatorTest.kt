package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.dsl.BehandlingDSL
import no.nav.dagpenger.behandling.hendelser.BehandlingSvar
import no.nav.dagpenger.behandling.persistence.BehandlingRepository
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class MediatorTest {

    @Test
    fun `Behandle BehandlingSvar hendelse`() {
        val mediator = Mediator(mockPersistence)
        mediator.behandle(
            BehandlingSvar(
                ident = "123",
                behandlingUUID = mockPersistence.behandlingId,
                stegUUID = mockPersistence.finnStegId("vilkår1"),
                verdi = false,
            ),
        )

        mediator.behandle(
            BehandlingSvar(
                ident = "123",
                behandlingUUID = mockPersistence.behandlingId,
                stegUUID = mockPersistence.finnStegId("vilkår 1 dato"),
                verdi = LocalDate.now(),
            ),
        )

        mediator.behandle(
            BehandlingSvar(
                ident = "123",
                behandlingUUID = mockPersistence.behandlingId,
                stegUUID = mockPersistence.finnStegId("fastsettelse1"),
                verdi = 3,
            ),
        )
    }

    private val mockPersistence = object : BehandlingRepository {
        val testPerson = Person("123")
        var behandlingId: UUID
        val behandlinger = listOf(
            BehandlingDSL.behandling(testPerson) {
                steg {
                    vilkår("vilkår1") {
                        avhengerAvFastsettelse<LocalDate>("vilkår 1 dato")
                    }
                }
                steg {
                    fastsettelse<Int>("fastsettelse1")
                }
            }.also { behandlingId = it.uuid },
        )

        override fun hentBehandlinger() = behandlinger
        override fun hentBehandling(behandlingUUID: UUID) = behandlinger.single { it.uuid == behandlingUUID }

        fun finnStegId(id: String): UUID {
            return behandlinger.single().alleSteg().single { it.id == id }.uuid
        }
    }
}
