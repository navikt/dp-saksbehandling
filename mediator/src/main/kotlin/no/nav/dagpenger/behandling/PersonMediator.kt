package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.hendelser.mottak.SøknadMottak
import no.nav.helse.rapids_rivers.RapidsConnection

internal class PersonMediator(rapidsConnection: RapidsConnection) {

    init {
        SøknadMottak(rapidsConnection, this)
    }
    fun behandle(søknadHendelse: SøknadHendelse) {
        // finne eller opprette person?
        // person.håndter(søknadHendelse)
        // sende behov?
        TODO("Not yet implemented")
    }
}
