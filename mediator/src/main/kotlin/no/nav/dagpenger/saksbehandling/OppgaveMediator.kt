package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.AlertManager.OppgaveAlertType.BEHANDLING_IKKE_FUNNET
import no.nav.dagpenger.saksbehandling.Oppgave.Handling
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.GOSYS
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.INGEN
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand
import no.nav.dagpenger.saksbehandling.behandling.BehandlingException
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.db.Transaksjoner
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository.OppgaveSøkResultat
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.AvbrytOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingTilGodkjenningHendelse
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
import no.nav.dagpenger.saksbehandling.hendelser.OppfølgingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.PåVentFristUtgåttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SlettNotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.meldekortkontroll.HarAvvikendeMeldkortSyklusException
import no.nav.dagpenger.saksbehandling.meldekortkontroll.MeldekortKontrollKlient
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.utboks.Utboks
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
    private val utboks: Utboks,
    private val transaksjoner: Transaksjoner,
    private val meldekortKontrollKlient: MeldekortKontrollKlient,
) {
    fun lagOppgaveForInnsendingBehandling(
        innsendingMottattHendelse: InnsendingMottattHendelse,
        behandling: Behandling,
        person: Person,
        ctx: Transaksjonskontekst = Transaksjonskontekst.IkkeAktiv,
    ) {
        val forventerBehandlingOpprettet =
            innsendingMottattHendelse.søknadId != null &&
                innsendingMottattHendelse.kategori in setOf(Kategori.NY_SØKNAD, Kategori.GJENOPPTAK)

        val oppgave =
            Oppgave(
                emneknagger = setOf(),
                opprettet = innsendingMottattHendelse.registrertTidspunkt,
                behandling = behandling,
                person = person,
                meldingOmVedtak =
                    Oppgave.MeldingOmVedtak(
                        kilde = DP_SAK,
                        kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                    ),
            ).also {
                if (!forventerBehandlingOpprettet) {
                    it.settKlarTilBehandling(innsendingMottattHendelse)
                }
            }

        oppgaveRepository.lagre(oppgave, ctx)
    }

    fun lagOppgaveForOppfølging(
        hendelse: OpprettOppfølgingHendelse,
        behandling: Behandling,
        person: Person,
        ctx: Transaksjonskontekst = Transaksjonskontekst.IkkeAktiv,
    ): Oppgave {
        val oppgave =
            Oppgave(
                emneknagger = setOf(hendelse.aarsak),
                opprettet = hendelse.registrertTidspunkt,
                behandling = behandling,
                hendelse = hendelse,
                person = person,
                meldingOmVedtak =
                    Oppgave.MeldingOmVedtak(
                        kilde = DP_SAK,
                        kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                    ),
            )

        oppgave.klargjørForBehandling(hendelse)
        oppgaveRepository.lagre(oppgave, ctx)
        return oppgave
    }

    fun lagOppgaveForKlage(
        hendelse: BehandlingOpprettetHendelse,
        sakHistorikk: SakHistorikk,
        saksbehandler: Saksbehandler? = null,
        ctx: Transaksjonskontekst.Aktiv,
    ): Oppgave {
        val behandling =
            sakHistorikk.finnBehandling(hendelse.behandlingId)
                ?: throw IllegalStateException(
                    "Fant ikke behandling ${hendelse.behandlingId} i sakHistorikk for klageopprettelse",
                )

        val oppgave =
            Oppgave(
                emneknagger = emptySet(),
                opprettet = behandling.opprettet,
                person = sakHistorikk.person,
                behandling = behandling,
                meldingOmVedtak =
                    Oppgave.MeldingOmVedtak(
                        kilde = DP_SAK,
                        kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                    ),
            )

        oppgave.settKlarTilBehandling(hendelse)
        saksbehandler?.let {
            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = it.navIdent,
                    utførtAv = it,
                ),
            )
        }
        oppgaveRepository.lagre(oppgave, ctx)
        return oppgave
    }

    fun settEmneknaggEttersending(
        hendelse: InnsendingMottattHendelse,
        ctx: Transaksjonskontekst = Transaksjonskontekst.IkkeAktiv,
    ) {
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
                oppgave.settEmneknagg(hendelse)
                oppgaveRepository.lagre(oppgave, ctx)
            } ?: logger.warn {
            "Fant ingen oppgave for søknad med id ${hendelse.søknadId}. Kunne ikke legge til ettersending."
        }
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

    fun opprettEllerOppdaterOppgave(forslagTilVedtakHendelse: ForslagTilVedtakHendelse): Oppgave? {
        var oppgave: Oppgave? = null
        val sakHistorikk = sakMediator.finnSakHistorikk(forslagTilVedtakHendelse.ident)
        val behandling =
            sakHistorikk
                ?.finnBehandling(forslagTilVedtakHendelse.behandlingId)

        if (behandling == null) {
            loggOgAlertBehandlingIkkeFunnet(forslagTilVedtakHendelse)
        } else {
            oppgave = oppgaveRepository.finnOppgaveFor(forslagTilVedtakHendelse.behandlingId)
            when (oppgave == null) {
                true -> {
                    oppgave =
                        Oppgave(
                            emneknagger = forslagTilVedtakHendelse.emneknagger,
                            opprettet = behandling.opprettet,
                            person = sakHistorikk.person,
                            behandling = behandling,
                            meldingOmVedtak =
                                Oppgave.MeldingOmVedtak(
                                    kilde = DP_SAK,
                                    kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                                ),
                        ).also {
                            it.settKlarTilBehandling(forslagTilVedtakHendelse)
                        }
                    transaksjoner.transaksjon { ctx ->
                        if (forslagTilVedtakHendelse.ident.first().digitToInt() in 4..7) {
                            oppgave.leggTilEmneknagger(setOf(Emneknagg.Søknadsavklaring.D_NUMMER.visningsnavn))
                        }
                        oppgaveRepository.lagre(oppgave, ctx)
                        if (forslagTilVedtakHendelse.behandletHendelseType == "Søknad") {
                            sendSøknadsavklaringBehov(oppgave, forslagTilVedtakHendelse, ctx)
                        }
                    }
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
                                logger.error {
                                    "Feil ved sjekk om behandling krever totrinns av behandling med id ${oppgave.behandling.behandlingId}: ${it.message}"
                                }
                            }.getOrThrow()

                    oppgave.sendTilKontroll(sendTilKontrollHendelse)
                    when (kreverToTrinnsKontroll) {
                        true -> {
                            behandlingKlient
                                .godkjenn(
                                    behandlingId = oppgave.behandling.behandlingId,
                                    ident = oppgave.personIdent(),
                                    saksbehandlerToken = saksbehandlerToken,
                                ).let { håndterMuligKonflikt(it) }
                        }

                        false -> {
                            logger.info {
                                "Behandling med id: ${oppgave.behandling.behandlingId} krever ikke totrinnskontroll. Oppgaven settes til kvalitetskontroll."
                            }
                        }
                    }
                    oppgaveRepository.lagre(oppgave)
                    logger.info {
                        "Behandlet SendTilKontrollHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                    }
                }
            }
        }
    }

    private fun håndterMuligKonflikt(it: Result<Unit>) {
        it.exceptionOrNull()?.let {
            if (!(it is BehandlingException && it.status == 409 && it.tilAlleredeTilBeslutning() == AlleredeTilBeslutning)) {
                throw it
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
                runBlocking {
                    val kreverToTrinnsKontroll =
                        behandlingKlient
                            .kreverTotrinnskontroll(
                                oppgave.behandling.behandlingId,
                                saksbehandlerToken = beslutterToken,
                            ).onFailure {
                                logger.error { "Feil ved henting av behandling med id ${oppgave.behandling.behandlingId}: ${it.message}" }
                            }.getOrThrow()
                    when (kreverToTrinnsKontroll) {
                        true -> {
                            behandlingKlient
                                .sendTilbake(
                                    behandlingId = oppgave.behandling.behandlingId,
                                    ident = oppgave.personIdent(),
                                    saksbehandlerToken = beslutterToken,
                                ).onSuccess {
                                    logger.info { "Sendt behandling med id ${oppgave.behandling.behandlingId} tilbake til saksbehandling" }
                                }.onFailure {
                                    val feil =
                                        "Feil ved sending av behandling med id ${oppgave.behandling.behandlingId} " +
                                            "tilbake til saksbehandling: ${it.message}"
                                    logger.error { feil }
                                    throw it
                                }
                        }

                        false -> {
                            logger.info {
                                "Behandling med id: ${oppgave.behandling.behandlingId} krever ikke totrinnskontroll. Oppgaven settes til under behandling"
                            }
                        }
                    }
                    oppgaveRepository.lagre(oppgave)
                    logger.info {
                        "Behandlet ReturnerTilSaksbehandlingHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                    }
                }
            }
        }
    }

    fun behandlingTilGodkjenning(hendelse: BehandlingTilGodkjenningHendelse) {
        val oppgave = oppgaveRepository.finnOppgaveFor(hendelse.behandlingId)
        if (oppgave == null) {
            logger.info {
                "Mottatt BehandlingTilGodkjenningHendelse for behandling ${hendelse.behandlingId}, " +
                    "men fant ingen oppgave. Dette er forventet ved førstegangs TilGodkjenning, hvor " +
                    "oppgaven opprettes av forslag_til_behandlingsresultat. Ignorerer hendelsen."
            }
            return
        }
        withLoggingContext(
            "oppgaveId" to oppgave.oppgaveId.toString(),
            "behandlingId" to oppgave.behandling.behandlingId.toString(),
        ) {
            logger.info {
                "Mottatt BehandlingTilGodkjenningHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
            }
            when (oppgave.behandlingTilGodkjenning(hendelse)) {
                Handling.LAGRE_OPPGAVE -> {
                    oppgaveRepository.lagre(oppgave)
                    logger.info {
                        "Behandlet BehandlingTilGodkjenningHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                    }
                }

                Handling.INGEN -> {
                    logger.info {
                        "BehandlingTilGodkjenningHendelse ga ingen tilstandsendring. Tilstand: ${oppgave.tilstand().type}"
                    }
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
                transaksjoner.transaksjon { ctx ->
                    oppgaveRepository.lagre(oppgave, ctx)
                    if (meldingOmVedtakKilde != DP_SAK) {
                        utsendingMediator.slettUtsendingForBehandling(oppgave.behandling.behandlingId, ctx)
                    }
                }
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

    fun avbryt(avbrytOppgaveHendelse: AvbrytOppgaveHendelse) {
        oppgaveRepository.hentOppgave(avbrytOppgaveHendelse.oppgaveId).let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                oppgave.avbryt(avbrytOppgaveHendelse = avbrytOppgaveHendelse)
                transaksjoner.transaksjon { ctx ->
                    oppgaveRepository.lagre(oppgave, ctx)
                    utsendingMediator.avbrytUtsendingForBehandling(oppgave.behandling.behandlingId, ctx)
                    if (oppgave.behandling.utløstAv is HendelseBehandler.DpBehandling) {
                        utboks.send(
                            key = oppgave.personIdent(),
                            message =
                                JsonMessage
                                    .newMessage(
                                        eventName = "avbryt_behandling",
                                        map =
                                            mapOf(
                                                "behandlingId" to oppgave.behandling.behandlingId,
                                                "ident" to oppgave.personIdent(),
                                                "årsak" to avbrytOppgaveHendelse.årsak.visningsnavn,
                                            ),
                                    ).toJson(),
                            ctx = ctx,
                        )
                    }
                }
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

    fun ferdigstillOppgave(
        oppfølgingFerdigstiltHendelse: OppfølgingFerdigstiltHendelse,
        ctx: Transaksjonskontekst = Transaksjonskontekst.IkkeAktiv,
    ) {
        oppgaveRepository.hentOppgaveFor(oppfølgingFerdigstiltHendelse.oppfølgingId).let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                logger.info {
                    "Mottatt OppfølgingFerdigstiltHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
                oppgave.ferdigstill(oppfølgingFerdigstiltHendelse)
                oppgaveRepository.lagre(oppgave, ctx)
                logger.info {
                    "Behandlet OppfølgingFerdigstiltHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                }
            }
        }
    }

    fun ferdigstillOppgave(
        avbruttHendelse: AvbruttHendelse,
        ctx: Transaksjonskontekst = Transaksjonskontekst.IkkeAktiv,
    ) {
        oppgaveRepository.hentOppgaveFor(avbruttHendelse.behandlingId).let { oppgave ->
            withLoggingContext(
                "oppgaveId" to oppgave.oppgaveId.toString(),
                "behandlingId" to oppgave.behandling.behandlingId.toString(),
            ) {
                logger.info {
                    "Mottatt AvbruttHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
                oppgave.ferdigstill(avbruttHendelse)
                oppgaveRepository.lagre(oppgave, ctx)
                logger.info {
                    "Behandlet AvbruttHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                }
            }
        }
    }

    fun ferdigstillOppgave(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
        ctx: Transaksjonskontekst = Transaksjonskontekst.IkkeAktiv,
    ): Result<UUID> =
        runCatching {
            oppgaveRepository.hentOppgaveFor(behandlingId = behandlingId).let { oppgave ->
                oppgave.ferdigstill(
                    GodkjentBehandlingHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                    ),
                )
                oppgaveRepository.lagre(oppgave, ctx)
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
                oppgave.ferdigstill(
                    godkjentBehandlingHendelse =
                        GodkjentBehandlingHendelse(
                            oppgaveId = oppgaveId,
                            meldingOmVedtak = null,
                            utførtAv = saksbehandler,
                        ),
                )
                when (val meldingOmVedtakKilde = oppgave.meldingOmVedtakKilde()) {
                    DP_SAK -> {
                        logger.info { "Oppgave ferdigstilles med melding om vedtak i DP-Sak" }
                        ferdigstillOppgaveMedUtsending(
                            oppgave = oppgave,
                            saksbehandlerToken = saksbehandlerToken,
                        )
                    }

                    GOSYS, INGEN -> {
                        logger.info { "Oppgave ferdigstilles med melding om vedtak. Melding om vedtak kilde = $meldingOmVedtakKilde" }
                        ferdigstillOppgaveUtenUtsending(
                            oppgave = oppgave,
                            saksbehandlerToken = saksbehandlerToken,
                        )
                    }
                }
            }
        }
    }

    private fun ferdigstillOppgaveMedUtsending(
        oppgave: Oppgave,
        saksbehandlerToken: String,
    ) {
        utsendingMediator
            .hentEllerOpprettUtsending(
                behandlingId = oppgave.behandling.behandlingId,
                brev = null,
                ident = oppgave.personIdent(),
            )

        godkjennEllerBeslutt(
            oppgave = oppgave,
            saksbehandlerToken = saksbehandlerToken,
        ).onSuccess {
            oppgaveRepository.lagre(oppgave)
        }.getOrThrow()
    }

    private fun ferdigstillOppgaveUtenUtsending(
        oppgave: Oppgave,
        saksbehandlerToken: String,
    ) {
        godkjennEllerBeslutt(
            oppgave = oppgave,
            saksbehandlerToken = saksbehandlerToken,
        ).onSuccess {
            oppgaveRepository.lagre(oppgave)
        }.getOrThrow()
    }

    fun avbrytOppgave(
        hendelse: BehandlingAvbruttHendelse,
        ctx: Transaksjonskontekst = Transaksjonskontekst.IkkeAktiv,
    ) {
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
                    oppgaveRepository.lagre(oppgave, ctx)
                    utsendingMediator.avbrytUtsendingForBehandling(oppgave.behandling.behandlingId, ctx)
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

    fun finnOppgaverFor(
        ident: String,
        antall: Int? = 50,
    ): List<Oppgave> = oppgaveRepository.finnOppgaverFor(ident, antall)

    fun søk(søkefilter: Søkefilter): OppgaveSøkResultat = oppgaveRepository.søk(søkefilter)

    fun hentDistinkteEmneknagger(): Set<String> = oppgaveRepository.hentDistinkteEmneknagger()

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

    fun håndter(
        vedtakFattetHendelse: VedtakFattetHendelse,
        emneknagger: Set<String> = emptySet(),
    ) {
        withLoggingContext("behandlingId" to vedtakFattetHendelse.behandlingId.toString()) {
            logger.info { "Mottatt VedtakFattetHendelse for behandlingId ${vedtakFattetHendelse.behandlingId}" }
            val oppgave: Oppgave =
                oppgaveRepository.finnOppgaveFor(vedtakFattetHendelse.behandlingId)
                    ?: oppretteOppgaveForAutomatiskFattetVedtak(vedtakFattetHendelse, emneknagger)
            withLoggingContext("oppgaveId" to oppgave.oppgaveId.toString()) {
                logger.info {
                    "Mottatt VedtakFattetHendelse for oppgave i tilstand ${oppgave.tilstand().type}"
                }
                oppgave.ferdigstill(vedtakFattetHendelse).let { handling ->
                    when (handling) {
                        Handling.LAGRE_OPPGAVE -> {
                            oppgaveRepository.lagre(oppgave)
                        }

                        Handling.INGEN -> {}
                    }
                }
                logger.info {
                    "Behandlet VedtakFattetHendelse. Tilstand etter behandling: ${oppgave.tilstand().type}"
                }
            }
        }
    }

    private fun oppretteOppgaveForAutomatiskFattetVedtak(
        vedtakFattetHendelse: VedtakFattetHendelse,
        emneknagger: Set<String>,
    ): Oppgave {
        require(vedtakFattetHendelse.automatiskBehandlet == true) {
            "Mottatt manuell VedtakFattetHendelse uten tilhørende oppgave for " +
                "behandlingId ${vedtakFattetHendelse.behandlingId}. " +
                "Oppgave skal alltid eksistere for manuelle vedtak."
        }

        val sakHistorikk =
            requireNotNull(sakMediator.finnSakHistorikk(vedtakFattetHendelse.ident)) {
                sikkerlogger.error {
                    "Mottatt VedtakFattetHendelse for behandlingId ${vedtakFattetHendelse.behandlingId}, " +
                        "men fant ingen sakshistorikk for ident ${vedtakFattetHendelse.ident}"
                }
                logger.error {
                    "Mottatt VedtakFattetHendelse for behandlingId ${vedtakFattetHendelse.behandlingId}, " +
                        "men fant ingen sakshistorikk. Se sikkerlogg for ident "
                }
            }

        val behandling =
            requireNotNull(sakHistorikk.finnBehandling(vedtakFattetHendelse.behandlingId)) {
                logger.error {
                    "Mottatt VedtakFattetHendelse for behandlingId ${vedtakFattetHendelse.behandlingId}, " +
                        "men fant ingen behandling i sakshistorikken"
                }
            }

        return Oppgave(
            emneknagger = emneknagger,
            opprettet = behandling.opprettet,
            person = sakHistorikk.person,
            behandling = behandling,
            meldingOmVedtak =
                Oppgave.MeldingOmVedtak(
                    kilde = DP_SAK,
                    kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                ),
        ).also {
            logger.info {
                "Opprettet oppgave(${it.oppgaveId}) for behandlingId " +
                    "${vedtakFattetHendelse.behandlingId} ved mottak av VedtakFattetHendelse"
            }
        }
    }

    fun leggTilEmneknagger(
        oppgaveId: UUID,
        emneknagger: Set<String>,
    ) {
        runCatching {
            oppgaveRepository.hentOppgave(oppgaveId).also { oppgave ->
                oppgave.leggTilEmneknagger(emneknagger)
                oppgaveRepository.lagre(oppgave)
            }
        }.onFailure {
            logger.error { "Feil ved legging av emneknagger $emneknagger til oppgave $oppgaveId: ${it.message}" }
        }
    }

    private fun sendSøknadsavklaringBehov(
        oppgave: Oppgave,
        forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ctx: Transaksjonskontekst.Aktiv,
    ) {
        logger.info {
            "Publiserer behov for søknadsinformasjon, behandling ${oppgave.behandling.behandlingId}, oppgave ${oppgave.oppgaveId}"
        }
        utboks.send(
            key = forslagTilVedtakHendelse.ident,
            message =
                JsonMessage
                    .newNeed(
                        setOf(
                            "EØSArbeid",
                            "BostedslandErNorge",
                            "PermittertGrensearbeider",
                            "Sanksjon",
                            "BarnOver16",
                            "PlanleggerUtdanning",
                            "EØSPengestøtte",
                        ),
                        mapOf(
                            "ident" to forslagTilVedtakHendelse.ident,
                            "søknadId" to forslagTilVedtakHendelse.behandletHendelseId,
                            "behandlingId" to oppgave.behandling.behandlingId,
                            "oppgaveId" to oppgave.oppgaveId,
                        ),
                    ).toJson(),
            ctx = ctx,
        )
    }

    private fun loggOgAlertBehandlingIkkeFunnet(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        val feilmelding =
            "Mottatt forslag til vedtak for behandling med id ${forslagTilVedtakHendelse.behandlingId}. " +
                "Fant ikke behandlingen. Gjør derfor ingenting med hendelsen."
        logger.error { feilmelding }
        transaksjoner.transaksjon {
            utboks.send(
                key = forslagTilVedtakHendelse.ident,
                message =
                    AlertManager.alertMessage(
                        feilType = BEHANDLING_IKKE_FUNNET,
                        utvidetFeilMelding = feilmelding,
                    ),
                it,
            )
        }
    }

    private suspend fun feilVedAvvikendeMeldkortSyklus(oppgave: Oppgave) {
        oppgave.søknadId()?.let { søknadId ->
            if (meldekortKontrollKlient
                    .harAvvikendeMeldkortSyklus(
                        ident = oppgave.personIdent(),
                        søknadId = søknadId,
                    ).getOrThrow()
            ) {
                throw HarAvvikendeMeldkortSyklusException(oppgave.behandling.behandlingId)
            }
        }
    }

    private fun godkjennEllerBeslutt(
        oppgave: Oppgave,
        saksbehandlerToken: String,
    ): Result<Unit> {
        val behandlingId = oppgave.behandling.behandlingId
        val ident = oppgave.personIdent()
        return runBlocking {
            behandlingKlient
                .kreverTotrinnskontroll(
                    behandlingId = behandlingId,
                    saksbehandlerToken = saksbehandlerToken,
                ).mapCatching {
                    when (it) {
                        true -> {
                            feilVedAvvikendeMeldkortSyklus(oppgave)
                            behandlingKlient
                                .beslutt(
                                    behandlingId = behandlingId,
                                    ident = ident,
                                    saksbehandlerToken = saksbehandlerToken,
                                ).getOrThrow()
                        }

                        false ->
                            behandlingKlient
                                .godkjenn(
                                    behandlingId = behandlingId,
                                    ident = ident,
                                    saksbehandlerToken = saksbehandlerToken,
                                ).getOrThrow()
                    }
                }
        }
    }
}
