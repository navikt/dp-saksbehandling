package no.nav.dagpenger.behandling.persistence

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import java.util.UUID

interface BehandlingRepository {
    fun hentBehandlinger(): List<Behandling>
    fun hentBehandling(behandlingUUID: UUID): Behandling
    fun hentBehandlingerFor(fnr: String): List<Behandling>
    fun lagreBehandling(behandling: Behandling): Unit = TODO()
}

object Inmemory : BehandlingRepository {
    private val behandlinger = mutableListOf(
        SøknadInnsendtHendelse(UUID.randomUUID(), "", "123").lagBehandling(),
        SøknadInnsendtHendelse(UUID.randomUUID(), "", "456").lagBehandling(),
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
