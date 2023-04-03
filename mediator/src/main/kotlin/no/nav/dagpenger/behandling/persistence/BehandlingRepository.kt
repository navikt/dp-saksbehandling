package no.nav.dagpenger.behandling.persistence

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Hubba
import no.nav.dagpenger.behandling.Person
import java.util.UUID

interface BehandlingRepository {
    fun hentBehandlinger(): List<Behandling>
    fun hentBehandling(behandlingUUID: UUID): Behandling
    fun hentBehandlingerFor(fnr: String): List<Behandling>
    fun lagreBehandling(behandling: Behandling): Unit = TODO()
}

object Inmemory : BehandlingRepository {
    val testPersonOla = Person("123")
    val testPersonKari = Person("456")

    val behandlinger = mutableListOf(
        Hubba.bubba(testPersonOla),
        Hubba.bubba(testPersonKari),

    )

    override fun hentBehandlinger() = behandlinger

    override fun hentBehandling(behandlingUUID: UUID): Behandling {
        return behandlinger.firstOrNull { behandling ->
            behandling.uuid == behandlingUUID
        } ?: throw NoSuchElementException()
    }

    override fun hentBehandlingerFor(fnr: String): List<Behandling> {
        val behandlingerForFnr = behandlinger.filter { behandling ->
            behandling.person.ident == fnr
        }.takeIf {
            it.isNotEmpty()
        }

        return behandlingerForFnr ?: throw NoSuchElementException()
    }

    override fun lagreBehandling(behandling: Behandling) {
        behandlinger.add(behandling)
    }
}
