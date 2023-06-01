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

class Mediator(
    private val rapidsConnection: RapidsConnection,
    private val oppgaveRepository: OppgaveRepository = InMemoryOppgaveRepository(),
    val dings: Dings,
) : OppgaveRepository by oppgaveRepository, BehandlingObserver {
    fun behandle(hendelse: SøknadInnsendtHendelse) {
        // TODO: Fullfør denne tanken. Dings skal være ansvarlig for å finne alle prosesser for en hendelse
        /*val oppgaver = dings.oppgaver(hendelse)
        oppgaver.forEach {
            lagreOppgave(it)
        }*/
        lagreOppgave(hendelse.oppgave())
    }

    fun behandle(hendelse: StegUtført, block: Svarbart.() -> Unit) {
        val oppgave = hentOppgave(hendelse.oppgaveUUID)
        block(oppgave)
        lagreOppgave(oppgave)
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
