package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse

private val logger = KotlinLogging.logger {}

internal class Mediator(private val personRepository: PersonRepository) {
    fun behandle(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        val ident = søknadsbehandlingOpprettetHendelse.ident
        val person = personRepository.hent(ident) ?: Person(ident)
        person.håndter(søknadsbehandlingOpprettetHendelse)
        personRepository.lagre(person)
    }
}
