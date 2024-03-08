package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.api.AvbrytBehandlingHendelse
import no.nav.dagpenger.saksbehandling.api.BekreftOppgaveHendelse
import no.nav.dagpenger.saksbehandling.api.OppdaterOppgaveHendelse
import no.nav.dagpenger.saksbehandling.api.alderskravStegFra
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import no.nav.dagpenger.saksbehandling.api.minsteinntektStegFra
import no.nav.dagpenger.saksbehandling.db.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.PersonRepository
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.maskinell.BehandlingKlient
import java.io.FileNotFoundException

private val logger = KotlinLogging.logger {}
val sikkerLogger = KotlinLogging.logger("tjenestekall")

internal class Mediator(
    private val personRepository: PersonRepository,
    private val oppgaveRepository: OppgaveRepository,
    private val behandlingKlient: BehandlingKlient,
) : PersonRepository by personRepository, OppgaveRepository by oppgaveRepository {

    fun behandle(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        val person = personRepository.hentBehandlingFra(søknadsbehandlingOpprettetHendelse.ident) ?: Person(
            søknadsbehandlingOpprettetHendelse.ident
        )

        val behandling = Behandling(
            behandlingId = søknadsbehandlingOpprettetHendelse.behandlingId,
            person = person,
        )

        behandling.håndter(søknadsbehandlingOpprettetHendelse)
        lagre(behandling)
    }

    fun behandle(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        this.hentBehandlingFra(forslagTilVedtakHendelse.behandlingId).let { behandling ->
            behandling.håndter(forslagTilVedtakHendelse)
            lagre(behandling)
        }
    }

    fun hentOppgaverKlarTilBehandling(): List<Oppgave> {
        return oppgaveRepository.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)
    }

    suspend fun oppdaterOppgaveMedSteg(hendelse: OppdaterOppgaveHendelse): Oppgave? {
        val oppgave = oppgaveRepository.hent(hendelse.oppgaveId)
        return when (oppgave) {
            null -> null
            else -> {
                val behandling = hentBehandling(oppgave.oppgaveId)

                val behandlingDTO = kotlin.runCatching {
                    behandlingKlient.hentBehandling(
                        behandlingId = behandling.behandlingId,
                        saksbehandlerToken = hendelse.saksbehandlerSignatur
                    )
                }.getOrNull()

                val nyeSteg = mutableListOf<Steg>()
                minsteinntektStegFra(behandlingDTO)?.let { nyeSteg.add(it) }
                alderskravStegFra(behandlingDTO)?.let { nyeSteg.add(it) }

                val oppdatertOppgave = oppgave.copy(steg = nyeSteg)
                sikkerLogger.info { "Oppdatert oppgave: $oppdatertOppgave" }
                return oppdatertOppgave
            }
        }
    }

    suspend fun bekreftOppgavensOpplysninger(hendelse: BekreftOppgaveHendelse): Oppgave? {
        val oppgave = oppgaveRepository.hent(hendelse.oppgaveId)
        when (oppgave) {
            null -> return null
            else -> {
                val behandling = hentBehandling(oppgave.oppgaveId)
                kotlin.runCatching {
                    behandlingKlient.bekreftBehandling(
                        behandlingId = behandling.behandlingId,
                        saksbehandlerToken = hendelse.saksbehandlerSignatur
                    )
                }
                oppgave.tilstand = FERDIG_BEHANDLET
                sikkerLogger.info { "Bekreftet oppgaveId: ${oppgave.oppgaveId}, behandlingId: ${behandling.behandlingId}" }
            }
        }
        return oppgave
    }

    suspend fun avbrytBehandling(hendelse: AvbrytBehandlingHendelse): Oppgave? {
        val oppgave = oppgaveRepository.hent(hendelse.oppgaveId)
        when (oppgave) {
            null -> return null
            else -> {
                // TODO kall behandlingKlient.avbrytBehandling

                val behandling = hentBehandling(oppgave.oppgaveId)
                oppgave.tilstand = FERDIG_BEHANDLET
                sikkerLogger.info { "Avbrutt oppgaveId: ${oppgave.oppgaveId}, behandlingId: ${behandling.behandlingId}" }
            }
        }
        return oppgave
    }
}

// TODO: Fjernes når mocken fjernes
private fun String.fileAsText(): String {
    return object {}.javaClass.getResource(this)?.readText()
        ?: throw FileNotFoundException()
}

private fun behandlingResponseMock(): BehandlingDTO? =
    objectMapper.readValue(
        "/behandlingResponseMock.json".fileAsText(),
        BehandlingDTO::class.java,
    )
