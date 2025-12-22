package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.AlertManager.OppgaveAlertType.BEHANDLING_IKKE_FUNNET
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.Oppgave.FerdigstillBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.Handling
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.GOSYS
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.INGEN
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKreverIkkeTotrinnskontrollException
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository.OppgaveSøkResultat
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.AvbrytOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.EndreMeldingOmVedtakKildeHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.LagreBrevKvitteringHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.PåVentFristUtgåttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SlettNotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class OppgaveMediator(
    private val oppgaveRepository: OppgaveRepository,
    private val behandlingKlient: BehandlingKlient,
    private val utsendingMediator: UtsendingMediator,
    private val sakMediator: SakMediator,
) {
    private lateinit var rapidsConnection: RapidsConnection

    fun setRapidsConnection(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

    fun lagOppgaveForInnsendingBehandling(
        innsendingMottattHendelse: InnsendingMottattHendelse,
        behandling: Behandling,
        person: Person,
    ) {
        val oppgaveId = UUIDv7.ny()

        // Forventer at søknadsbehandlinger skal opprettes via regelmotor.
        val tilstand =
            if (innsendingMottattHendelse.søknadId != null &&
                innsendingMottattHendelse.kategori in setOf(Kategori.NY_SØKNAD, Kategori.GJENOPPTAK)
            ) {
                Oppgave.Opprettet
            } else {
                Oppgave.KlarTilBehandling
            }

        val oppgave =
            Oppgave(
                oppgaveId = oppgaveId,
                emneknagger = setOf(),
                opprettet = innsendingMottattHendelse.registrertTidspunkt,
                tilstand = tilstand,
                tilstandslogg =
                    OppgaveTilstandslogg(
                        Tilstandsendring(
                            tilstand = tilstand.type,
                            hendelse = innsendingMottattHendelse,
                        ),
                    ),
                behandling = behandling,
                person = person,
                meldingOmVedtak =
                    Oppgave.MeldingOmVedtak(
                        kilde = DP_SAK,
                        kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                    ),
            )
        oppgaveRepository.lagre(oppgave)
    }

    fun taImotEttersending(hendelse: InnsendingMottattHendelse) {
        if (!hendelse.erEttersendingMedSøknadId()) {
            return
        }

        oppgaveRepository
            .søk(
                Søkefilter(
                    periode = Periode.UBEGRENSET_PERIODE,
                    tilstander = Tilstand.Type.values,
                    personIdent = hendelse.ident,
                    søknadId = hendelse.søknadId,
                ),
            ).oppgaver
            .singleOrNull()
            ?.let { oppgave ->
                oppgave.taImotEttersending(hendelse)
                oppgaveRepository.lagre(oppgave)
            } ?: logger.warn {
            "Fant ingen oppgave for søknad med id ${hendelse.søknadId}. Kunne ikke legge til ettersending."
        }
    }

    fun opprettOppgaveForKlageBehandling(behandlingOpprettetHendelse: BehandlingOpprettetHendelse): Oppgave {
        var oppgave: Oppgave? = null

        val sakHistorikk = sakMediator.finnSakHistorikk(behandlingOpprettetHendelse.ident)

        val behandling =
            sakHistorikk
                ?.finnBehandling(behandlingOpprettetHendelse.behandlingId)

        if (behandling == null) {
            val feilmelding =
                "Mottatt hendelse behandlingOpprettetHendelse for behandling med id " +
                    "${behandlingOpprettetHendelse.behandlingId}." +
                    "Fant ikke behandling for hendelsen. Gjør derfor ingenting med hendelsen."
            logger.error { feilmelding }
            sendAlertTilRapid(BEHANDLING_IKKE_FUNNET, feilmelding)
        } else {
            oppgave =
                Oppgave(
                    oppgaveId = UUIDv7.ny(),
                    emneknagger = emptySet(),
                    opprettet = behandling.opprettet,
                    tilstandslogg =
                        OppgaveTilstandslogg(
                            Tilstandsendring(
                                tilstand = KLAR_TIL_BEHANDLING,
                                hendelse = behandlingOpprettetHendelse,
                            ),
                        ),
                    person = sakHistorikk.person,
                    behandling = behandling,
                    meldingOmVedtak =
                        Oppgave.MeldingOmVedtak(
                            kilde = DP_SAK,
                            kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                        ),
                )
            oppgaveRepository.lagre(oppgave)
        }

        // todo Bedre  Exception håndtering
        return oppgave ?: throw IllegalStateException(
            "Kunne ikke opprette oppgave for hendelse behandlingOpprettetHendelse med id " +
                "${behandlingOpprettetHendelse.behandlingId}. Oppgave ble ikke opprettet.",
        )
    }

    fun hentAlleOppgaverMedTilstand(tilstand: Tilstand.Type): List<Oppgave> = oppgaveRepository.hentAlleOppgaverMedTilstand(tilstand)

    fun hentOppgave(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave =
        oppgaveRepository.hentOppgave(oppgaveId).also { oppgave ->
            oppgave.egneAnsatteTilgangskontroll(saksbehandler)
            oppgave.adressebeskyttelseTilgangskontroll(saksbehandler)
        }

    fun hentOppgaveFor(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave =
        oppgaveRepository.hentOppgaveFor(behandlingId).also { oppgave ->
            oppgave.egneAnsatteTilgangskontroll(saksbehandler)
            oppgave.adressebeskyttelseTilgangskontroll(saksbehandler)
        }

    fun hentOppgaveHvisTilgang(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave =
        oppgaveRepository.hentOppgaveFor(behandlingId).also { oppgave ->
            oppgave.egneAnsatteTilgangskontroll(saksbehandler)
            oppgave.adressebeskyttelseTilgangskontroll(saksbehandler)
        }

    fun opprettEllerOppdaterOppgave(forslagTilVedtakHendelse: ForslagTilVedtakHendelse): Oppgave? {
        var oppgave: Oppgave? = null
        val sakHistorikk = sakMediator.finnSakHistorikk(forslagTilVedtakHendelse.ident)
        val behandling =
            sakHistorikk
                ?.finnBehandling(forslagTilVedtakHendelse.behandlingId)

        if (behandling == null) {
            val feilmelding =
                "Mottatt forslag til vedtak for behandling med id ${forslagTilVedtakHendelse.behandlingId}. " +
                    "Fant ikke behandlingen. Gjør derfor ingenting med hendelsen."
            logger.error { feilmelding }
            sendAlertTilRapid(BEHANDLING_IKKE_FUNNET, feilmelding)
        } else {
            oppgave = oppgaveRepository.finnOppgaveFor(forslagTilVedtakHendelse.behandlingId)
            when (oppgave == null) {
                true -> {
                    oppgave =
                        Oppgave(
                            oppgaveId = UUIDv7.ny(),
                            emneknagger = forslagTilVedtakHendelse.emneknagger,
                            opprettet = behandling.opprettet,
                            tilstandslogg =
                                OppgaveTilstandslogg(
                                    Tilstandsendring(
                                        tilstand = KLAR_TIL_BEHANDLING,
                                        hendelse = forslagTilVedtakHendelse,
                                    ),
                                ),
                            person = sakHistorikk.person,
                            behandling = behandling,
                            meldingOmVedtak =
                                Oppgave.MeldingOmVedtak(
                                    kilde = DP_SAK,
                                    kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                                ),
                        )
                    oppgaveRepository.lagre(oppgave)
                }

                false -> {
                    oppgave.oppgaveKlarTilBehandling(forslagTilVedtakHendelse).let { handling ->
                        when (handling) {
                            Handling.LAGRE_OPPGAVE -> {
                                oppgaveRepository.lagre(oppgave)
                                logger.info {
                                    "Behandlet forslag til vedtak. Oppgavens tilstand er" +
                                        " ${oppgave.tilstand().type} etter behandling."
                                }
                            }

                            Handling.INGEN -> {
                                logger.info {
                                    "Mottatt forslag til vedtak. Oppgavens tilstand er uendret." +
                                        " ${oppgave.tilstand().type}"
                                }
                            }
                        }
                    }
                }
            }
        }
        return oppgave
    }

    fun fristillOppgave(fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse) {
        oppgaveRepository
            .hentOppgave(fjernOppgaveAnsvarHendelse.oppgaveId)
            .let { oppgave ->
                oppgave.fjernAnsvar(fjernOppgaveAnsvarHendelse)
                oppgaveRepository.lagre(oppgave)
            }
    }

    fun tildelOppgave(settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse): Oppgave =
        oppgaveRepository.hentOppgave(settOppgaveAnsvarHendelse.oppgaveId).also { oppgave ->
            oppgave.tildel(settOppgaveAnsvarHendelse)
            oppgaveRepository.lagre(oppgave)
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
                        behandlingKlient
                            .kreverTotrinnskontroll(
                                oppgave.behandling.behandlingId,
                                saksbehandlerToken = saksbehandlerToken,
                            ).onFailure {
                                logger.error { "Feil ved henting av behandling med id ${oppgave.behandling.behandlingId}: ${it.message}" }
                            }.getOrThrow()

                    when (kreverToTrinnsKontroll) {
                        true -> {
                            oppgave.sendTilKontroll(sendTilKontrollHendelse)
                            behandlingKlient
                                .godkjenn(
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
                behandlingKlient
                    .sendTilbake(
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

    fun lagreKontrollertBrev(
        oppgaveId: UUID,
        kontrollertBrev: Oppgave.KontrollertBrev,
        saksbehandler: Saksbehandler,
    ) {
        oppgaveRepository.hentOppgave(oppgaveId).let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                oppgave.lagreBrevKvittering(
                    LagreBrevKvitteringHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        kontrollertBrev = kontrollertBrev,
                        utførtAv = saksbehandler,
                    ),
                )
                oppgaveRepository.lagre(oppgave)
            }
        }
    }

    fun endreMeldingOmVedtakKilde(
        oppgaveId: UUID,
        meldingOmVedtakKilde: Oppgave.MeldingOmVedtakKilde,
        saksbehandler: Saksbehandler,
    ) {
        oppgaveRepository.hentOppgave(oppgaveId).let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                oppgave.endreMeldingOmVedtakKilde(
                    EndreMeldingOmVedtakKildeHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        meldingOmVedtakKilde = meldingOmVedtakKilde,
                        utførtAv = saksbehandler,
                    ),
                )
                oppgaveRepository.lagre(oppgave)
            }
        }
    }

    fun lagreNotat(notatHendelse: NotatHendelse): LocalDateTime =
        oppgaveRepository.hentOppgave(notatHendelse.oppgaveId).let { oppgave ->
            oppgave.lagreNotat(notatHendelse)
            oppgaveRepository.lagreNotatFor(oppgave)
        }

    fun slettNotat(slettNotatHendelse: SlettNotatHendelse): LocalDateTime {
        oppgaveRepository.hentOppgave(slettNotatHendelse.oppgaveId).let { oppgave ->
            oppgave.slettNotat(slettNotatHendelse)
            oppgaveRepository.slettNotatFor(oppgave)
        }
        return LocalDateTime.now()
    }

    fun avbryt(
        avbrytOppgaveHendelse: AvbrytOppgaveHendelse,
        saksbehandlerToken: String,
    ) {
        oppgaveRepository.hentOppgave(avbrytOppgaveHendelse.oppgaveId).let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                oppgave.avbryt(avbrytOppgaveHendelse = avbrytOppgaveHendelse)
                behandlingKlient
                    .avbryt(
                        behandlingId = oppgave.behandling.behandlingId,
                        ident = oppgave.personIdent(),
                        saksbehandlerToken = saksbehandlerToken,
                    ).onSuccess {
                        oppgaveRepository.lagre(oppgave)
                    }.onFailure {
                        logger.error { "Feil ved avbryting av oppgave: $it" }
                    }.getOrThrow()
            }
        }
    }

    fun ferdigstillOppgave(innsendingFerdigstiltHendelse: InnsendingFerdigstiltHendelse) {
        oppgaveRepository.hentOppgaveFor(innsendingFerdigstiltHendelse.innsendingId).let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                logger.info {
                    "Mottatt InnsendingFerdigstiltHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
                oppgave.ferdigstill(innsendingFerdigstiltHendelse)
                oppgaveRepository.lagre(oppgave)
                logger.info {
                    "Behandlet InnsendingFerdigstiltHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                }
            }
        }
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

    fun ferdigstillOppgave(vedtakFattetHendelse: VedtakFattetHendelse): Oppgave =
        oppgaveRepository.hentOppgaveFor(vedtakFattetHendelse.behandlingId).also { oppgave ->
            withLoggingContext("oppgaveId" to oppgave.oppgaveId.toString()) {
                logger.info {
                    "Mottatt VedtakFattetHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
                oppgave.ferdigstill(vedtakFattetHendelse).let { handling ->
                    when (handling) {
                        Handling.LAGRE_OPPGAVE -> oppgaveRepository.lagre(oppgave)
                        Handling.INGEN -> {}
                    }
                }

                logger.info {
                    "Behandlet VedtakFattetHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                }
            }
        }

    fun ferdigstillOppgave(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): Result<UUID> =
        runCatching {
            oppgaveRepository.hentOppgaveFor(behandlingId = behandlingId).let { oppgave ->
                oppgave.ferdigstill(
                    GodkjentBehandlingHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                    ),
                )
                oppgave.oppgaveId
            }
        }

    fun ferdigstillOppgave(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ) {
        oppgaveRepository.hentOppgave(oppgaveId).let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                val ferdigstillBehandling =
                    oppgave.ferdigstill(
                        godkjentBehandlingHendelse =
                            GodkjentBehandlingHendelse(
                                oppgaveId = oppgaveId,
                                meldingOmVedtak = null,
                                utførtAv = saksbehandler,
                            ),
                    )

                when (oppgave.meldingOmVedtakKilde()) {
                    DP_SAK -> {
                        logger.info { "Oppgave ferdigstilles med melding om vedtak i DP-Sak" }
                        ferdigstillOppgaveMedUtsending(
                            oppgave = oppgave,
                            ferdigstillBehandling = ferdigstillBehandling,
                            saksbehandlerToken = saksbehandlerToken,
                        )
                    }

                    GOSYS -> {
                        logger.info { "Oppgave ferdigstilles med melding om vedtak i Gosys" }
                        ferdigstillOppgaveUtenUtsending(
                            oppgave = oppgave,
                            ferdigstillBehandling = ferdigstillBehandling,
                            saksbehandlerToken = saksbehandlerToken,
                        )
                    }

                    INGEN -> {
                        logger.info { "Oppgave ferdigstilles uten melding om vedtak" }
                        ferdigstillOppgaveUtenUtsending(
                            oppgave = oppgave,
                            ferdigstillBehandling = ferdigstillBehandling,
                            saksbehandlerToken = saksbehandlerToken,
                        )
                    }
                }
            }
        }
    }

    private fun ferdigstillOppgaveMedUtsending(
        oppgave: Oppgave,
        ferdigstillBehandling: FerdigstillBehandling,
        saksbehandlerToken: String,
    ) {
        val utsendingId =
            utsendingMediator.opprettUtsending(
                behandlingId = oppgave.behandling.behandlingId,
                brev = null,
                ident = oppgave.personIdent(),
            )
        when (ferdigstillBehandling) {
            FerdigstillBehandling.GODKJENN -> {
                behandlingKlient
                    .godkjenn(
                        behandlingId = oppgave.behandling.behandlingId,
                        ident = oppgave.personIdent(),
                        saksbehandlerToken = saksbehandlerToken,
                    ).onSuccess {
                        oppgaveRepository.lagre(oppgave)
                    }.onFailure {
                        utsendingMediator.slettUtsending(utsendingId).also { rowsDeleted ->
                            when (rowsDeleted) {
                                1 -> logger.info { "Slettet utsending med id $utsendingId" }
                                else -> logger.error { "Fant ikke utsending med id $utsendingId" }
                            }
                        }
                        throw it
                    }
            }

            FerdigstillBehandling.BESLUTT -> {
                behandlingKlient
                    .beslutt(
                        behandlingId = oppgave.behandling.behandlingId,
                        ident = oppgave.personIdent(),
                        saksbehandlerToken = saksbehandlerToken,
                    ).onSuccess {
                        oppgaveRepository.lagre(oppgave)
                    }.onFailure {
                        utsendingMediator.slettUtsending(utsendingId).also { rowsDeleted ->
                            when (rowsDeleted) {
                                1 -> logger.info { "Slettet utsending med id $utsendingId" }
                                else -> logger.error { "Fant ikke utsending med id $utsendingId" }
                            }
                        }
                        throw it
                    }
            }
        }
    }

    private fun ferdigstillOppgaveUtenUtsending(
        oppgave: Oppgave,
        ferdigstillBehandling: FerdigstillBehandling,
        saksbehandlerToken: String,
    ) {
        when (ferdigstillBehandling) {
            FerdigstillBehandling.GODKJENN -> {
                behandlingKlient
                    .godkjenn(
                        behandlingId = oppgave.behandling.behandlingId,
                        ident = oppgave.personIdent(),
                        saksbehandlerToken = saksbehandlerToken,
                    ).onSuccess {
                        oppgaveRepository.lagre(oppgave)
                    }.onFailure {
                        throw it
                    }
            }

            FerdigstillBehandling.BESLUTT -> {
                behandlingKlient
                    .beslutt(
                        behandlingId = oppgave.behandling.behandlingId,
                        ident = oppgave.personIdent(),
                        saksbehandlerToken = saksbehandlerToken,
                    ).onSuccess {
                        oppgaveRepository.lagre(oppgave)
                    }.onFailure {
                        throw it
                    }
            }
        }
    }

    fun avbrytOppgave(hendelse: BehandlingAvbruttHendelse) {
        oppgaveRepository
            .søk(
                Søkefilter(
                    periode = Periode.UBEGRENSET_PERIODE,
                    tilstander = Tilstand.Type.values,
                    behandlingId = hendelse.behandlingId,
                ),
            ).oppgaver
            .singleOrNull()
            ?.let { oppgave ->
                withLoggingContext(
                    "oppgaveId" to oppgave.oppgaveId.toString(),
                ) {
                    logger.info { "Mottatt BehandlingAvbruttHendelse for oppgave i tilstand ${oppgave.tilstand().type}" }
                    oppgave.avbryt(hendelse)
                    oppgaveRepository.lagre(oppgave)
                    logger.info { "Tilstand etter BehandlingAvbruttHendelse: ${oppgave.tilstand().type}" }
                }
            }
    }

    fun utsettOppgave(utsettOppgaveHendelse: UtsettOppgaveHendelse) {
        oppgaveRepository.hentOppgave(utsettOppgaveHendelse.oppgaveId).let { oppgave ->
            oppgave.utsett(utsettOppgaveHendelse)
            oppgaveRepository.lagre(oppgave)
        }
    }

    fun finnOppgaverPåVentMedUtgåttFrist(frist: LocalDate): List<UUID> = oppgaveRepository.finnOppgaverPåVentMedUtgåttFrist(frist)

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

    fun oppgaveTilstandForSøknad(
        søknadId: UUID,
        ident: String,
    ) = oppgaveRepository.oppgaveTilstandForSøknad(søknadId = søknadId, ident = ident)

    private fun sendAlertTilRapid(
        feilType: AlertManager.AlertType,
        utvidetFeilmelding: String,
    ) = rapidsConnection.sendAlertTilRapid(feilType, utvidetFeilmelding)
}
