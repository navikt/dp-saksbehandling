package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.BehandlingSvar
import no.nav.dagpenger.behandling.persistence.BehandlingRepository
import no.nav.dagpenger.behandling.persistence.Inmemory

class Mediator(
    private val behandlingRepository: BehandlingRepository = Inmemory,

) : BehandlingRepository by behandlingRepository {

    inline fun<reified T> behandle(hendelse: BehandlingSvar<T>) {
        val behandling = hentBehandling(hendelse.behandlingUUID)
        behandling.besvar(hendelse.stegUUID, verdi = hendelse.verdi)
    }
}