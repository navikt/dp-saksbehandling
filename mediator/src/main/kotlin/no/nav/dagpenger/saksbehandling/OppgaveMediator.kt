package no.nav.dagpenger.saksbehandling

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
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
import no.nav.dagpenger.saksbehandling.hendelser.ToTrinnskontrollHendelse
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
            logger.warn {
                "Mottatt hendelse behandling_opprettet, men behandling med id " +
                    "${søknadsbehandlingOpprettetHendelse.behandlingId} finnes allerede."
            }
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

        withLoggingContext("oppgaveId" to oppgave.oppgaveId.toString()) {
            logger.info {
                "Mottatt hendelse behandling_opprettet med id ${behandling.behandlingId}. Oppgave opprettet."
            }
        }
    }

    fun settOppgaveKlarTilBehandling(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        val oppgave = finnOppgaveFor(forslagTilVedtakHendelse.behandlingId)
        when (oppgave) {
            null -> {
                logger.warn {
                    "Mottatt hendelse forslag_til_vedtak for behandling med id " +
                        "${forslagTilVedtakHendelse.behandlingId}." +
                        "Fant ikke oppgave for behandlingen. Gjør derfor ingenting med hendelsen."
                }
            }

            else -> {
                oppgave.oppgaveKlarTilBehandling(forslagTilVedtakHendelse)
                lagre(oppgave)
                withLoggingContext("oppgaveId" to oppgave.oppgaveId.toString()) {
                    logger.info {
                        "Mottatt hendelse forslag_til_vedtak for behandling med id " +
                            "${forslagTilVedtakHendelse.behandlingId}. Oppgave er klar til behandling. Emneknagger: ${oppgave.emneknagger.joinToString()}"
                    }
                }
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

    fun gjørKlarTilKontroll(klarTilKontrollHendelse: KlarTilKontrollHendelse) {
        repository.hentOppgave(klarTilKontrollHendelse.oppgaveId).also { oppgave ->
            oppgave.gjørKlarTilKontroll(klarTilKontrollHendelse)
            repository.lagre(oppgave)
        }
    }

    fun ferdigstillOppgave(vedtakFattetHendelse: VedtakFattetHendelse): Oppgave {
        return hentOppgaveFor(vedtakFattetHendelse.behandlingId).also { oppgave ->
            oppgave.ferdigstill(vedtakFattetHendelse)
            lagre(oppgave)
            withLoggingContext("oppgaveId" to oppgave.oppgaveId.toString()) {
                logger.info {
                    "Mottatt hendelse vedtak_fattet for behandling med id ${vedtakFattetHendelse.behandlingId}. " +
                        "Oppgave ferdigstilt."
                }
            }
        }
    }

    fun ferdigstillOppgave(
        godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        saksbehandlerToken: String,
    ) {
        repository.hentOppgave(godkjentBehandlingHendelse.oppgaveId).let { oppgave ->

            behandlingKlient.godkjennBehandling(
                behandlingId = oppgave.behandlingId,
                ident = oppgave.ident,
                saksbehandlerToken = saksbehandlerToken,
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

    fun ferdigstillOppgave(
        godkjennBehandlingMedBrevIArena: GodkjennBehandlingMedBrevIArena,
        saksbehandlerToken: String,
    ) {
        repository.hentOppgave(godkjennBehandlingMedBrevIArena.oppgaveId).let { oppgave ->

            behandlingKlient.godkjennBehandling(
                behandlingId = oppgave.behandlingId,
                ident = oppgave.ident,
                saksbehandlerToken = saksbehandlerToken,
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

    fun tildelTotrinnskontroll(toTrinnskontrollHendelse: ToTrinnskontrollHendelse) {
        repository.hentOppgave(toTrinnskontrollHendelse.oppgaveId).also { oppgave ->
            oppgave.tildelTotrinnskontroll(toTrinnskontrollHendelse)
            repository.lagre(oppgave)
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
