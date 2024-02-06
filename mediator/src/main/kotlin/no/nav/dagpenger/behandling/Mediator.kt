package no.nav.dagpenger.behandling

import mu.KotlinLogging
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.behandling.modell.hendelser.VurderAvslagPåMinsteinntektHendelse

private val logger = KotlinLogging.logger {}

internal class Mediator {
    fun behandle(vurderAvslagPåMinsteinntektHendelse: VurderAvslagPåMinsteinntektHendelse) {
        logger.info { "Her skal vi behandle noe" }
    }

    fun behandle(vurderAvslagPåMinsteinntektHendelse: SøknadInnsendtHendelse) {
        TODO("Not yet implemented")
    }
}
