package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.AlertManager.OppgaveAlertType.OPPGAVE_IKKE_FUNNET
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.BEHANDLES_I_ARENA
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKreverIkkeTotrinnskontrollException
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository.OppgaveSøkResultat
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
import no.nav.dagpenger.saksbehandling.db.person.PersonRepository
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.PåVentFristUtgåttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SlettNotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.vedtaksmelding.MeldingOmVedtakKlient
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

class OppgaveMediator(
    private val personRepository: PersonRepository,
    private val oppgaveRepository: OppgaveRepository,
    private val behandlingKlient: BehandlingKlient,
    private val utsendingMediator: UtsendingMediator,
    private val oppslag: Oppslag,
    private val meldingOmVedtakKlient: MeldingOmVedtakKlient,
) {
    private lateinit var rapidsConnection: RapidsConnection

    fun setRapidsConnection(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

    fun opprettOppgaveForBehandling(søknadsbehandlingOpprettetHendelse: SøknadsbehandlingOpprettetHendelse) {
        val person =
            personRepository.finnPerson(søknadsbehandlingOpprettetHendelse.ident)
                ?: runBlocking {
                    oppslag.hentPersonMedSkjermingOgGradering(søknadsbehandlingOpprettetHendelse.ident)
                }

        if (oppgaveRepository.finnBehandling(søknadsbehandlingOpprettetHendelse.behandlingId) != null) {
            logger.warn { "Mottatt hendelse behandling_opprettet, men behandling finnes allerede." }

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
                tilstandslogg =
                    Tilstandslogg(
                        Tilstandsendring(
                            tilstand = OPPRETTET,
                            hendelse = søknadsbehandlingOpprettetHendelse,
                        ),
                    ),
                behandlingId = behandling.behandlingId,
                behandlingType = behandling.type,
                personIdent = person.ident,
            )

        oppgaveRepository.lagre(oppgave)
    }

    fun opprettOppgaveForBehandling(behandlingOpprettetHendelse: BehandlingOpprettetHendelse): Oppgave {
        val person =
            personRepository.finnPerson(behandlingOpprettetHendelse.ident)
                ?: runBlocking {
                    oppslag.hentPersonMedSkjermingOgGradering(behandlingOpprettetHendelse.ident)
                }

        val behandling =
            Behandling(
                behandlingId = behandlingOpprettetHendelse.behandlingId,
                person = person,
                opprettet = behandlingOpprettetHendelse.opprettet,
                hendelse = behandlingOpprettetHendelse,
                type = behandlingOpprettetHendelse.type,
            )

        val oppgave =
            Oppgave(
                oppgaveId = UUIDv7.ny(),
                emneknagger = emptySet(),
                opprettet = behandlingOpprettetHendelse.opprettet,
                behandlerIdent = null,
                behandling = behandling,
                tilstand = Oppgave.KlarTilBehandling,
                tilstandslogg =
                    Tilstandslogg(
                        Tilstandsendring(
                            tilstand = Oppgave.KlarTilBehandling.type,
                            hendelse = behandlingOpprettetHendelse,
                        ),
                    ),
                behandlingId = behandling.behandlingId,
                behandlingType = behandling.type,
                personIdent = person.ident,
            )

        oppgaveRepository.lagre(oppgave)
        return oppgave
    }

    fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave> {
        return oppgaveRepository.hentAlleOppgaverMedTilstand(tilstand)
    }

    fun hentOppgave(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave {
        return oppgaveRepository.hentOppgave(oppgaveId).also { oppgave ->
            oppgave.egneAnsatteTilgangskontroll(saksbehandler)
            oppgave.adressebeskyttelseTilgangskontroll(saksbehandler)
        }
    }

    fun hentOppgaveFor(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave {
        return oppgaveRepository.hentOppgaveFor(behandlingId).also { oppgave ->
            oppgave.egneAnsatteTilgangskontroll(saksbehandler)
            oppgave.adressebeskyttelseTilgangskontroll(saksbehandler)
        }
    }

    fun hentOppgaveHvisTilgang(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave {
        return oppgaveRepository.hentOppgaveFor(behandlingId).also { oppgave ->
            oppgave.egneAnsatteTilgangskontroll(saksbehandler)
            oppgave.adressebeskyttelseTilgangskontroll(saksbehandler)
        }
    }

    fun settOppgaveKlarTilBehandling(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        val oppgave = oppgaveRepository.finnOppgaveFor(forslagTilVedtakHendelse.behandlingId)
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
                ) {
                    logger.info {
                        "Mottatt hendelse forslag_til_vedtak. Oppgavens tilstand er" +
                            " ${oppgave.tilstand().type} når hendelsen mottas."
                    }
                    oppgave.oppgaveKlarTilBehandling(forslagTilVedtakHendelse).let { handling ->
                        when (handling) {
                            Oppgave.Handling.LAGRE_OPPGAVE -> {
                                oppgaveRepository.lagre(oppgave)
                                logger.info {
                                    "Behandlet hendelse forslag_til_vedtak. Oppgavens tilstand er" +
                                        " ${oppgave.tilstand().type} etter behandling."
                                }
                            }

                            Oppgave.Handling.INGEN -> {
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
        oppgaveRepository.hentOppgave(fjernOppgaveAnsvarHendelse.oppgaveId)
            .let { oppgave ->
                oppgave.fjernAnsvar(fjernOppgaveAnsvarHendelse)
                oppgaveRepository.lagre(oppgave)
            }
    }

    fun tildelOppgave(settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse): Oppgave {
        return oppgaveRepository.hentOppgave(settOppgaveAnsvarHendelse.oppgaveId).also { oppgave ->
            oppgave.tildel(settOppgaveAnsvarHendelse)
            oppgaveRepository.lagre(oppgave)
        }
    }

    fun sendTilKontroll(
        sendTilKontrollHendelse: SendTilKontrollHendelse,
        saksbehandlerToken: String,
    ) {
        oppgaveRepository.hentOppgave(sendTilKontrollHendelse.oppgaveId).also { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                logger.info {
                    "Mottatt SendTilKontrollHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
                runBlocking {
                    val kreverToTrinnsKontroll =
                        behandlingKlient.kreverTotrinnskontroll(
                            oppgave.behandling.behandlingId,
                            saksbehandlerToken = saksbehandlerToken,
                        ).onFailure {
                            logger.error { "Feil ved henting av behandling med id ${oppgave.behandling.behandlingId}: ${it.message}" }
                        }.getOrThrow()

                    when (kreverToTrinnsKontroll) {
                        true -> {
                            oppgave.sendTilKontroll(sendTilKontrollHendelse)
                            behandlingKlient.godkjenn(
                                behandlingId = oppgave.behandling.behandlingId,
                                ident = oppgave.personIdent(),
                                saksbehandlerToken = saksbehandlerToken,
                            ).onSuccess {
                                oppgaveRepository.lagre(oppgave)
                                logger.info {
                                    "Behandlet SendTilKontrollHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                                }
                            }.onFailure {
                                logger.error { "Feil ved godkjenning av behandling: ${it.message}" }
                            }.getOrThrow()
                        }

                        false -> {
                            throw BehandlingKreverIkkeTotrinnskontrollException("Behandling krever ikke totrinnskontroll")
                        }
                    }
                }
            }
        }
    }

    fun returnerTilSaksbehandling(
        returnerTilSaksbehandlingHendelse: ReturnerTilSaksbehandlingHendelse,
        beslutterToken: String,
    ) {
        oppgaveRepository.hentOppgave(returnerTilSaksbehandlingHendelse.oppgaveId).also { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                logger.info {
                    "Mottatt ReturnerTilSaksbehandlingHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
                oppgave.returnerTilSaksbehandling(returnerTilSaksbehandlingHendelse)
                behandlingKlient.sendTilbake(
                    behandlingId = oppgave.behandling.behandlingId,
                    ident = oppgave.personIdent(),
                    saksbehandlerToken = beslutterToken,
                ).onSuccess {
                    oppgaveRepository.lagre(oppgave)
                    logger.info { "Sendt behandling med id ${oppgave.behandling.behandlingId} tilbake til saksbehandling" }
                }.onFailure {
                    val feil =
                        "Feil ved sending av behandling med id ${oppgave.behandling.behandlingId} " +
                            "tilbake til saksbehandling: ${it.message}"
                    logger.error { feil }
                    throw it
                }
                logger.info {
                    "Behandlet ReturnerTilSaksbehandlingHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                }
            }
        }
    }

    fun lagreNotat(notatHendelse: NotatHendelse): LocalDateTime {
        return oppgaveRepository.hentOppgave(notatHendelse.oppgaveId).let { oppgave ->
            oppgave.lagreNotat(notatHendelse)
            oppgaveRepository.lagreNotatFor(oppgave)
        }
    }

    fun slettNotat(slettNotatHendelse: SlettNotatHendelse): LocalDateTime {
        oppgaveRepository.hentOppgave(slettNotatHendelse.oppgaveId).let { oppgave ->
            oppgave.slettNotat(slettNotatHendelse)
            oppgaveRepository.slettNotatFor(oppgave)
        }
        return LocalDateTime.now()
    }

    fun ferdigstillOppgave(avbruttHendelse: AvbruttHendelse) {
        oppgaveRepository.hentOppgaveFor(avbruttHendelse.behandlingId).let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                logger.info {
                    "Mottatt AvbruttHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
                oppgave.ferdigstill(avbruttHendelse)
                oppgaveRepository.lagre(oppgave)
                logger.info {
                    "Behandlet AvbruttHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                }
            }
        }
    }

    fun ferdigstillOppgave(godkjentBehandlingHendelse: GodkjentBehandlingHendelse) {
        oppgaveRepository.hentOppgave(godkjentBehandlingHendelse.oppgaveId).let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                logger.info {
                    "Mottatt GodkjentBehandlingHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
                oppgave.ferdigstill(godkjentBehandlingHendelse)
                oppgaveRepository.lagre(oppgave)
                logger.info {
                    "Behandlet GodkjentBehandlingHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                }
            }
        }
    }

    fun ferdigstillOppgave(vedtakFattetHendelse: VedtakFattetHendelse): Oppgave {
        return oppgaveRepository.hentOppgaveFor(vedtakFattetHendelse.behandlingId).also { oppgave ->
            withLoggingContext("oppgaveId" to oppgave.oppgaveId.toString()) {
                logger.info {
                    "Mottatt hendelse vedtak_fattet for oppgave i tilstand ${oppgave.tilstand().type}"
                }
                oppgave.ferdigstill(vedtakFattetHendelse).let { handling ->
                    when (handling) {
                        Oppgave.Handling.LAGRE_OPPGAVE -> oppgaveRepository.lagre(oppgave)
                        Oppgave.Handling.INGEN -> {}
                    }
                }

                logger.info {
                    "Behandlet hendelse vedtak_fattet. Tilstand etter behandling: ${oppgave.tilstand().type}"
                }
            }
        }
    }

    suspend fun ferdigstillOppgave(
        oppgaveId: UUID,
        saksBehandler: Saksbehandler,
        saksbehandlerToken: String,
    ) {
        oppgaveRepository.hentOppgave(oppgaveId).let { oppgave ->
            coroutineScope {
                val person = async(Dispatchers.IO) { oppslag.hentPerson(oppgave.personIdent()) }
                val saksbehandler =
                    async(Dispatchers.IO) {
                        oppgave.sisteSaksbehandler()?.let { saksbehandlerIdent ->
                            oppslag.hentBehandler(saksbehandlerIdent)
                        } ?: throw RuntimeException("Fant ikke saksbehandler for oppgave ${oppgave.oppgaveId}")
                    }
                val beslutter =
                    async(Dispatchers.IO) {
                        oppgave.sisteBeslutter()?.let { beslutterIdent ->
                            oppslag.hentBehandler(beslutterIdent)
                        }
                    }

                meldingOmVedtakKlient.lagOgHentMeldingOmVedtak(
                    person = person.await(),
                    saksbehandler = saksbehandler.await(),
                    beslutter = beslutter.await(),
                    behandlingId = oppgave.behandling.behandlingId,
                    saksbehandlerToken,
                ).onSuccess { html ->
                    ferdigstillOppgave(
                        godkjentBehandlingHendelse =
                            GodkjentBehandlingHendelse(
                                oppgaveId = oppgaveId,
                                meldingOmVedtak = html,
                                utførtAv = saksBehandler,
                            ),
                        saksbehandlerToken = saksbehandlerToken,
                    )
                }.onFailure {
                    throw it
                }
            }
        }
    }

    fun ferdigstillOppgave(
        godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        saksbehandlerToken: String,
        oppgaveTilFerdigstilling: Oppgave? = null,
    ) {
        oppgaveTilFerdigstilling ?: oppgaveRepository.hentOppgave(godkjentBehandlingHendelse.oppgaveId).let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                logger.info {
                    "Mottatt GodkjentBehandlingHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
                val ferdigstillBehandling = oppgave.ferdigstill(godkjentBehandlingHendelse)

                val utsendingID =
                    utsendingMediator.opprettUtsending(
                        oppgave.oppgaveId,
                        godkjentBehandlingHendelse.meldingOmVedtak,
                        oppgave.personIdent(),
                    )

                when (ferdigstillBehandling) {
                    Oppgave.FerdigstillBehandling.GODKJENN -> {
                        behandlingKlient.godkjenn(
                            behandlingId = oppgave.behandling.behandlingId,
                            ident = oppgave.personIdent(),
                            saksbehandlerToken = saksbehandlerToken,
                        ).onSuccess {
                            oppgaveRepository.lagre(oppgave)
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
                            throw it
                        }
                    }

                    Oppgave.FerdigstillBehandling.BESLUTT -> {
                        behandlingKlient.beslutt(
                            behandlingId = oppgave.behandling.behandlingId,
                            ident = oppgave.personIdent(),
                            saksbehandlerToken = saksbehandlerToken,
                        ).onSuccess {
                            oppgaveRepository.lagre(oppgave)
                            logger.info {
                                "Behandlet GodkjentBehandlingHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                            }
                        }.onFailure {
                            val feil = "Feil ved beslutting av behandling: ${it.message}"
                            logger.error { feil }
                            utsendingMediator.slettUtsending(utsendingID).also { rowsDeleted ->
                                when (rowsDeleted) {
                                    1 -> logger.info { "Slettet utsending med id $utsendingID" }
                                    else -> logger.error { "Fant ikke utsending med id $utsendingID" }
                                }
                            }
                            throw it
                        }
                    }
                }
            }
        }
    }

    fun ferdigstillOppgave(
        godkjennBehandlingMedBrevIArena: GodkjennBehandlingMedBrevIArena,
        saksbehandlerToken: String,
    ) {
        oppgaveRepository.hentOppgave(godkjennBehandlingMedBrevIArena.oppgaveId).let { oppgave ->
            oppgave.ferdigstill(godkjennBehandlingMedBrevIArena)

            behandlingKlient.godkjenn(
                behandlingId = oppgave.behandling.behandlingId,
                ident = oppgave.personIdent(),
                saksbehandlerToken = saksbehandlerToken,
            ).onSuccess {
                oppgaveRepository.lagre(oppgave)
            }.onFailure {
                val feil = "Feil ved godkjenning av behandling: ${it.message}"
                logger.error { feil }
                throw it
            }
        }
    }

    fun avbrytOppgave(hendelse: BehandlingAvbruttHendelse) {
        oppgaveRepository.finnOppgaveFor(hendelse.behandlingId)?.let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
            ) {
                logger.info { "Mottatt BehandlingAvbruttHendelse for oppgave i tilstand ${oppgave.tilstand().type}" }
                if (oppgave.tilstand() == Oppgave.Opprettet) {
                    oppgaveRepository.slettBehandling(hendelse.behandlingId)
                } else {
                    logger.info { "Oppgaven behandles i Arena" }
                    oppgave.behandlesIArena(hendelse)
                    oppgaveRepository.lagre(oppgave)
                    logger.info { "Tilstand etter BehandlingAvbruttHendelse: ${oppgave.tilstand().type}" }
                }
            }
        }
    }

    fun utsettOppgave(utsettOppgaveHendelse: UtsettOppgaveHendelse) {
        oppgaveRepository.hentOppgave(utsettOppgaveHendelse.oppgaveId).let { oppgave ->
            oppgave.utsett(utsettOppgaveHendelse)
            oppgaveRepository.lagre(oppgave)
        }
    }

    fun finnOppgaverPåVentMedUtgåttFrist(frist: LocalDate): List<UUID> {
        return oppgaveRepository.finnOppgaverPåVentMedUtgåttFrist(frist)
    }

    fun håndterPåVentFristUtgått(påVentFristUtgåttHendelse: PåVentFristUtgåttHendelse) {
        oppgaveRepository.hentOppgave(påVentFristUtgåttHendelse.oppgaveId).let { oppgave ->
            oppgave.oppgaverPåVentMedUtgåttFrist(påVentFristUtgåttHendelse)
            oppgaveRepository.lagre(oppgave)
        }
    }

    fun hentOppgaveIdFor(behandlingId: UUID): UUID? = oppgaveRepository.hentOppgaveIdFor(behandlingId)

    fun finnOppgaverFor(ident: String): List<Oppgave> = oppgaveRepository.finnOppgaverFor(ident)

    fun søk(søkefilter: Søkefilter): OppgaveSøkResultat = oppgaveRepository.søk(søkefilter)

    fun tildelOgHentNesteOppgave(
        nesteOppgaveHendelse: NesteOppgaveHendelse,
        queryString: String,
    ): Oppgave? {
        val tildelNesteOppgaveFilter =
            TildelNesteOppgaveFilter.fra(
                queryString = queryString,
                saksbehandler = nesteOppgaveHendelse.utførtAv,
            )
        return oppgaveRepository.tildelOgHentNesteOppgave(nesteOppgaveHendelse, tildelNesteOppgaveFilter)
    }

    fun skalEttersendingTilSøknadVarsles(
        søknadId: UUID,
        ident: String,
    ): Boolean {
        val tilstand = oppgaveRepository.oppgaveTilstandForSøknad(søknadId = søknadId, ident = ident)

        return when (tilstand) {
            OPPRETTET -> false
            KLAR_TIL_BEHANDLING -> false
            BEHANDLES_I_ARENA -> false
            AVVENTER_LÅS_AV_BEHANDLING -> false
            AVVENTER_OPPLÅSING_AV_BEHANDLING -> false
            else -> true
        }
    }

    fun hentPerson(ident: String): Person {
        return personRepository.hentPerson(ident)
    }

    fun hentPerson(personId: UUID): Person {
        return personRepository.hentPerson(personId)
    }

    private fun sendAlertTilRapid(
        feilType: AlertManager.AlertType,
        utvidetFeilmelding: String,
    ) = rapidsConnection.sendAlertTilRapid(feilType, utvidetFeilmelding)
}
