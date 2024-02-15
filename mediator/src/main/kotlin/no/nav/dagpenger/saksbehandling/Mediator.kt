package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VerifiserOpplysningHendelse

private val logger = KotlinLogging.logger {}

internal class Mediator(private val personRepository: PersonRepository) {
    fun behandle(behandlingOpprettetHendelse: BehandlingOpprettetHendelse) {
        val ident = behandlingOpprettetHendelse.ident
        val person = personRepository.hent(ident) ?: Person(ident)
        person.håndter(behandlingOpprettetHendelse)
        personRepository.lagre(person)
    }

    fun behandle(verifiserOpplysningHendelse: VerifiserOpplysningHendelse) {
        val ident = verifiserOpplysningHendelse.ident
        val person = personRepository.hent(ident) ?: Person(ident)
        person.håndter(verifiserOpplysningHendelse)
        personRepository.lagre(person)
    }
}
