package no.nav.dagpenger.behandling

import mu.KotlinLogging
import no.nav.dagpenger.behandling.db.PersonRepository
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.hendelser.mottak.AldersbehovLøsningMottak
import no.nav.dagpenger.behandling.hendelser.mottak.SøknadMottak
import no.nav.helse.rapids_rivers.RapidsConnection
import java.lang.RuntimeException

internal class PersonMediator(rapidsConnection: RapidsConnection, private val personRepository: PersonRepository) {

    private companion object {
        val logger = KotlinLogging.logger {}
        val sikkerLogger = KotlinLogging.logger("tjenestekall.PersonMediator")
    }

    private val behovMediator = BehovMediator(
        rapidsConnection, sikkerLogger
    )
    init {
        SøknadMottak(rapidsConnection, this)
        AldersbehovLøsningMottak(rapidsConnection, this)
    }
    fun behandle(søknadHendelse: SøknadHendelse) {
        behandle(søknadHendelse) { person ->
            person.håndter(søknadHendelse)
        }
    }

    fun behandle(hendelse: Paragraf_4_23_alder_resultat) {
        behandle(hendelse) { person ->
            person.håndter(hendelse)
        }
    }

    private fun behandle(hendelse: Hendelse, håndter: (Person) -> Unit) {
        try {
            val person = hentEllerOpprettPerson(hendelse)
            håndter(person)
            personRepository.lagrePerson(person)
            behovMediator.håndter(hendelse)
        } catch (e: Exception) {
            logger.error(e) { "Kunne ikke behandle hendelse ${hendelse.javaClass.simpleName}" }
        }
    }

    private fun hentEllerOpprettPerson(hendelse: Hendelse): Person {
        val person: Person? = personRepository.hentPerson(hendelse.ident())
        return when (hendelse) {
            is SøknadHendelse -> person ?: Person(hendelse.ident())
            else -> person ?: throw RuntimeException("Person finnes ikke")
        }
    }
}
