package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.api.OppdaterOppgaveHendelse
import no.nav.dagpenger.saksbehandling.api.alderskravStegFra
import no.nav.dagpenger.saksbehandling.api.minsteinntektStegFra
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.maskinell.BehandlingKlient

private val logger = KotlinLogging.logger {}
val sikkerLogger = KotlinLogging.logger("tjenestekall")

internal class Mediator(
    private val personRepository: PersonRepository,
    private val behandlingKlient: BehandlingKlient,
) :
    PersonRepository by personRepository {
    fun behandle(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        val ident = søknadsbehandlingOpprettetHendelse.ident
        val person = hent(ident) ?: Person(ident)
        person.håndter(søknadsbehandlingOpprettetHendelse)
        lagre(person)
    }

    suspend fun oppdaterOppgaveMedSteg(hendelse: OppdaterOppgaveHendelse): Oppgave? {
        val oppgave = personRepository.hent(hendelse.oppgaveId)
        return when (oppgave) {
            null -> null
            else -> {
                val behandlingDTO =
                    kotlin.runCatching {
                        behandlingKlient.hentBehandling(oppgave.behandlingId, hendelse.saksbehandlerSignatur)
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
}
