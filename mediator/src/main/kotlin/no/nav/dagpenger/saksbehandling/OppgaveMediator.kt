package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveAlertManager.OppgaveAlertType.OPPGAVE_IKKE_FUNNET
import no.nav.dagpenger.saksbehandling.OppgaveAlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKreverIkkeTotrinnskontrollException
import no.nav.dagpenger.saksbehandling.behandling.GodkjennBehandlingFeiletException
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository.OppgaveSøkResultat
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
        withLoggingContext(
            "oppgaveId" to oppgave.oppgaveId.toString(),
            "behandlingId" to oppgave.behandling.behandlingId.toString(),
        ) {
            logger.info {
                "Mottatt og behandlet hendelse behandling_opprettet. Oppgave opprettet."
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
                val feilmelding =
                    "Mottatt hendelse forslag_til_vedtak for behandling med id " +
                        "${forslagTilVedtakHendelse.behandlingId}." +
                        "Fant ikke oppgave for behandlingen. Gjør derfor ingenting med hendelsen."
                logger.error { feilmelding }
                sendAlertTilRapid(OPPGAVE_IKKE_FUNNET, feilmelding)
            }

            else -> {
                withLoggingContext(
                    "oppgaveId" to oppgave.oppgaveId.toString(),
                    "behandlingId" to forslagTilVedtakHendelse.behandlingId.toString(),
                ) {
                    logger.info {
                        "Mottatt hendelse forslag_til_vedtak. Oppgavens tilstand er" +
                            " ${oppgave.tilstand().type} når hendelsen mottas."
                    }
                    oppgave.oppgaveKlarTilBehandling(forslagTilVedtakHendelse).let {
                        when (it) {
                            true -> {
                                repository.lagre(oppgave)
                                logger.info {
                                    "Behandlet hendelse forslag_til_vedtak. Oppgavens tilstand er" +
                                        " ${oppgave.tilstand().type} etter behandling."
                                }
                            }

                            false -> {
                                logger.info {
                                    "Mottatt hendelse forslag_til_vedtak. Oppgavens tilstand er uendret" +
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
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                logger.info {
                    "Mottatt SendTilKontrollHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
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
                                logger.info {
                                    "Behandlet SendTilKontrollHendelse og publisert oppgave_sendt_til_kontroll " +
                                        "event. Tilstand etter behandling: ${oppgave.tilstand().type}"
                                }
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
    }

    fun returnerTilSaksbehandling(returnerTilSaksbehandlingHendelse: ReturnerTilSaksbehandlingHendelse) {
        repository.hentOppgave(returnerTilSaksbehandlingHendelse.oppgaveId).also { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                logger.info {
                    "Mottatt ReturnerTilSaksbehandlingHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
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
                logger.info {
                    "Behandlet ReturnerTilSaksbehandlingHendelse og publisert oppgave_returnert_til_saksbehandling " +
                        "event. Tilstand etter behandling: ${oppgave.tilstand().type}"
                }
            }
        }
    }

    fun settOppgaveUnderBehandling(behandlingOpplåstHendelse: BehandlingOpplåstHendelse) {
        val oppgave = repository.finnOppgaveFor(behandlingOpplåstHendelse.behandlingId)
        when (oppgave) {
            null -> {
                val feilMelding =
                    "Mottatt hendelse behandling_opplåst for behandling med id " +
                        "${behandlingOpplåstHendelse.behandlingId}." +
                        "Fant ikke oppgave for behandlingen. Gjør derfor ingenting med hendelsen."
                logger.error { feilMelding }
                sendAlertTilRapid(OPPGAVE_IKKE_FUNNET, feilMelding)
            }

            else -> {
                withLoggingContext(
                    "oppgaveId" to oppgave.oppgaveId.toString(),
                    "behandlingId" to oppgave.behandling.behandlingId.toString(),
                ) {
                    logger.info {
                        "Mottatt BehandlingOpplåstHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                    }
                    oppgave.klarTilBehandling(behandlingOpplåstHendelse)
                    repository.lagre(oppgave)
                    logger.info {
                        "Behandlet BehandlingOpplåstHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
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
                val feilMelding =
                    "Mottatt hendelse behandling_låst for behandling med id " +
                        "${behandlingLåstHendelse.behandlingId}." +
                        "Fant ikke oppgave for behandlingen. Gjør derfor ingenting med hendelsen."
                logger.error { feilMelding }
                sendAlertTilRapid(OPPGAVE_IKKE_FUNNET, feilMelding)
            }

            else -> {
                withLoggingContext(
                    "oppgaveId" to oppgave.oppgaveId.toString(),
                    "behandlingId" to oppgave.behandling.behandlingId.toString(),
                ) {
                    logger.info {
                        "Mottatt BehandlingLåstHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                    }
                    oppgave.klarTilKontroll(behandlingLåstHendelse)
                    repository.lagre(oppgave)
                    logger.info {
                        "Behandlet BehandlingLåstHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                    }
                }
            }
        }
    }

    fun ferdigstillOppgave(vedtakFattetHendelse: VedtakFattetHendelse): Oppgave {
        return repository.hentOppgaveFor(vedtakFattetHendelse.behandlingId).also { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                logger.info {
                    "Mottatt hendelse vedtak_fattet for oppgave i tilstand ${oppgave.tilstand().type}"
                }
                oppgave.ferdigstill(vedtakFattetHendelse).let {
                    when (it) {
                        true -> repository.lagre(oppgave)
                        false -> {}
                    }
                }

                logger.info {
                    "Behandlet hendelse vedtak_fattet. Tilstand etter behandling: ${oppgave.tilstand().type}"
                }
            }
        }
    }

    fun ferdigstillOppgave(
        godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        saksbehandlerToken: String,
    ) {
        repository.hentOppgave(godkjentBehandlingHendelse.oppgaveId).let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                logger.info {
                    "Mottatt GodkjentBehandlingHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
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
                    logger.info {
                        "Behandlet GodkjentBehandlingHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                    }
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
        repository.finnOppgaveFor(hendelse.behandlingId)?.let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                logger.info {
                    "Mottatt BehandlingAvbruttHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
                if (oppgave.tilstand() == Oppgave.Opprettet) {
                    repository.slettBehandling(hendelse.behandlingId)
                } else {
                    logger.info { "Behandling med id ${hendelse.behandlingId} behandles i Arena" }
                    oppgave.behandlesIArena(hendelse)
                    repository.lagre(oppgave)
                    logger.info {
                        "Behandlet BehandlingAvbruttHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                    }
                }
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

    fun søk(søkefilter: Søkefilter): OppgaveSøkResultat = repository.søk(søkefilter)

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

    private fun sendAlertTilRapid(
        feilType: OppgaveAlertManager.AlertType,
        utvidetFeilmelding: String,
    ) = rapidsConnection.sendAlertTilRapid(feilType, utvidetFeilmelding)
}
