package no.nav.dagpenger.saksbehandling

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.IkkeRelevantAvklaringHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlarTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator

private val logger = KotlinLogging.logger {}

class OppgaveMediator(
    private val repository: OppgaveRepository,
    private val skjermingKlient: SkjermingKlient,
    private val pdlKlient: PDLKlient,
    private val behandlingKlient: BehandlingKlient,
    private val utsendingMediator: UtsendingMediator,
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

    fun fristillOppgave(fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse) {
        repository.hentOppgave(fjernOppgaveAnsvarHendelse.oppgaveId)
            .let { oppgave ->
                oppgave.fjernAnsvar(fjernOppgaveAnsvarHendelse)
                repository.lagre(oppgave)
            }
    }

    fun tildelOppgave(settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse): Oppgave {
        return repository.hentOppgave(settOppgaveAnsvarHendelse.oppgaveId).also { oppgave ->
            oppgave.tildel(settOppgaveAnsvarHendelse)
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
        repository.hentOppgave(godkjentBehandlingHendelse.oppgaveId).let { oppgave ->

            behandlingKlient.godkjennBehandling(
                behandlingId = oppgave.behandlingId,
                ident = oppgave.ident,
                saksbehandlerToken = godkjentBehandlingHendelse.saksbehandlerToken,
            )
            utsendingMediator.opprettUtsending(
                oppgave.oppgaveId,
                godkjentBehandlingHendelse.meldingOmVedtak,
                oppgave.ident,
            )
            oppgave.ferdigstill(godkjentBehandlingHendelse)
            repository.lagre(oppgave)
        }
    }

    fun ferdigstillOppgave(godkjennBehandlingMedBrevIArena: GodkjennBehandlingMedBrevIArena) {
        repository.hentOppgave(godkjennBehandlingMedBrevIArena.oppgaveId).let { oppgave ->

            behandlingKlient.godkjennBehandling(
                behandlingId = oppgave.behandlingId,
                ident = oppgave.ident,
                saksbehandlerToken = godkjennBehandlingMedBrevIArena.saksbehandlerToken,
            )
            oppgave.ferdigstill(godkjennBehandlingMedBrevIArena)
            repository.lagre(oppgave)
        }
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

    fun gjørKlarTilKontroll(klarTilKontrollHendelse: KlarTilKontrollHendelse) {
        TODO("Not yet implemented")
    }
}
