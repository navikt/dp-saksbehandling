package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.BehandlingSvar
import no.nav.dagpenger.behandling.persistence.BehandlingRepository
import no.nav.dagpenger.behandling.persistence.Inmemory

class Mediator(
    private val behandlingRepository: BehandlingRepository = Inmemory,

) : BehandlingRepository by behandlingRepository {
    fun behandle(hendelse: BehandlingSvar) {
        val behandling = hentBehandling(hendelse.behandlinUUID)
        behandling.besvar(
            hendelse.stegUUID,
            hendelse.svar.verdi,
        )
    }
}
