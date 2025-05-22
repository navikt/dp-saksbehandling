package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.FerdigstillBehandling.BESLUTT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.BEHANDLES_I_ARENA
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.EGNE_ANSATTE
import no.nav.dagpenger.saksbehandling.TilgangType.FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND
import no.nav.dagpenger.saksbehandling.hendelser.AnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.PåVentFristUtgåttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SlettNotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

data class Oppgave private constructor(
    val oppgaveId: UUID,
    val opprettet: LocalDateTime,
    var behandlerIdent: String? = null,
    private val _emneknagger: MutableSet<String>,
    private var tilstand: Tilstand = Opprettet,
    val behandling: Behandling,
    private var utsattTil: LocalDate? = null,
    private val _tilstandslogg: Tilstandslogg = Tilstandslogg(),
) {
    constructor(
        oppgaveId: UUID,
        emneknagger: Set<String> = emptySet(),
        opprettet: LocalDateTime,
        tilstand: Tilstand = Opprettet,
        behandlerIdent: String? = null,
        behandling: Behandling,
        tilstandslogg: Tilstandslogg = Tilstandslogg(),
    ) : this(
        oppgaveId = oppgaveId,
        behandlerIdent = behandlerIdent,
        opprettet = opprettet,
        _emneknagger = emneknagger.toMutableSet(),
        tilstand = tilstand,
        behandling = behandling,
        _tilstandslogg = tilstandslogg,
    )

    companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")

        internal const val RETUR_FRA_KONTROLL = "Retur fra kontroll"
        internal const val TIDLIGERE_KONTROLLERT = "Tidligere kontrollert"
        internal val kontrollEmneknagger: Set<String> = setOf(RETUR_FRA_KONTROLL, TIDLIGERE_KONTROLLERT)
        internal val påVentEmneknagger: Set<String> =
            Emneknagg.PåVent.entries.map { påVentÅrsaker ->
                påVentÅrsaker.visningsnavn
            }.toSet()

        fun rehydrer(
            oppgaveId: UUID,
            behandlerIdent: String?,
            opprettet: LocalDateTime,
            emneknagger: Set<String>,
            tilstand: Tilstand,
            behandling: Behandling,
            utsattTil: LocalDate?,
            tilstandslogg: Tilstandslogg = Tilstandslogg(),
        ): Oppgave =
            Oppgave(
                oppgaveId = oppgaveId,
                opprettet = opprettet,
                behandlerIdent = behandlerIdent,
                _emneknagger = emneknagger.toMutableSet(),
                tilstand = tilstand,
                behandling = behandling,
                utsattTil = utsattTil,
                _tilstandslogg = tilstandslogg,
            )

        private fun requireEierskapTilOppgave(
            oppgave: Oppgave,
            saksbehandler: Saksbehandler,
            hendelseNavn: String,
        ) {
            require(oppgave.erEierAvOppgave(saksbehandler)) {
                throw Tilstand.SaksbehandlerEierIkkeOppgaven(
                    "Ulovlig hendelse $hendelseNavn på oppgave i tilstand ${oppgave.tilstand.type} uten å eie oppgaven. " +
                        "Oppgave eies av ${oppgave.behandlerIdent} og ikke ${saksbehandler.navIdent}",
                )
            }
        }

        private fun requireBeslutterTilgang(
            saksbehandler: Saksbehandler,
            tilstandType: Type,
            hendelseNavn: String,
        ) {
            require(saksbehandler.tilganger.contains(BESLUTTER)) {
                throw Tilstand.ManglendeBeslutterTilgang("Kan ikke behandle $hendelseNavn i tilstand $tilstandType uten beslutter-tilgang")
            }
        }

        private fun requireBeslutterUlikSaksbehandler(
            oppgave: Oppgave,
            beslutter: Saksbehandler,
            hendelseNavn: String,
        ) {
            require(oppgave.sisteSaksbehandler() != beslutter.navIdent) {
                throw Tilstand.KanIkkeBeslutteEgenSaksbehandling(
                    "Ulovlig hendelse $hendelseNavn på oppgave i tilstand ${oppgave.tilstand.type}. " +
                        "Oppgave kan ikke behandles og kontrolleres av samme person. Saksbehandler på oppgaven er " +
                        "${oppgave.sisteSaksbehandler()} og kan derfor ikke kontrolleres av ${beslutter.navIdent}",
                )
            }
        }
    }

    val emneknagger: Set<String>
        get() = _emneknagger.toSet()
    val tilstandslogg: Tilstandslogg
        get() = _tilstandslogg

    fun tilstand() = this.tilstand

    fun egneAnsatteTilgangskontroll(saksbehandler: Saksbehandler) {
        require(
            if (this.behandling.person.skjermesSomEgneAnsatte) {
                saksbehandler.tilganger.contains(EGNE_ANSATTE)
            } else {
                true
            },
        ) {
            throw Tilstand.IkkeTilgangTilEgneAnsatte("Saksbehandler har ikke tilgang til egne ansatte")
        }
    }

    fun adressebeskyttelseTilgangskontroll(saksbehandler: Saksbehandler) {
        val adressebeskyttelseGradering = this.behandling.person.adressebeskyttelseGradering
        require(
            when (adressebeskyttelseGradering) {
                FORTROLIG -> saksbehandler.tilganger.contains(FORTROLIG_ADRESSE)
                STRENGT_FORTROLIG -> saksbehandler.tilganger.contains(STRENGT_FORTROLIG_ADRESSE)
                STRENGT_FORTROLIG_UTLAND -> saksbehandler.tilganger.contains(STRENGT_FORTROLIG_ADRESSE_UTLAND)
                UGRADERT -> true
            },
        ) {
            throw Tilstand.ManglendeTilgangTilAdressebeskyttelse(
                "Saksbehandler mangler tilgang til adressebeskyttede personer. Adressebeskyttelse: $adressebeskyttelseGradering",
            )
        }
    }

    fun utsattTil() = this.utsattTil

    fun oppgaveKlarTilBehandling(forslagTilVedtakHendelse: ForslagTilVedtakHendelse): Handling {
        val beholdEmneknagger = this._emneknagger.filter { it in kontrollEmneknagger + påVentEmneknagger }.toSet()
        this._emneknagger.clear()
        this._emneknagger.addAll(forslagTilVedtakHendelse.emneknagger)
        this._emneknagger.addAll(beholdEmneknagger)
        return tilstand.oppgaveKlarTilBehandling(this, forslagTilVedtakHendelse)
    }

    fun ferdigstill(vedtakFattetHendelse: VedtakFattetHendelse): Handling {
        return tilstand.ferdigstill(this, vedtakFattetHendelse)
    }

    fun ferdigstill(godkjentBehandlingHendelse: GodkjentBehandlingHendelse): FerdigstillBehandling {
        adressebeskyttelseTilgangskontroll(godkjentBehandlingHendelse.utførtAv)
        egneAnsatteTilgangskontroll(godkjentBehandlingHendelse.utførtAv)
        return tilstand.ferdigstill(this, godkjentBehandlingHendelse)
    }

    fun ferdigstill(avbruttHendelse: AvbruttHendelse) {
        adressebeskyttelseTilgangskontroll(avbruttHendelse.utførtAv)
        egneAnsatteTilgangskontroll(avbruttHendelse.utførtAv)
        tilstand.ferdigstill(this, avbruttHendelse)
    }

    fun ferdigstill(godkjennBehandlingMedBrevIArena: GodkjennBehandlingMedBrevIArena) {
        adressebeskyttelseTilgangskontroll(godkjennBehandlingMedBrevIArena.utførtAv)
        egneAnsatteTilgangskontroll(godkjennBehandlingMedBrevIArena.utførtAv)
        tilstand.ferdigstill(this, godkjennBehandlingMedBrevIArena)
    }

    fun fjernAnsvar(fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse) {
        tilstand.fjernAnsvar(this, fjernOppgaveAnsvarHendelse)
    }

    fun tildel(settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse) {
        egneAnsatteTilgangskontroll(settOppgaveAnsvarHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(settOppgaveAnsvarHendelse.utførtAv)
        tilstand.tildel(this, settOppgaveAnsvarHendelse)
    }

    fun utsett(utsettOppgaveHendelse: UtsettOppgaveHendelse) {
        egneAnsatteTilgangskontroll(utsettOppgaveHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(utsettOppgaveHendelse.utførtAv)
        tilstand.utsett(this, utsettOppgaveHendelse)
    }

    fun sendTilKontroll(sendTilKontrollHendelse: SendTilKontrollHendelse) {
        egneAnsatteTilgangskontroll(sendTilKontrollHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(sendTilKontrollHendelse.utførtAv)
        tilstand.sendTilKontroll(this, sendTilKontrollHendelse)
    }

    fun behandlesIArena(behandlingAvbruttHendelse: BehandlingAvbruttHendelse) {
        tilstand.behandlesIArena(this, behandlingAvbruttHendelse)
    }

    fun lagreNotat(notatHendelse: NotatHendelse) {
        egneAnsatteTilgangskontroll(notatHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(notatHendelse.utførtAv)
        tilstand.lagreNotat(this, notatHendelse)
    }

    fun slettNotat(slettNotatHendelse: SlettNotatHendelse) {
        egneAnsatteTilgangskontroll(slettNotatHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(slettNotatHendelse.utførtAv)
        tilstand.slettNotat(this, slettNotatHendelse)
    }

    fun returnerTilSaksbehandling(returnerTilSaksbehandlingHendelse: ReturnerTilSaksbehandlingHendelse) {
        tilstand.returnerTilSaksbehandling(this, returnerTilSaksbehandlingHendelse)
    }

    fun oppgaverPåVentMedUtgåttFrist(hendelse: PåVentFristUtgåttHendelse) {
        tilstand.oppgavePåVentMedUtgåttFrist(this, hendelse)
    }

    fun erEierAvOppgave(saksbehandler: Saksbehandler): Boolean = this.behandlerIdent == saksbehandler.navIdent

    private fun endreTilstand(
        nyTilstand: Tilstand,
        hendelse: Hendelse,
    ) {
        logger.info {
            "Endrer tilstand fra ${this.tilstand.type} til ${nyTilstand.type} for oppgaveId: ${this.oppgaveId} " +
                "basert på hendelse: ${hendelse.javaClass.simpleName} "
        }
        this.tilstand = nyTilstand
        this._tilstandslogg.leggTil(nyTilstand.type, hendelse)
    }

    fun sisteSaksbehandler(): String? {
        return runCatching {
            _tilstandslogg.firstOrNull { it.tilstand == UNDER_BEHANDLING && it.hendelse is AnsvarHendelse }?.let {
                (it.hendelse as AnsvarHendelse).ansvarligIdent
            }
        }
            .onFailure { e -> logger.error(e) { "Feil ved henting av siste saksbehandler for oppgave:  ${this.oppgaveId}" } }
            .getOrThrow()
    }

    fun sisteBeslutter(): String? {
        return runCatching {
            _tilstandslogg.firstOrNull { it.tilstand == UNDER_KONTROLL && it.hendelse is AnsvarHendelse }?.let {
                (it.hendelse as AnsvarHendelse).ansvarligIdent
            }
        }
            .onFailure { e -> logger.error(e) { "Feil ved henting av siste beslutter for oppgave:  ${this.oppgaveId}" } }
            .getOrThrow()
    }

    fun soknadId(): UUID? {
        return runCatching {
            _tilstandslogg.firstOrNull { it.hendelse is ForslagTilVedtakHendelse }?.let {
                (it.hendelse as ForslagTilVedtakHendelse).søknadId
            }
        }
            .onFailure { e -> logger.error(e) { "Feil ved henting av søknadId for oppgave:  ${this.oppgaveId}" } }
            .getOrThrow()
    }

    object Opprettet : Tilstand {
        override val type: Type = OPPRETTET

        override fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            oppgave.endreTilstand(KlarTilBehandling, forslagTilVedtakHendelse)
            return Handling.LAGRE_OPPGAVE
        }

        override fun behandlesIArena(
            oppgave: Oppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(BehandlesIArena, behandlingAvbruttHendelse)
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
            return Handling.LAGRE_OPPGAVE
        }
    }

    object KlarTilBehandling : Tilstand {
        override val type: Type = KLAR_TIL_BEHANDLING

        override fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId} i tilstand $type" }
            return Handling.LAGRE_OPPGAVE
        }

        override fun tildel(
            oppgave: Oppgave,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(UnderBehandling, settOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = settOppgaveAnsvarHendelse.ansvarligIdent
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
            return Handling.LAGRE_OPPGAVE
        }

        override fun behandlesIArena(
            oppgave: Oppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(BehandlesIArena, behandlingAvbruttHendelse)
        }
    }

    object UnderBehandling : Tilstand {
        override val type: Type = UNDER_BEHANDLING

        override fun sendTilKontroll(
            oppgave: Oppgave,
            sendTilKontrollHendelse: SendTilKontrollHendelse,
        ) {
            requireEierskapTilOppgave(
                oppgave,
                sendTilKontrollHendelse.utførtAv,
                sendTilKontrollHendelse.javaClass.simpleName,
            )

            if (oppgave.sisteBeslutter() == null) {
                oppgave.behandlerIdent = null
                oppgave.endreTilstand(KlarTilKontroll, sendTilKontrollHendelse)
            } else {
                oppgave.behandlerIdent = oppgave.sisteBeslutter()
                oppgave.endreTilstand(UnderKontroll(), sendTilKontrollHendelse)
                oppgave._emneknagger.add(TIDLIGERE_KONTROLLERT)
                oppgave._emneknagger.remove(RETUR_FRA_KONTROLL)
            }
        }

        override fun fjernAnsvar(
            oppgave: Oppgave,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(KlarTilBehandling, fjernOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = null
        }

        override fun behandlesIArena(
            oppgave: Oppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(BehandlesIArena, behandlingAvbruttHendelse)
        }

        override fun tildel(
            oppgave: Oppgave,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            if (oppgave.behandlerIdent != settOppgaveAnsvarHendelse.ansvarligIdent) {
                sikkerlogg.warn {
                    "Kan ikke tildele oppgave med id ${oppgave.oppgaveId} til ${settOppgaveAnsvarHendelse.ansvarligIdent}. " +
                        "Oppgave er allerede tildelt ${oppgave.behandlerIdent}."
                }
                throw AlleredeTildeltException(
                    "Kan ikke tildele oppgave til annen saksbehandler. Oppgaven er allerede tildelt.",
                )
            }
        }

        override fun utsett(
            oppgave: Oppgave,
            utsettOppgaveHendelse: UtsettOppgaveHendelse,
        ) {
            oppgave._emneknagger.add(utsettOppgaveHendelse.årsak.visningsnavn)
            oppgave.endreTilstand(PåVent, utsettOppgaveHendelse)
            oppgave.behandlerIdent =
                when (utsettOppgaveHendelse.beholdOppgave) {
                    true -> utsettOppgaveHendelse.navIdent
                    false -> null
                }
            oppgave.utsattTil = utsettOppgaveHendelse.utsattTil
        }

        override fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId} i tilstand ${type.name}" }
            return Handling.LAGRE_OPPGAVE
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            when (vedtakFattetHendelse.automatiskBehandlet) {
                true -> {
                    logger.info { "Mottok automatisk behandlet vedtak fattet i tilstand $type. Ferdigstiller oppgave." }
                    oppgave.behandlerIdent = null
                    oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
                    return Handling.LAGRE_OPPGAVE
                }

                else -> {
                    logger.info { "Mottok vedtak fattet i tilstand $type. Ignorerer meldingen." }
                    return Handling.INGEN
                }
            }
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        ): FerdigstillBehandling {
            requireEierskapTilOppgave(
                oppgave = oppgave,
                saksbehandler = godkjentBehandlingHendelse.utførtAv,
                hendelseNavn = godkjentBehandlingHendelse.javaClass.simpleName,
            )
            oppgave.endreTilstand(FerdigBehandlet, godkjentBehandlingHendelse)
            return FerdigstillBehandling.GODKJENN
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            godkjennBehandlingMedBrevIArena: GodkjennBehandlingMedBrevIArena,
        ) {
            requireEierskapTilOppgave(
                oppgave,
                godkjennBehandlingMedBrevIArena.utførtAv,
                godkjennBehandlingMedBrevIArena.javaClass.simpleName,
            )
            oppgave.endreTilstand(FerdigBehandlet, godkjennBehandlingMedBrevIArena)
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            avbruttHendelse: AvbruttHendelse,
        ) {
            requireEierskapTilOppgave(
                oppgave,
                avbruttHendelse.utførtAv,
                avbruttHendelse.javaClass.simpleName,
            )
            oppgave.endreTilstand(FerdigBehandlet, avbruttHendelse)
        }
    }

    class AlleredeTildeltException(
        message: String,
    ) : RuntimeException(message)

    object FerdigBehandlet : Tilstand {
        override val type: Type = FERDIG_BEHANDLET

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            logger.warn { "Mottok vedtak fattet i tilstand $type. Ignorerer meldingen." }
            return Handling.INGEN
        }

        override fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.warn { "Mottok forslagTilVedtakHendelse i tilstand $type. Ignorerer meldingen." }
            return Handling.INGEN
        }
    }

    object BehandlesIArena : Tilstand {
        override val type: Type = BEHANDLES_I_ARENA

        override fun behandlesIArena(
            oppgave: Oppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            logger.info { "Behandling er allerede avbrutt." }
        }
    }

    object PåVent : Tilstand {
        override val type: Type = PAA_VENT

        override fun tildel(
            oppgave: Oppgave,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(UnderBehandling, settOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = settOppgaveAnsvarHendelse.ansvarligIdent
            oppgave.utsattTil = null
        }

        override fun fjernAnsvar(
            oppgave: Oppgave,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(KlarTilBehandling, fjernOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = null
            oppgave.utsattTil = null
        }

        override fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId} i tilstand ${type.name}" }
            return Handling.LAGRE_OPPGAVE
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
            return Handling.LAGRE_OPPGAVE
        }

        override fun behandlesIArena(
            oppgave: Oppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(BehandlesIArena, behandlingAvbruttHendelse)
        }

        override fun oppgavePåVentMedUtgåttFrist(
            oppgave: Oppgave,
            hendelse: PåVentFristUtgåttHendelse,
        ) {
            val nyTilstand =
                if (oppgave.behandlerIdent == null) {
                    KlarTilBehandling
                } else {
                    UnderBehandling
                }
            oppgave.endreTilstand(nyTilstand, hendelse)
            oppgave.utsattTil = null
            oppgave._emneknagger.add(Emneknagg.PåVent.TIDLIGERE_UTSATT.visningsnavn)
        }
    }

    object KlarTilKontroll : Tilstand {
        override val type: Type = KLAR_TIL_KONTROLL

        override fun tildel(
            oppgave: Oppgave,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            requireBeslutterTilgang(
                saksbehandler = settOppgaveAnsvarHendelse.utførtAv,
                tilstandType = type,
                hendelseNavn = settOppgaveAnsvarHendelse.javaClass.simpleName,
            )
            requireBeslutterUlikSaksbehandler(
                oppgave = oppgave,
                beslutter = settOppgaveAnsvarHendelse.utførtAv,
                hendelseNavn = settOppgaveAnsvarHendelse.javaClass.simpleName,
            )

            oppgave.endreTilstand(UnderKontroll(), settOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = settOppgaveAnsvarHendelse.ansvarligIdent
        }

        override fun behandlesIArena(
            oppgave: Oppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(BehandlesIArena, behandlingAvbruttHendelse)
        }
    }

    object AvventerLåsAvBehandling : Tilstand {
        override val type: Type = AVVENTER_LÅS_AV_BEHANDLING
    }

    object AvventerOpplåsingAvBehandling : Tilstand {
        override val type: Type = AVVENTER_OPPLÅSING_AV_BEHANDLING
    }

    data class UnderKontroll(private var notat: Notat? = null) : Tilstand {
        override val type: Type = UNDER_KONTROLL

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            when (vedtakFattetHendelse.automatiskBehandlet) {
                true -> {
                    logger.info { "Mottok automatisk behandlet vedtak fattet i tilstand $type. Ferdigstiller oppgave." }
                    oppgave.behandlerIdent = null
                    oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
                    return Handling.LAGRE_OPPGAVE
                }

                else -> {
                    logger.info { "Mottok vedtak fattet i tilstand $type. Ignorerer meldingen." }
                    return Handling.INGEN
                }
            }
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        ): FerdigstillBehandling {
            requireBeslutterTilgang(
                saksbehandler = godkjentBehandlingHendelse.utførtAv,
                tilstandType = type,
                hendelseNavn = godkjentBehandlingHendelse.javaClass.simpleName,
            )
            requireEierskapTilOppgave(
                oppgave = oppgave,
                saksbehandler = godkjentBehandlingHendelse.utførtAv,
                hendelseNavn = godkjentBehandlingHendelse.javaClass.simpleName,
            )
            requireBeslutterUlikSaksbehandler(
                oppgave = oppgave,
                beslutter = godkjentBehandlingHendelse.utførtAv,
                hendelseNavn = godkjentBehandlingHendelse.javaClass.simpleName,
            )

            oppgave.endreTilstand(FerdigBehandlet, godkjentBehandlingHendelse)
            return BESLUTT
        }

        override fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.warn { "Mottok forslagTilVedtakHendelse i tilstand $type. Ignorerer meldingen." }
            return Handling.INGEN
        }

        override fun tildel(
            oppgave: Oppgave,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            requireEierskapTilOppgave(
                oppgave = oppgave,
                saksbehandler = settOppgaveAnsvarHendelse.utførtAv,
                hendelseNavn = settOppgaveAnsvarHendelse.javaClass.simpleName,
            )
            requireBeslutterUlikSaksbehandler(
                oppgave = oppgave,
                beslutter = settOppgaveAnsvarHendelse.utførtAv,
                hendelseNavn = settOppgaveAnsvarHendelse.javaClass.simpleName,
            )
        }

        override fun returnerTilSaksbehandling(
            oppgave: Oppgave,
            returnerTilSaksbehandlingHendelse: ReturnerTilSaksbehandlingHendelse,
        ) {
            requireBeslutterTilgang(
                saksbehandler = returnerTilSaksbehandlingHendelse.utførtAv,
                tilstandType = type,
                hendelseNavn = returnerTilSaksbehandlingHendelse.javaClass.simpleName,
            )
            requireEierskapTilOppgave(
                oppgave = oppgave,
                saksbehandler = returnerTilSaksbehandlingHendelse.utførtAv,
                hendelseNavn = returnerTilSaksbehandlingHendelse.javaClass.simpleName,
            )
            requireBeslutterUlikSaksbehandler(
                oppgave = oppgave,
                beslutter = returnerTilSaksbehandlingHendelse.utførtAv,
                hendelseNavn = returnerTilSaksbehandlingHendelse.javaClass.simpleName,
            )

            oppgave.endreTilstand(UnderBehandling, returnerTilSaksbehandlingHendelse)
            oppgave.behandlerIdent = oppgave.sisteSaksbehandler()
            oppgave._emneknagger.add(RETUR_FRA_KONTROLL)
            oppgave._emneknagger.remove(TIDLIGERE_KONTROLLERT)
        }

        override fun fjernAnsvar(
            oppgave: Oppgave,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(KlarTilKontroll, fjernOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = null
        }

        override fun behandlesIArena(
            oppgave: Oppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(BehandlesIArena, behandlingAvbruttHendelse)
        }

        override fun lagreNotat(
            oppgave: Oppgave,
            notatHendelse: NotatHendelse,
        ) {
            when (notat) {
                null -> {
                    notat =
                        Notat(
                            notatId = UUIDv7.ny(),
                            tekst = notatHendelse.tekst,
                            sistEndretTidspunkt = LocalDateTime.now(),
                            skrevetAv = notatHendelse.utførtAv.navIdent,
                        )
                }

                else -> {
                    notat?.endreTekst(notatHendelse.tekst)
                }
            }
        }

        override fun slettNotat(
            oppgave: Oppgave,
            slettNotatHendelse: SlettNotatHendelse,
        ) {
            notat = null
        }

        override fun notat(): Notat? = notat
    }

    enum class FerdigstillBehandling {
        BESLUTT,
        GODKJENN,
    }

    enum class Handling {
        LAGRE_OPPGAVE,
        INGEN,
    }

    sealed interface Tilstand {
        val type: Type

        enum class Type {
            OPPRETTET,
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            FERDIG_BEHANDLET,
            PAA_VENT,
            KLAR_TIL_KONTROLL,
            UNDER_KONTROLL,
            AVVENTER_LÅS_AV_BEHANDLING,
            AVVENTER_OPPLÅSING_AV_BEHANDLING,
            BEHANDLES_I_ARENA,
            ;

            companion object {
                val values
                    get() = entries.toSet()

                val søkbareTilstander =
                    entries.toSet()
                        .minus(OPPRETTET)
                        .minus(AVVENTER_LÅS_AV_BEHANDLING)
                        .minus(AVVENTER_OPPLÅSING_AV_BEHANDLING)
            }
        }

        fun notat(): Notat? = null

        fun behov() = emptySet<String>()

        fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om forslag til vedtak i tilstand $type",
            )
        }

        fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message =
                    "Kan ikke ferdigstille oppgave i tilstand $type for " +
                        "${vedtakFattetHendelse.javaClass.simpleName}",
            )
        }

        fun ferdigstill(
            oppgave: Oppgave,
            godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        ): FerdigstillBehandling {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message =
                    "Kan ikke ferdigstille oppgave i tilstand $type for " +
                        "${godkjentBehandlingHendelse.javaClass.simpleName}",
            )
        }

        fun ferdigstill(
            oppgave: Oppgave,
            godkjennBehandlingMedBrevIArena: GodkjennBehandlingMedBrevIArena,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message =
                    "Kan ikke ferdigstille oppgave i tilstand $type for " +
                        "${godkjennBehandlingMedBrevIArena.javaClass.simpleName}",
            )
        }

        fun ferdigstill(
            oppgave: Oppgave,
            avbruttHendelse: AvbruttHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message =
                    "Kan ikke ferdigstille oppgave i tilstand $type for " +
                        "${avbruttHendelse.javaClass.simpleName}",
            )
        }

        fun fjernAnsvar(
            oppgave: Oppgave,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om fjerne oppgaveansvar i tilstand $type",
            )
        }

        fun tildel(
            oppgave: Oppgave,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å tildele oppgave i tilstand $type",
            )
        }

        fun utsett(
            oppgave: Oppgave,
            utsettOppgaveHendelse: UtsettOppgaveHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å utsette oppgave i tilstand $type",
            )
        }

        fun sendTilKontroll(
            oppgave: Oppgave,
            sendTilKontrollHendelse: SendTilKontrollHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å sende til kontroll i tilstand $type",
            )
        }

        fun behandlesIArena(
            oppgave: Oppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om behandling avbrutt i tilstand $type",
            )
        }

        fun returnerTilSaksbehandling(
            oppgave: Oppgave,
            returnerTilSaksbehandlingHendelse: ReturnerTilSaksbehandlingHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å returnere til saksbehandling fra kontroll i tilstand $type",
            )
        }

        fun lagreNotat(
            oppgave: Oppgave,
            notatHendelse: NotatHendelse,
        ) {
            throw RuntimeException("Notat er ikke tillatt i tilstand $type")
        }

        fun slettNotat(
            oppgave: Oppgave,
            slettNotatHendelse: SlettNotatHendelse,
        ) {
            throw RuntimeException("Kan ikke slette notat i tilstand $type")
        }

        fun oppgavePåVentMedUtgåttFrist(
            oppgave: Oppgave,
            hendelse: PåVentFristUtgåttHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å sette utgått frist for oppgave på vent i tilstand $type",
            )
        }

        private fun ulovligTilstandsendring(
            oppgaveId: UUID,
            message: String,
        ): Nothing {
            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                logger.error { message }
            }
            throw UlovligTilstandsendringException(message)
        }

        open class ManglendeTilgang(
            message: String,
        ) : RuntimeException(message)

        class SaksbehandlerEierIkkeOppgaven(
            message: String,
        ) : ManglendeTilgang(message)

        class ManglendeBeslutterTilgang(
            message: String,
        ) : ManglendeTilgang(message)

        class KanIkkeBeslutteEgenSaksbehandling(
            message: String,
        ) : ManglendeTilgang(message)

        class IkkeTilgangTilEgneAnsatte(
            message: String,
        ) : ManglendeTilgang(message)

        class ManglendeTilgangTilAdressebeskyttelse(
            message: String,
        ) : ManglendeTilgang(message)

        class UlovligTilstandsendringException(
            message: String,
        ) : RuntimeException(message)

        class UgyldigTilstandException(
            message: String,
        ) : RuntimeException(message)
    }
}
