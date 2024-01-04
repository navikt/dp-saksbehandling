package no.nav.dagpenger.behandling

import mu.KotlinLogging
import no.nav.dagpenger.behandling.BehandlingObserver.BehandlingEndretTilstand
import no.nav.dagpenger.behandling.BehandlingObserver.VedtakFattet
import no.nav.dagpenger.behandling.db.BehandlingRepository
import no.nav.dagpenger.behandling.db.OppgaveRepository
import no.nav.dagpenger.behandling.db.PersonRepository
import no.nav.dagpenger.behandling.db.VurderingRepository
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.behandling.hendelser.VedtakStansetHendelse
import no.nav.dagpenger.behandling.iverksett.IverksettClient
import no.nav.dagpenger.behandling.iverksett.IverksettDTOBuilder
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class Mediator(
    private val rapidsConnection: RapidsConnection,
    private val oppgaveRepository: OppgaveRepository,
    private val personRepository: PersonRepository,
    private val behandlingRepository: BehandlingRepository,
    private val aktivitetsloggMediator: AktivitetsloggMediator,
    private val iverksettClient: IverksettClient,
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
        publiserBehovForVilkårsvurderingAvMinsteInntekt(hendelse, oppgave)
    }

    private fun publiserBehovForVilkårsvurderingAvMinsteInntekt(
        hendelse: SøknadInnsendtHendelse,
        oppgave: Oppgave,
    ) {
        val ident = oppgave.person.ident
        val stegUUID = oppgave.steg("Oppfyller kravet til minste inntekt").uuid
        rapidsConnection.publish(
            key = ident,
            message =
                JsonMessage.newNeed(
                    listOf("VurderingAvMinsteInntekt"),
                    mapOf(
                        "ident" to ident,
                        "oppgaveUUID" to oppgave.uuid,
                        "stegUUID" to stegUUID,
                        "virkningsdato" to hendelse.innsendtDato,
                    ),
                ).toJson(),
        )
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

    override fun vedtakFattet(
        vedtakFattetEvent: VedtakFattet,
        kommando: UtførStegKommando,
    ) {
        val behandling = behandlingRepository.hentBehandling(vedtakFattetEvent.behandlingId)
        iverksettClient.iverksett(subjectToken = kommando.token, iverksettDto = IverksettDTOBuilder(behandling).bygg())
        logger.info { "Rammevedtak med behandlingId ${behandling.uuid} er sendt til iverksetting" }

        publishEvent("rettighet_behandlet_hendelse", vedtakFattetEvent).also {
            logger.info {
                "Publiserer rettighet_behandlet_hendelse for behandlingId=${vedtakFattetEvent.behandlingId}"
            }
        }
    }

    private fun publishEvent(
        navn: String,
        event: BehandlingObserver.BehandlingEvent,
    ) = rapidsConnection.publish(event.ident, JsonMessage.newMessage(navn, event.toMap()).toJson())
}
