package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.BehandlingSvar
import no.nav.dagpenger.behandling.hendelser.StegUtført
import no.nav.dagpenger.behandling.hendelser.SøknadBehandletHendelse
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.behandling.oppgave.InMemoryOppgaveRepository
import no.nav.dagpenger.behandling.oppgave.OppgaveRepository
import no.nav.dagpenger.behandling.persistence.BehandlingRepository
import no.nav.dagpenger.behandling.persistence.Inmemory
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

class Mediator(
    private val rapidsConnection: RapidsConnection,
    private val behandlingRepository: BehandlingRepository = Inmemory,
    private val oppgaveRepository: OppgaveRepository = InMemoryOppgaveRepository(),
) : BehandlingRepository by behandlingRepository, OppgaveRepository by oppgaveRepository {
    inline fun <reified T> behandle(hendelse: BehandlingSvar<T>) {
        val behandling = hentBehandling(hendelse.behandlingUUID)
        behandling.besvar(hendelse.stegUUID, hendelse.verdi)
    }

    fun behandle(hendelse: SøknadInnsendtHendelse) {
        behandlingRepository.lagreBehandling(hendelse.behandling)
        lagreOppgave(hendelse.oppgave())
    }

    fun behandle(søknadBehandlet: SøknadBehandlet) {
        val behandling = hentBehandling(søknadBehandlet.behandlingId)
        val søknadBehandletHendelse = SøknadBehandletHendelse(
            behandling = behandling,
            innvilget = søknadBehandlet.innvilget,
        )

        behandling.håndter(søknadBehandletHendelse)
        val søknadBehandletMessage = JsonMessage.newMessage(
            eventName = "søknad_behandlet_hendelse",
            map = søknadBehandletHendelse.toJsonMessageMap(),
        )
        rapidsConnection.publish(behandling.person.ident, søknadBehandletMessage.toJson())
    }

    fun behandle(hendelse: StegUtført, block: IBehandling.() -> Unit) {
        val oppgave = hentOppgave(hendelse.oppgaveUUID)
        block(oppgave)
        lagreOppgave(oppgave)
    }
}
