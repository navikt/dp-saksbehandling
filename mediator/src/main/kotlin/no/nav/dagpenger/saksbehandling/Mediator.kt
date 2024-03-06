package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.saksbehandling.api.AvbrytBehandlingHendelse
import no.nav.dagpenger.saksbehandling.api.BekreftOppgaveHendelse
import no.nav.dagpenger.saksbehandling.api.OppdaterOppgaveHendelse
import no.nav.dagpenger.saksbehandling.api.alderskravStegFra
import no.nav.dagpenger.saksbehandling.api.config.objectMapper
import no.nav.dagpenger.saksbehandling.api.minsteinntektStegFra
import no.nav.dagpenger.saksbehandling.api.mockSøknadBehandlingId
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.maskinell.BehandlingKlient
import java.io.FileNotFoundException

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
    fun behandle(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        this.hent(forslagTilVedtakHendelse.ident)?.let { person ->
            person.håndter(forslagTilVedtakHendelse)
            lagre(person)
        } ?: throw IllegalArgumentException("Fant ikke person") // todo
    }

    override fun hentAlleOppgaver(): List<Oppgave> {
        return personRepository.hentAlleOppgaverMedTilstand(Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING)
    }

    suspend fun oppdaterOppgaveMedSteg(hendelse: OppdaterOppgaveHendelse): Oppgave? {
        val oppgave = personRepository.hent(hendelse.oppgaveId)
        return when (oppgave) {
            null -> null
            else -> {
                val behandlingDTO =
                    when (oppgave.behandlingId) {
                        mockSøknadBehandlingId -> {
                            logger.info { "Bruker mockdata for behandlingId $mockSøknadBehandlingId" }
                            behandlingResponseMock()
                        }

                        else ->
                            kotlin.runCatching {
                                behandlingKlient.hentBehandling(oppgave.behandlingId, hendelse.saksbehandlerSignatur)
                            }.getOrNull()
                    }

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
        val oppgave = personRepository.hent(hendelse.oppgaveId)
        when (oppgave) {
            null -> return null
            else -> {
                kotlin.runCatching {
                    behandlingKlient.bekreftBehandling(oppgave.behandlingId, hendelse.saksbehandlerSignatur)
                }
                // TODO Skal den ha getOrNull()????
            }
        }
        oppgave.tilstand = Oppgave.Tilstand.Type.FERDIG_BEHANDLET
        sikkerLogger.info { "Bekreftet oppgaveId: ${oppgave.oppgaveId}, behandlingId: ${oppgave.behandlingId}" }
        return oppgave
    }

    suspend fun avbrytBehandling(hendelse: AvbrytBehandlingHendelse): Oppgave? {
        val oppgave = personRepository.hent(hendelse.oppgaveId)
        when (oppgave) {
            null -> return null
            else -> {
                // TODO kall behandlingKlient.avbrytBehandling
            }
        }
        oppgave.tilstand = Oppgave.Tilstand.Type.FERDIG_BEHANDLET
        sikkerLogger.info { "Avbrutt oppgaveId: ${oppgave.oppgaveId}, behandlingId: ${oppgave.behandlingId}" }
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
