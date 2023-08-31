package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.BehandlingObserver.BehandlingEndretTilstand
import no.nav.dagpenger.behandling.BehandlingObserver.VedtakFattet
import no.nav.dagpenger.behandling.hendelser.StegUtført
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.behandling.oppgave.InMemoryOppgaveRepository
import no.nav.dagpenger.behandling.oppgave.OppgaveRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

internal class Mediator(
    private val rapidsConnection: RapidsConnection,
    private val oppgaveRepository: OppgaveRepository = InMemoryOppgaveRepository(),
    private val personRepository: PersonRepository = InMemoryPersonRepository,
    private val aktivitetsloggMediator: AktivitetsloggMediator,
) : OppgaveRepository by oppgaveRepository, BehandlingObserver {
    fun behandle(hendelse: SøknadInnsendtHendelse) {
        val person = personRepository.hentPerson(hendelse.ident()) ?: Person(hendelse.ident()).also {
            it.håndter(hendelse)
            personRepository.lagrePerson(it)
        }
        lagreOppgave(hendelse.oppgave(person))
        aktivitetsloggMediator.håndter(hendelse)
    }

    fun behandle(hendelse: StegUtført, block: Svarbart.() -> Unit) {
        val oppgave = hentOppgave(hendelse.oppgaveUUID)
        block(oppgave)
        lagreOppgave(oppgave)
        aktivitetsloggMediator.håndter(hendelse)
    }

    override fun hentOppgave(uuid: UUID) = oppgaveRepository.hentOppgave(uuid).also {
        it.addObserver(this)
    }

    override fun behandlingEndretTilstand(søknadEndretTilstandEvent: BehandlingEndretTilstand) =
        publishEvent("behandling_endret_tilstand", søknadEndretTilstandEvent)

    override fun vedtakFattet(vedtakFattetEvent: VedtakFattet) =
        publishEvent("søknad_behandlet_hendelse", vedtakFattetEvent)

    private fun publishEvent(navn: String, event: BehandlingObserver.BehandlingEvent) =
        rapidsConnection.publish(event.ident, JsonMessage.newMessage(navn, event.toMap()).toJson())
}
