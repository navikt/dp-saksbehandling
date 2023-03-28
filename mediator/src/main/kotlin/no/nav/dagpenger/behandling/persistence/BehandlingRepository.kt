package no.nav.dagpenger.behandling.persistence

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Hubba
import no.nav.dagpenger.behandling.Person
import java.util.UUID

interface BehandlingRepository {
    fun hentBehandlinger(): List<Behandling>
    fun hentBehandling(behandlingUUID: UUID): Behandling
}

object Inmemory : BehandlingRepository {
    val testPersonOla = Person("123")
    val testPersonKari = Person("456")

    val behandlinger = listOf<Behandling>(
        Hubba.bubba(testPersonOla),
        Hubba.bubba(testPersonKari),

    )

    override fun hentBehandlinger(): List<Behandling> {
        return behandlinger
    }

    override fun hentBehandling(behandlingUUID: UUID): Behandling {
        return behandlinger.single {
            it.uuid == behandlingUUID
        }
    }
}
