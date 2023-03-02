package no.nav.dagpenger.behandling.observers

import mu.KotlinLogging
import no.nav.dagpenger.behandling.BehandlingObserver
import no.nav.dagpenger.behandling.PersonObserver

object PersonLogger : PersonObserver {

    private val logger = KotlinLogging.logger { }
    override fun behandlingTilstandEndret(event: BehandlingObserver.BehandlingEndretTilstandEvent) {
        logger.info { "Behandling ${event.behandlingsId} endrer tilstand fra ${event.forrigeTilstand} til ny tilstand ${event.gjeldendeTilstand}" }
    }
}
