package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.db.PersonRepository
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.hendelser.mottak.SøknadMottak
import no.nav.helse.rapids_rivers.RapidsConnection

internal class PersonMediator(rapidsConnection: RapidsConnection, private val personRepository: PersonRepository) {

    init {
        SøknadMottak(rapidsConnection, this)
    }
    fun behandle(søknadHendelse: SøknadHendelse) {
        val person = personRepository.hentPerson(søknadHendelse.ident()) ?: Person(søknadHendelse.ident())
        person.håndter(søknadHendelse)
        personRepository.lagrePerson(person)
        // sende behov?
    }
}
