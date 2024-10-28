package no.nav.dagpenger.saksbehandling

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.GodkjennBehandlingFeiletException
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.IkkeRelevantAvklaringHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlarTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import java.util.UUID

private val logger = KotlinLogging.logger {}

class OppgaveMediator(
    private val repository: OppgaveRepository,
    private val skjermingKlient: SkjermingKlient,
    private val pdlKlient: PDLKlient,
    private val behandlingKlient: BehandlingKlient,
    private val utsendingMediator: UtsendingMediator,
) {
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

        repository.lagre(oppgave)

        withLoggingContext("oppgaveId" to oppgave.oppgaveId.toString()) {
            logger.info {
                "Mottatt hendelse behandling_opprettet med id ${behandling.behandlingId}. Oppgave opprettet."
            }
        }
    }

    fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave> {
        return repository.hentAlleOppgaverMedTilstand(tilstand)
    }

    fun hentOppgave(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave {
        return repository.hentOppgave(oppgaveId).also { oppgave ->
            oppgave.egneAnsatteTilgangskontroll(saksbehandler)
            oppgave.adressebeskyttelseTilgangskontroll(saksbehandler)
        }
    }

    fun settOppgaveKlarTilBehandling(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        val oppgave = repository.finnOppgaveFor(forslagTilVedtakHendelse.behandlingId)
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
                repository.lagre(oppgave)
                withLoggingContext("oppgaveId" to oppgave.oppgaveId.toString()) {
                    logger.info {
                        "Mottatt hendelse forslag_til_vedtak for behandling med id " +
                            "${forslagTilVedtakHendelse.behandlingId}. Oppgave er klar til behandling. " +
                            "Emneknagger: ${oppgave.emneknagger.joinToString()}"
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

    fun tildelOppgave(settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse): Oppgave.Tilstand {
        return repository.hentOppgave(settOppgaveAnsvarHendelse.oppgaveId).also { oppgave ->
            oppgave.tildel(settOppgaveAnsvarHendelse)
            repository.lagre(oppgave)
        }.tilstand()
    }

    fun sendTilKontroll(klarTilKontrollHendelse: KlarTilKontrollHendelse) {
        repository.hentOppgave(klarTilKontrollHendelse.oppgaveId).also { oppgave ->
            oppgave.sendTilKontroll(klarTilKontrollHendelse)
            repository.lagre(oppgave)
        }
    }

    fun lagreNotat(notatHendelse: NotatHendelse) {
        repository.hentOppgave(notatHendelse.oppgaveId).also { oppgave ->
            oppgave.lagreNotat(notatHendelse)
            repository.lagre(oppgave)
        }
    }

    fun ferdigstillOppgave(vedtakFattetHendelse: VedtakFattetHendelse): Oppgave {
        return repository.hentOppgaveFor(vedtakFattetHendelse.behandlingId).also { oppgave ->
            oppgave.ferdigstill(vedtakFattetHendelse)
            repository.lagre(oppgave)
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
            oppgave.ferdigstill(godkjentBehandlingHendelse)

            val utsendingID =
                utsendingMediator.opprettUtsending(
                    oppgave.oppgaveId,
                    godkjentBehandlingHendelse.meldingOmVedtak,
                    oppgave.ident,
                )

            behandlingKlient.godkjennBehandling(
                behandlingId = oppgave.behandlingId,
                ident = oppgave.ident,
                saksbehandlerToken = saksbehandlerToken,
            ).onSuccess {
                repository.lagre(oppgave)
            }.onFailure {
                val feil = "Feil ved godkjenning av behandling: ${it.message}"
                logger.error { feil }
                utsendingMediator.slettUtsending(utsendingID).also { rowsDeleted ->
                    when (rowsDeleted) {
                        1 -> logger.info { "Slettet utsending med id $utsendingID" }
                        else -> logger.error { "Fant ikke utsending med id $utsendingID" }
                    }
                }
                throw GodkjennBehandlingFeiletException(feil)
            }
        }
    }

    fun ferdigstillOppgave(
        godkjennBehandlingMedBrevIArena: GodkjennBehandlingMedBrevIArena,
        saksbehandlerToken: String,
    ) {
        repository.hentOppgave(godkjennBehandlingMedBrevIArena.oppgaveId).let { oppgave ->
            oppgave.ferdigstill(godkjennBehandlingMedBrevIArena)

            behandlingKlient.godkjennBehandling(
                behandlingId = oppgave.behandlingId,
                ident = oppgave.ident,
                saksbehandlerToken = saksbehandlerToken,
            ).onSuccess {
                repository.lagre(oppgave)
            }.onFailure {
                val feil = "Feil ved godkjenning av behandling: ${it.message}"
                throw GodkjennBehandlingFeiletException(feil)
            }
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

    fun hentOppgaveIdFor(behandlingId: UUID): UUID? = repository.hentOppgaveIdFor(behandlingId)

    fun finnOppgaverFor(ident: String): List<Oppgave> = repository.finnOppgaverFor(ident)

    fun søk(søkefilter: Søkefilter): List<Oppgave> = repository.søk(søkefilter)

    fun tildelOgHentNesteOppgave(
        nesteOppgaveHendelse: NesteOppgaveHendelse,
        queryString: String,
    ): Oppgave? {
        val tildelNesteOppgaveFilter =
            TildelNesteOppgaveFilter.fra(
                queryString = queryString,
                saksbehandler = nesteOppgaveHendelse.utførtAv,
            )
        return repository.tildelOgHentNesteOppgave(nesteOppgaveHendelse, tildelNesteOppgaveFilter)
    }
}
