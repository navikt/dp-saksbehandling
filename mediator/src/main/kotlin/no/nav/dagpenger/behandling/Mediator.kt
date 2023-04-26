package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.StegUtført
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.behandling.oppgave.InMemoryOppgaveRepository
import no.nav.dagpenger.behandling.oppgave.OppgaveRepository
import no.nav.helse.rapids_rivers.RapidsConnection

class Mediator(
    private val rapidsConnection: RapidsConnection,
    private val oppgaveRepository: OppgaveRepository = InMemoryOppgaveRepository(),
) : OppgaveRepository by oppgaveRepository {
    fun behandle(hendelse: SøknadInnsendtHendelse) {
        lagreOppgave(hendelse.oppgave())
    }

    fun behandle(søknadBehandlet: SøknadBehandlet) {
        /*val behandling = hentBehandling(søknadBehandlet.behandlingId)
        val søknadBehandletHendelse = SøknadBehandletHendelse(
            behandling = behandling,
            innvilget = søknadBehandlet.innvilget,
        )

        behandling.håndter(søknadBehandletHendelse)
        val søknadBehandletMessage = JsonMessage.newMessage(
            eventName = "søknad_behandlet_hendelse",
            map = søknadBehandletHendelse.toJsonMessageMap(),
        )
        rapidsConnection.publish(behandling.person.ident, søknadBehandletMessage.toJson())*/
    }

    fun behandle(hendelse: StegUtført, block: Svarbart.() -> Unit) {
        val oppgave = hentOppgave(hendelse.oppgaveUUID)
        block(oppgave)
        lagreOppgave(oppgave)
    }
}
