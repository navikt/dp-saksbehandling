package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.api.AvbrytBehandlingHendelse
import no.nav.dagpenger.saksbehandling.api.GodkjennBehandlingHendelse
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

        // TODO: mer elegant duplikatsjekk
        if (repository.finnBehandling(søknadsbehandlingOpprettetHendelse.behandlingId) != null) {
            sikkerLogger.info { "Behandling med id ${søknadsbehandlingOpprettetHendelse.behandlingId} finnes allerede." }
            return
        }

        val behandling = Behandling(
            behandlingId = søknadsbehandlingOpprettetHendelse.behandlingId,
            person = person,
            opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
        )

        behandling.håndter(søknadsbehandlingOpprettetHendelse)
        lagre(behandling)
        sikkerLogger.info { "Mottatt søknadsbehandling med id ${behandling.behandlingId}: $søknadsbehandlingOpprettetHendelse" }
    }

    fun behandle(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        this.hentBehandling(forslagTilVedtakHendelse.behandlingId).let { behandling ->
            behandling.håndter(forslagTilVedtakHendelse)
            lagre(behandling)
            sikkerLogger.info { "Mottatt forslag til vedtak for behandling med id ${behandling.behandlingId}: $forslagTilVedtakHendelse" }
        }
    }

    fun hentOppgaverKlarTilBehandling(): List<Oppgave> {
        return repository.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)
    }

    suspend fun oppdaterOppgaveMedSteg(hendelse: OppdaterOppgaveHendelse): Pair<Oppgave, Map<String, Any>>? {
        val oppgave = repository.hentOppgave(hendelse.oppgaveId)
        return when (oppgave) {
            null -> null
            else -> {
                val behandling = hentBehandlingFra(oppgave.oppgaveId)

                val behandlingResponse = kotlin.runCatching {
                    behandlingKlient.hentBehandling(
                        behandlingId = behandling.behandlingId,
                        saksbehandlerToken = hendelse.saksbehandlerSignatur,
                    )
                }.getOrNull()
                val behandlingDTO = behandlingResponse?.first

                sikkerLogger.info { "Hentet BehandlingDTO: $behandlingResponse" }

                val nyeSteg = mutableListOf<Steg>()
                minsteinntektStegFra(behandlingDTO)?.let { nyeSteg.add(it) }
                alderskravStegFra(behandlingDTO)?.let { nyeSteg.add(it) }

                val oppdatertOppgave = oppgave.copy(steg = nyeSteg)
                sikkerLogger.info { "Oppdatert oppgave: $oppdatertOppgave" }
                return Pair(oppdatertOppgave, behandlingResponse!!.second)
            }
        }
    }

    suspend fun godkjennBehandling(hendelse: GodkjennBehandlingHendelse): Result<Int> {
        val oppgave = repository.hentOppgave(hendelse.oppgaveId)

        return when (oppgave) {
            null -> Result.failure(NoSuchElementException("Oppgave finnes ikke med id ${hendelse.oppgaveId}"))
            else -> {
                kotlin.runCatching {
                    behandlingKlient.godkjennBehandling(
                        behandlingId = oppgave.behandlingId,
                        ident = oppgave.ident,
                        saksbehandlerToken = hendelse.saksbehandlerSignatur,
                    )
                }.onSuccess {
                    oppgave.tilstand = FERDIG_BEHANDLET
                    lagre(oppgave)
                    sikkerLogger.info { "Godkjente behandling med id: ${oppgave.behandlingId}, oppgaveId: ${oppgave.oppgaveId}" }
                }.onFailure { e ->
                    sikkerLogger.error(e) { "Feilet godkjenning av behandling med id: ${oppgave.behandlingId}, oppgaveId: ${oppgave.oppgaveId}" }
                }
            }
        }
    }

    suspend fun avbrytBehandling(hendelse: AvbrytBehandlingHendelse): Result<Int> {
        val oppgave = repository.hentOppgave(hendelse.oppgaveId)

        return when (oppgave) {
            null -> Result.failure(NoSuchElementException("Oppgave finnes ikke med id ${hendelse.oppgaveId}"))
            else -> {
                kotlin.runCatching {
                    behandlingKlient.avbrytBehandling(
                        behandlingId = oppgave.behandlingId,
                        ident = oppgave.ident,
                        saksbehandlerToken = hendelse.saksbehandlerSignatur,
                    )
                }.onSuccess {
                    oppgave.tilstand = FERDIG_BEHANDLET
                    lagre(oppgave)
                    sikkerLogger.info { "Avbrutt behandling med id: ${oppgave.behandlingId}, oppgaveId: ${oppgave.oppgaveId}" }
                }.onFailure { e ->
                    sikkerLogger.error(e) { "Feilet avbryting av behandling med id: ${oppgave.behandlingId}, oppgaveId: ${oppgave.oppgaveId}" }
                }
            }
        }
    }
}
