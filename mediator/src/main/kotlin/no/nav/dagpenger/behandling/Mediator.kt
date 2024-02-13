package no.nav.dagpenger.behandling

import mu.KotlinLogging
import no.nav.dagpenger.behandling.mottak.BehandlingOpprettetHendelse

private val logger = KotlinLogging.logger {}

internal class Mediator {
    fun behandle(behandlingOpprettetHendelse: BehandlingOpprettetHendelse) {
        logger.info { "Her skal vi behandle noe" }
    }
}
