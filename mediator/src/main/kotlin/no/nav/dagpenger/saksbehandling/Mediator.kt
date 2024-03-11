package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.api.AvbrytBehandlingHendelse
import no.nav.dagpenger.saksbehandling.api.BekreftOppgaveHendelse
import no.nav.dagpenger.saksbehandling.api.OppdaterOppgaveHendelse
import no.nav.dagpenger.saksbehandling.api.alderskravStegFra
import no.nav.dagpenger.saksbehandling.api.minsteinntektStegFra
import no.nav.dagpenger.saksbehandling.db.Repository
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.maskinell.BehandlingKlient

private val logger = KotlinLogging.logger {}
val sikkerLogger = KotlinLogging.logger("tjenestekall")

internal class Mediator(
    private val repository: Repository,
    private val behandlingKlient: BehandlingKlient,
) : Repository by repository {

    fun behandle(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        val person = repository.finnPerson(søknadsbehandlingOpprettetHendelse.ident) ?: Person(
            ident = søknadsbehandlingOpprettetHendelse.ident,
        )

        val behandling = Behandling(
            behandlingId = søknadsbehandlingOpprettetHendelse.behandlingId,
            person = person,
            opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
        )

        behandling.håndter(søknadsbehandlingOpprettetHendelse)
        lagre(behandling)
    }

    fun behandle(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        this.hentBehandling(forslagTilVedtakHendelse.behandlingId).let { behandling ->
            behandling.håndter(forslagTilVedtakHendelse)
            lagre(behandling)
        }
    }

    fun hentOppgaverKlarTilBehandling(): List<Oppgave> {
        return repository.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)
    }

    suspend fun oppdaterOppgaveMedSteg(hendelse: OppdaterOppgaveHendelse): Oppgave? {
        val oppgave = repository.hentOppgave(hendelse.oppgaveId)
        return when (oppgave) {
            null -> null
            else -> {
                val behandling = hentBehandlingFra(oppgave.oppgaveId)

                val behandlingDTO = kotlin.runCatching {
                    behandlingKlient.hentBehandling(
                        behandlingId = behandling.behandlingId,
                        saksbehandlerToken = hendelse.saksbehandlerSignatur,
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
        val oppgave = repository.hentOppgave(hendelse.oppgaveId)
        when (oppgave) {
            null -> return null
            else -> {
                val behandling = hentBehandlingFra(oppgave.oppgaveId)
                kotlin.runCatching {
                    behandlingKlient.bekreftBehandling(
                        behandlingId = behandling.behandlingId,
                        saksbehandlerToken = hendelse.saksbehandlerSignatur,
                    )
                }
                oppgave.tilstand = FERDIG_BEHANDLET
                sikkerLogger.info { "Bekreftet oppgaveId: ${oppgave.oppgaveId}, behandlingId: ${behandling.behandlingId}" }
            }
        }
        return oppgave
    }

    suspend fun avbrytBehandling(hendelse: AvbrytBehandlingHendelse): Oppgave? {
        val oppgave = repository.hentOppgave(hendelse.oppgaveId)
        when (oppgave) {
            null -> return null
            else -> {
                // TODO kall behandlingKlient.avbrytBehandling

                val behandling = hentBehandlingFra(oppgave.oppgaveId)
                oppgave.tilstand = FERDIG_BEHANDLET
                sikkerLogger.info { "Avbrutt oppgaveId: ${oppgave.oppgaveId}, behandlingId: ${behandling.behandlingId}" }
            }
        }
        return oppgave
    }
}
