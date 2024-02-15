package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse

private val logger = KotlinLogging.logger {}

internal class Mediator(private val personRepository: PersonRepository) {
    fun behandle(behandlingOpprettetHendelse: BehandlingOpprettetHendelse) {
        val ident = behandlingOpprettetHendelse.ident
        val person = personRepository.hent(ident) ?: Person(ident)
        person.håndter(behandlingOpprettetHendelse)
        personRepository.lagre(person)
    }
}
