package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetHendelse

private val logger = KotlinLogging.logger {}

internal class Mediator {
    fun behandle(behandlingOpprettetHendelse: BehandlingOpprettetHendelse) {
        logger.info { "Her skal vi behandle noe" }
    }
}
