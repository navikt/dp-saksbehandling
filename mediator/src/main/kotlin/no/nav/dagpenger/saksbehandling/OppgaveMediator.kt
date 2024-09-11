package no.nav.dagpenger.saksbehandling

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hubba
import no.nav.dagpenger.saksbehandling.hendelser.IkkeRelevantAvklaringHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient

private val logger = KotlinLogging.logger {}

class OppgaveMediator(
    private val repository: OppgaveRepository,
    private val skjermingKlient: SkjermingKlient,
    private val pdlKlient: PDLKlient,
) : OppgaveRepository by repository {
    fun opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        val person =
            repository.finnPerson(søknadsbehandlingOpprettetHendelse.ident) ?: lagPerson(
                ident = søknadsbehandlingOpprettetHendelse.ident,
            )

        if (repository.finnBehandling(søknadsbehandlingOpprettetHendelse.behandlingId) != null) {
            logger.info { "Behandling med id ${søknadsbehandlingOpprettetHendelse.behandlingId} finnes allerede." }
            return
        }

        val behandling =
            Behandling(
                behandlingId = søknadsbehandlingOpprettetHendelse.behandlingId,
                person = person,
                opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
                hendelse = søknadsbehandlingOpprettetHendelse,
            )

        val oppgave =
            Oppgave(
                oppgaveId = UUIDv7.ny(),
                emneknagger = setOf("Søknadsbehandling"),
                opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
                ident = person.ident,
                behandlingId = behandling.behandlingId,
                behandling = behandling,
            )

        lagre(oppgave)
        logger.info { "Mottatt søknadsbehandling med id ${behandling.behandlingId}" }
    }

    fun settOppgaveKlarTilBehandling(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        logger.info { "Mottatt forslag til vedtak hendelse for behandling med id ${forslagTilVedtakHendelse.behandlingId}" }
        val oppgave = finnOppgaveFor(forslagTilVedtakHendelse.behandlingId)
        when (oppgave) {
            null -> {
                logger.warn {
                    "Fant ikke oppgave for behandling med id ${forslagTilVedtakHendelse.behandlingId}. " +
                        "Gjør derfor ingenting med hendelsen"
                }
            }

            else -> {
                oppgave.oppgaveKlarTilBehandling(forslagTilVedtakHendelse)
                lagre(oppgave)
            }
        }
    }

    fun fristillOppgave(oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
        repository.hentOppgave(oppgaveAnsvarHendelse.oppgaveId).let { oppgave ->
            oppgave.fjernAnsvar(oppgaveAnsvarHendelse)
            repository.lagre(oppgave)
        }
    }

    fun tildelOppgave(oppgaveAnsvarHendelse: OppgaveAnsvarHendelse): Oppgave {
        return repository.hentOppgave(oppgaveAnsvarHendelse.oppgaveId).also { oppgave ->
            oppgave.tildel(oppgaveAnsvarHendelse)
            repository.lagre(oppgave)
        }
    }

    fun ferdigstillOppgave(vedtakFattetHendelse: VedtakFattetHendelse): Oppgave {
        logger.info {
            "Mottatt vedtak fattet hendelse for behandling med id ${vedtakFattetHendelse.behandlingId}. Oppgave ferdigstilt."
        }
        return hentOppgaveFor(vedtakFattetHendelse.behandlingId).also { oppgave ->
            oppgave.ferdigstill(vedtakFattetHendelse)
            lagre(oppgave)
        }
    }

    fun ferdigstillOppgave(godkjentBehandlingHendelse: GodkjentBehandlingHendelse) {
        logger.info { "Mottatt godkjent behandling hendelse for oppgave: ${godkjentBehandlingHendelse.oppgaveId}" }
    }

    fun ferdigstillOppgave(hubba: Hubba) {
        logger.info { "Mottatt godkjent behandling hendelse for oppgave: ${hubba.oppgaveId}" }
    }

    fun avbrytOppgave(hendelse: BehandlingAvbruttHendelse) {
        repository.slettBehandling(hendelse.behandlingId)
        logger.info { "Mottatt behandling avbrutt hendelse for behandling med id ${hendelse.behandlingId}. Behandling slettet." }
    }

    fun utsettOppgave(utsettOppgaveHendelse: UtsettOppgaveHendelse) {
        repository.hentOppgave(utsettOppgaveHendelse.oppgaveId).let { oppgave ->
            oppgave.utsett(utsettOppgaveHendelse)
            repository.lagre(oppgave)
        }
    }

    fun fjernEmneknagg(hendelse: IkkeRelevantAvklaringHendelse) {
        repository.fjerneEmneknagg(hendelse.behandlingId, hendelse.ikkeRelevantEmneknagg).let {
            when (it) {
                true -> logger.info { "Fjernet emneknagg: ${hendelse.ikkeRelevantEmneknagg} for behandlingId: ${hendelse.behandlingId}" }
                false -> logger.warn { "Fant ikke emneknagg: ${hendelse.ikkeRelevantEmneknagg} for behandlingId: ${hendelse.behandlingId}" }
            }
        }
    }

    private fun lagPerson(ident: String): Person {
        return runBlocking {
            val skjermesSomEgneAnsatte =
                async {
                    skjermingKlient.erSkjermetPerson(ident).getOrThrow()
                }

            val adresseBeskyttelseGradering =
                async {
                    pdlKlient.person(ident).getOrThrow().adresseBeskyttelseGradering
                }

            Person(
                ident = ident,
                skjermesSomEgneAnsatte = skjermesSomEgneAnsatte.await(),
                adressebeskyttelseGradering = adresseBeskyttelseGradering.await(),
            )
        }
    }
}
