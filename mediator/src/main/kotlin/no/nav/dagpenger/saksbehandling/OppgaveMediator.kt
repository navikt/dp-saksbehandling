package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKreverIkkeTotrinnskontrollException
import no.nav.dagpenger.saksbehandling.behandling.GodkjennBehandlingFeiletException
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingLåstHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpplåstHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.IkkeRelevantAvklaringHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

class OppgaveMediator(
    private val repository: OppgaveRepository,
    private val skjermingKlient: SkjermingKlient,
    private val pdlKlient: PDLKlient,
    private val behandlingKlient: BehandlingKlient,
    private val utsendingMediator: UtsendingMediator,
) {
    private lateinit var rapidsConnection: RapidsConnection

    fun setRapidsConnection(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

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
                emneknagger = emptySet(),
                opprettet = søknadsbehandlingOpprettetHendelse.opprettet,
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
                withLoggingContext("oppgaveId" to oppgave.oppgaveId.toString()) {
                    oppgave.oppgaveKlarTilBehandling(forslagTilVedtakHendelse).let {
                        when (it) {
                            true -> {
                                repository.lagre(oppgave)
                                logger.info {
                                    "Mottatt hendelse forslag_til_vedtak for behandling med id " +
                                        "${forslagTilVedtakHendelse.behandlingId}. Oppgavens tilstand er" +
                                        " ${oppgave.tilstand().type}"
                                }
                            }

                            false -> {
                                logger.info {
                                    "Mottatt hendelse forslag_til_vedtak for behandling med id " +
                                        "${forslagTilVedtakHendelse.behandlingId}. Oppgavens tilstand er uendret" +
                                        " ${oppgave.tilstand().type}"
                                }
                            }
                        }
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

    fun sendTilKontroll(
        sendTilKontrollHendelse: SendTilKontrollHendelse,
        saksbehandlerToken: String,
    ) {
        repository.hentOppgave(sendTilKontrollHendelse.oppgaveId).also { oppgave ->

            runBlocking {
                behandlingKlient.kreverTotrinnskontroll(
                    oppgave.behandling.behandlingId,
                    saksbehandlerToken = saksbehandlerToken,
                ).onSuccess { result ->
                    when (result) {
                        true -> {
                            oppgave.sendTilKontroll(sendTilKontrollHendelse)
                            rapidsConnection.publish(
                                key = oppgave.behandling.person.ident,
                                JsonMessage.newMessage(
                                    eventName = "oppgave_sendt_til_kontroll",
                                    map =
                                        mapOf(
                                            "behandlingId" to oppgave.behandling.behandlingId,
                                            "ident" to oppgave.behandling.person.ident,
                                        ),
                                ).toJson(),
                            )
                            repository.lagre(oppgave)
                        }

                        false -> {
                            throw BehandlingKreverIkkeTotrinnskontrollException("Behandling krever ikke totrinnskontroll")
                        }
                    }
                }.onFailure {
                    logger.error { "Feil ved henting av behandling med id ${oppgave.behandling.behandlingId}: ${it.message}" }
                }
            }
        }
    }

    fun returnerTilSaksbehandling(returnerTilSaksbehandlingHendelse: ReturnerTilSaksbehandlingHendelse) {
        repository.hentOppgave(returnerTilSaksbehandlingHendelse.oppgaveId).also { oppgave ->
            oppgave.returnerTilSaksbehandling(returnerTilSaksbehandlingHendelse)
            rapidsConnection.publish(
                key = oppgave.behandling.person.ident,
                JsonMessage.newMessage(
                    eventName = "oppgave_returnert_til_saksbehandling",
                    map =
                        mapOf(
                            "behandlingId" to oppgave.behandling.behandlingId,
                            "ident" to oppgave.behandling.person.ident,
                        ),
                ).toJson(),
            )
            repository.lagre(oppgave)
        }
    }

    fun settOppgaveUnderBehandling(behandlingOpplåstHendelse: BehandlingOpplåstHendelse) {
        val oppgave = repository.finnOppgaveFor(behandlingOpplåstHendelse.behandlingId)
        when (oppgave) {
            null -> {
                logger.error {
                    "Mottatt hendelse behandling_opplåst for behandling med id " +
                        "${behandlingOpplåstHendelse.behandlingId}." +
                        "Fant ikke oppgave for behandlingen. Gjør derfor ingenting med hendelsen."
                }
            }

            else -> {
                oppgave.klarTilBehandling(behandlingOpplåstHendelse)
                repository.lagre(oppgave)
                withLoggingContext("oppgaveId" to oppgave.oppgaveId.toString()) {
                    logger.info {
                        "Mottatt hendelse behandling_opplåst for behandling med id " +
                            "${behandlingOpplåstHendelse.behandlingId}. Oppgave er klar til ny behandling."
                    }
                }
            }
        }
    }

    fun lagreNotat(notatHendelse: NotatHendelse): LocalDateTime {
        return repository.hentOppgave(notatHendelse.oppgaveId).let { oppgave ->
            oppgave.lagreNotat(notatHendelse)
            repository.lagreNotatFor(oppgave)
        }
    }

    fun settOppgaveKlarTilKontroll(behandlingLåstHendelse: BehandlingLåstHendelse) {
        val oppgave = repository.finnOppgaveFor(behandlingLåstHendelse.behandlingId)
        when (oppgave) {
            null -> {
                logger.error {
                    "Mottatt hendelse behandling_låst for behandling med id " +
                        "${behandlingLåstHendelse.behandlingId}." +
                        "Fant ikke oppgave for behandlingen. Gjør derfor ingenting med hendelsen."
                }
            }

            else -> {
                oppgave.klarTilKontroll(behandlingLåstHendelse)
                repository.lagre(oppgave)
                withLoggingContext("oppgaveId" to oppgave.oppgaveId.toString()) {
                    logger.info {
                        "Mottatt hendelse behandling_låst for behandling med id " +
                            "${behandlingLåstHendelse.behandlingId}. Oppgave er klar til kontroll."
                    }
                }
            }
        }
    }

    fun ferdigstillOppgave(vedtakFattetHendelse: VedtakFattetHendelse): Oppgave {
        return repository.hentOppgaveFor(vedtakFattetHendelse.behandlingId).also { oppgave ->
            withLoggingContext("oppgaveId" to oppgave.oppgaveId.toString()) {
                logger.info {
                    "Mottatt hendelse vedtak_fattet for behandling med id ${vedtakFattetHendelse.behandlingId}. " +
                        "Oppgave ferdigstilt."
                }
                oppgave.ferdigstill(vedtakFattetHendelse).let {
                    when (it) {
                        true -> repository.lagre(oppgave)
                        false -> logger.warn { "Oppgave med id ${oppgave.oppgaveId} er allerede ferdigstilt" }
                    }
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
                    oppgave.behandling.person.ident,
                )

            behandlingKlient.godkjennBehandling(
                behandlingId = oppgave.behandling.behandlingId,
                ident = oppgave.behandling.person.ident,
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
                behandlingId = oppgave.behandling.behandlingId,
                ident = oppgave.behandling.person.ident,
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
        logger.info { "Mottatt behandling avbrutt hendelse for behandling med id ${hendelse.behandlingId}." }
        repository.finnOppgaveFor(hendelse.behandlingId)?.let { oppgave ->
            if (oppgave.tilstand() == Oppgave.Opprettet) {
                repository.slettBehandling(hendelse.behandlingId)
            } else {
                logger.info { "Behandling med id ${hendelse.behandlingId} behandles i Arena" }
                oppgave.behandlesIArena(hendelse)
                repository.lagre(oppgave)
            }
        }
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
