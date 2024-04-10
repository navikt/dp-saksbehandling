package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.api.OppdaterOppgaveHendelse
import no.nav.dagpenger.saksbehandling.api.models.KjonnDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonDTO
import no.nav.dagpenger.saksbehandling.db.Repository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient

val logger = KotlinLogging.logger {}

internal class Mediator(
    private val repository: Repository,
    private val pdlKlient: PDLKlient,
) : Repository by repository {

    fun behandle(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        val person = repository.finnPerson(søknadsbehandlingOpprettetHendelse.ident) ?: Person(
            ident = søknadsbehandlingOpprettetHendelse.ident,
        )

        if (repository.finnBehandling(søknadsbehandlingOpprettetHendelse.behandlingId) != null) {
            logger.info { "Behandling med id ${søknadsbehandlingOpprettetHendelse.behandlingId} finnes allerede." }
            return
        }

        val behandling = Behandling(
            behandlingId = søknadsbehandlingOpprettetHendelse.behandlingId,
            person = person,
            opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
        )

        behandling.håndter(søknadsbehandlingOpprettetHendelse)
        lagre(behandling)
        logger.info { "Mottatt søknadsbehandling med id ${behandling.behandlingId}" }
    }

    fun behandle(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        this.hentBehandling(forslagTilVedtakHendelse.behandlingId).let { behandling ->
            behandling.håndter(forslagTilVedtakHendelse)
            lagre(behandling)
            logger.info { "Mottatt forslag til vedtak hendelse for behandling med id ${behandling.behandlingId}" }
        }
    }

    fun hentOppgaverKlarTilBehandling(): List<Oppgave> {
        return repository.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)
    }

    suspend fun lagOppgaveDTO(hendelse: OppdaterOppgaveHendelse): OppgaveDTO? {
        val oppgave = repository.hentOppgave(hendelse.oppgaveId)
        return when (oppgave) {
            null -> null
            else -> {
                val person = pdlKlient.person(oppgave.ident).getOrThrow()

                OppgaveDTO(
                    oppgaveId = oppgave.oppgaveId,
                    behandlingId = oppgave.behandlingId,
                    personIdent = oppgave.ident,
                    person = PersonDTO(
                        ident = person.ident,
                        fornavn = person.fornavn,
                        etternavn = person.etternavn,
                        mellomnavn = person.mellomnavn,
                        fodselsdato = person.fødselsdato,
                        alder = person.alder,
                        kjonn = when (person.kjønn) {
                            PDLPerson.Kjonn.MANN -> KjonnDTO.MANN
                            PDLPerson.Kjonn.KVINNE -> KjonnDTO.KVINNE
                            PDLPerson.Kjonn.UKJENT -> KjonnDTO.UKJENT
                        },
                        statsborgerskap = person.statsborgerskap,
                    ),
                    tidspunktOpprettet = oppgave.opprettet,
                    emneknagger = oppgave.emneknagger.toList(),
                    tilstand = oppgave.tilstand.tilOppgaveTilstandDTO(),
                    journalpostIder = listOf(),
                )
            }
        }
    }

    fun avsluttBehandling(hendelse: VedtakFattetHendelse) {
        repository.hentBehandling(hendelse.behandlingId).let { behandling ->
            behandling.håndter(hendelse)
            lagre(behandling)
            logger.info { "Mottatt vedtak fattet hendelse for behandling med id ${behandling.behandlingId}. Behandling avsluttet." }
        }
    }

    fun avbrytOppgave(hendelse: BehandlingAvbruttHendelse) {
        repository.slettBehandling(hendelse.behandlingId)
        logger.info { "Mottatt behandling avbrutt hendelse for behandling med id ${hendelse.behandlingId}. Behandling slettet." }
    }

    private fun Oppgave.Tilstand.Type.tilOppgaveTilstandDTO() =
        when (this) {
            Oppgave.Tilstand.Type.OPPRETTET -> OppgaveTilstandDTO.OPPRETTET
            Oppgave.Tilstand.Type.FERDIG_BEHANDLET -> OppgaveTilstandDTO.FERDIG_BEHANDLET
            Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING -> OppgaveTilstandDTO.KLAR_TIL_BEHANDLING
        }
}
