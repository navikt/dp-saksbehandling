package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.BehandlingSvar
import no.nav.dagpenger.behandling.hendelser.SøknadBehandletHendelse
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.persistence.BehandlingRepository
import no.nav.dagpenger.behandling.persistence.Inmemory

class Mediator(
    private val behandlingRepository: BehandlingRepository = Inmemory,
) : BehandlingRepository by behandlingRepository {

    inline fun <reified T> behandle(hendelse: BehandlingSvar<T>) {
        val behandling = hentBehandling(hendelse.behandlingUUID)
        behandling.besvar(hendelse.stegUUID, verdi = hendelse.verdi)
    }

    fun behandle(hendelse: SøknadHendelse) {
        behandlingRepository.lagreBehandling(hendelse.lagBehandling())
    }

    fun behandle(hendelse: SøknadBehandletHendelse) {
        val behandling = hentBehandling(hendelse.behandlingId)
        behandling.håndter(hendelse)

        TODO()
        // puplisere melding på kafka
    }
}
