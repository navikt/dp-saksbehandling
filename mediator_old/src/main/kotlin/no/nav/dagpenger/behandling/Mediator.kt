package no.nav.dagpenger.behandling

import mu.KotlinLogging
import no.nav.dagpenger.behandling.BehandlingObserver.BehandlingEndretTilstand
import no.nav.dagpenger.behandling.db.OppgaveRepository
import no.nav.dagpenger.behandling.db.PersonRepository
import no.nav.dagpenger.behandling.db.VurderingRepository
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.behandling.hendelser.VedtakStansetHendelse
import no.nav.dagpenger.behandling.hendelser.VurderAvslagPåMinsteinntektHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class Mediator(
    private val rapidsConnection: RapidsConnection,
    private val oppgaveRepository: OppgaveRepository,
    private val personRepository: PersonRepository,
    private val aktivitetsloggMediator: AktivitetsloggMediator,
    private val vurderingRepository: VurderingRepository,
) : OppgaveRepository by oppgaveRepository, VurderingRepository by vurderingRepository, BehandlingObserver {
    fun behandle(hendelse: SøknadInnsendtHendelse) {
        val person =
            personRepository.hentPerson(hendelse.ident()) ?: Person(hendelse.ident()).also {
                it.håndter(hendelse)
                personRepository.lagrePerson(it)
            }
        val oppgave = hendelse.oppgave(person)
        lagreOppgave(oppgave)
        aktivitetsloggMediator.håndter(hendelse)
    }

    fun behandle(hendelse: VedtakStansetHendelse) {
        val person =
            personRepository.hentPerson(hendelse.ident())
                ?: throw IllegalArgumentException("Fant ikke person, kan ikke utføre stans")
        hendelse.oppgave(person).also {
            lagreOppgave(it)
        }
        aktivitetsloggMediator.håndter(hendelse)
    }

    fun utfør(kommando: UtførStegKommando) {
        val oppgave = hentOppgave(kommando.oppgaveUUID)
        oppgave.utfør(kommando)
        lagreOppgave(oppgave)
        aktivitetsloggMediator.håndter(kommando)
    }

    override fun hentOppgave(uuid: UUID) =
        oppgaveRepository.hentOppgave(uuid).also {
            it.addObserver(this)
        }

    override fun behandlingEndretTilstand(behandlingEndretTilstandEvent: BehandlingEndretTilstand) =
        publishEvent("behandling_endret_tilstand", behandlingEndretTilstandEvent).also {
            logger.info {
                "Publiserer behandling_endret_tilstand for behandlingId=${behandlingEndretTilstandEvent.behandlingId}"
            }
        }

    private fun publishEvent(
        navn: String,
        event: BehandlingObserver.BehandlingEvent,
    ) = rapidsConnection.publish(event.ident, JsonMessage.newMessage(navn, event.toMap()).toJson())

    fun behandle(vurderAvslagPåMinsteinntektHendelse: VurderAvslagPåMinsteinntektHendelse) {
        val oppgave = oppgaveRepository.hentOppgaveFor(vurderAvslagPåMinsteinntektHendelse.søknadUUID)
        oppgave.behandle(vurderAvslagPåMinsteinntektHendelse)
        oppgaveRepository.lagreOppgave(oppgave)
    }
}
