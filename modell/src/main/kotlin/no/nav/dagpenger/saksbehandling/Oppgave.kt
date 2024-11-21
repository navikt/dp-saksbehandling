package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
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
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingLåstHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpplåstHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
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
        behandling: Behandling,
        tilstandslogg: Tilstandslogg = Tilstandslogg(),
    ) : this(
        oppgaveId = oppgaveId,
        behandlerIdent = null,
        opprettet = opprettet,
        _emneknagger = emneknagger.toMutableSet(),
        tilstand = tilstand,
        behandling = behandling,
        _tilstandslogg = tilstandslogg,
    )

    companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")

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

        private fun requireSammeEier(
            oppgave: Oppgave,
            saksbehandler: Saksbehandler,
            hendelseNavn: String,
        ) {
            require(oppgave.behandlerIdent == saksbehandler.navIdent) {
                throw Tilstand.ManglendeTilgang(
                    "Ulovlig hendelse $hendelseNavn på oppgave i tilstand ${oppgave.tilstand.type} uten å eie oppgaven. " +
                        "Oppgave eies av ${oppgave.behandlerIdent} og ikke ${saksbehandler.navIdent}",
                )
            }
        }

        private fun requireBeslutterUlikSaksbehandler(
            oppgave: Oppgave,
            beslutter: Saksbehandler,
            hendelseNavn: String,
        ) {
            require(oppgave.sisteSaksbehandler() != beslutter.navIdent) {
                throw Tilstand.ManglendeTilgang(
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
            throw Tilstand.ManglendeTilgang("Saksbehandler har ikke tilgang til egne ansatte")
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
            throw Tilstand.ManglendeTilgang(
                "Saksbehandler mangler tilgang til adressebeskyttede personer. Adressebeskyttelse: $adressebeskyttelseGradering",
            )
        }
    }

    fun utsattTil() = this.utsattTil

    fun oppgaveKlarTilBehandling(forslagTilVedtakHendelse: ForslagTilVedtakHendelse): Boolean {
        this._emneknagger.clear()
        this._emneknagger.addAll(forslagTilVedtakHendelse.emneknagger)
        return tilstand.oppgaveKlarTilBehandling(this, forslagTilVedtakHendelse)
    }

    fun ferdigstill(vedtakFattetHendelse: VedtakFattetHendelse): Boolean {
        return tilstand.ferdigstill(this, vedtakFattetHendelse)
    }

    fun ferdigstill(godkjentBehandlingHendelse: GodkjentBehandlingHendelse) {
        adressebeskyttelseTilgangskontroll(godkjentBehandlingHendelse.utførtAv)
        egneAnsatteTilgangskontroll(godkjentBehandlingHendelse.utførtAv)
        tilstand.ferdigstill(this, godkjentBehandlingHendelse)
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

    fun klarTilKontroll(behandlingLåstHendelse: BehandlingLåstHendelse) {
        tilstand.klarTilKontroll(this, behandlingLåstHendelse)
    }

    fun klarTilBehandling(behandlingOpplåstHendelse: BehandlingOpplåstHendelse) {
        tilstand.klarTilBehandling(this, behandlingOpplåstHendelse)
    }

    fun behandlesIArena(behandlingAvbruttHendelse: BehandlingAvbruttHendelse) {
        tilstand.behandlesIArena(this, behandlingAvbruttHendelse)
    }

    fun lagreNotat(notatHendelse: NotatHendelse) {
        egneAnsatteTilgangskontroll(notatHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(notatHendelse.utførtAv)
        tilstand.lagreNotat(this, notatHendelse)
    }

    fun returnerTilSaksbehandling(returnerTilSaksbehandlingHendelse: ReturnerTilSaksbehandlingHendelse) {
        tilstand.returnerTilSaksbehandling(this, returnerTilSaksbehandlingHendelse)
    }

    fun harVærtITilstand(tilstandType: Type): Boolean {
        this.tilstandslogg.find { it.tilstand == tilstandType }?.let {
            return true
        }
        return false
    }

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
        return kotlin.runCatching {
            _tilstandslogg.firstOrNull { it.tilstand == UNDER_BEHANDLING && it.hendelse is AnsvarHendelse }?.let {
                (it.hendelse as AnsvarHendelse).ansvarligIdent
            }
        }
            .onFailure { e -> logger.error(e) { "Feil ved henting av siste saksbehandler for oppgave:  ${this.oppgaveId}" } }
            .getOrThrow()
    }

    fun sisteBeslutter(): String? {
        return kotlin.runCatching {
            _tilstandslogg.firstOrNull { it.tilstand == UNDER_KONTROLL && it.hendelse is AnsvarHendelse }?.let {
                (it.hendelse as AnsvarHendelse).ansvarligIdent
            }
        }
            .onFailure { e -> logger.error(e) { "Feil ved henting av siste beslutter for oppgave:  ${this.oppgaveId}" } }
            .getOrThrow()
    }

    object Opprettet : Tilstand {
        override val type: Type = OPPRETTET

        override fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Boolean {
            oppgave.endreTilstand(KlarTilBehandling, forslagTilVedtakHendelse)
            return true
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Boolean {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
            return true
        }
    }

    object KlarTilBehandling : Tilstand {
        override val type: Type = KLAR_TIL_BEHANDLING

        override fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Boolean {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId} i tilstand $type" }
            return true
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
        ): Boolean {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
            return true
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
            requireSammeEier(oppgave, sendTilKontrollHendelse.utførtAv, sendTilKontrollHendelse.javaClass.simpleName)

            oppgave.endreTilstand(AvventerLåsAvBehandling, sendTilKontrollHendelse)
            oppgave.behandlerIdent = null
            if (oppgave.harVærtITilstand(UNDER_KONTROLL)) {
                oppgave._emneknagger.add("Tidligere kontrollert")
                oppgave._emneknagger.remove("Retur fra kontroll")
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
        ): Boolean {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId} i tilstand ${type.name}" }
            return true
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Boolean {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
            return true
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        ) {
            requireSammeEier(
                oppgave,
                godkjentBehandlingHendelse.utførtAv,
                godkjentBehandlingHendelse.javaClass.simpleName,
            )
            oppgave.endreTilstand(FerdigBehandlet, godkjentBehandlingHendelse)
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            godkjennBehandlingMedBrevIArena: GodkjennBehandlingMedBrevIArena,
        ) {
            requireSammeEier(
                oppgave,
                godkjennBehandlingMedBrevIArena.utførtAv,
                godkjennBehandlingMedBrevIArena.javaClass.simpleName,
            )
            oppgave.endreTilstand(FerdigBehandlet, godkjennBehandlingMedBrevIArena)
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
        ): Boolean {
            logger.info { "Oppgave er allerede ferdigstilt for behandlingId: ${vedtakFattetHendelse.behandlingId}" }
            sikkerlogg.info {
                "Oppgave er allerede ferdigstilt for behandlingId: ${vedtakFattetHendelse.behandlingId}. " +
                    "VedtakFattetHendelse: $vedtakFattetHendelse "
            }
            return false
        }
    }

    object BehandlesIArena : Tilstand {
        override val type: Type = BEHANDLES_I_ARENA
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
        ): Boolean {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId} i tilstand ${type.name}" }
            return true
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Boolean {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
            return true
        }

        override fun behandlesIArena(
            oppgave: Oppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(BehandlesIArena, behandlingAvbruttHendelse)
        }
    }

    object KlarTilKontroll : Tilstand {
        override val type: Type = KLAR_TIL_KONTROLL

        override fun tildel(
            oppgave: Oppgave,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            require(settOppgaveAnsvarHendelse.utførtAv.tilganger.contains(BESLUTTER)) {
                throw Tilstand.ManglendeTilgang("Kan ikke ta oppgave til totrinnskontroll i tilstand $type uten beslutter-tilgang")
            }
            requireBeslutterUlikSaksbehandler(
                oppgave = oppgave,
                beslutter = settOppgaveAnsvarHendelse.utførtAv,
                hendelseNavn = settOppgaveAnsvarHendelse.javaClass.simpleName,
            )

            oppgave.endreTilstand(UnderKontroll(), settOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = settOppgaveAnsvarHendelse.ansvarligIdent
        }
    }

    object AvventerLåsAvBehandling : Tilstand {
        override val type: Type = AVVENTER_LÅS_AV_BEHANDLING

        override fun klarTilKontroll(
            oppgave: Oppgave,
            behandlingLåstHendelse: BehandlingLåstHendelse,
        ) {
            if (oppgave.sisteBeslutter() == null) {
                oppgave.endreTilstand(KlarTilKontroll, behandlingLåstHendelse)
            } else {
                oppgave.behandlerIdent = oppgave.sisteBeslutter()
                oppgave.endreTilstand(UnderKontroll(), behandlingLåstHendelse)
            }
        }
    }

    object AvventerOpplåsingAvBehandling : Tilstand {
        override val type: Type = AVVENTER_OPPLÅSING_AV_BEHANDLING

        override fun klarTilBehandling(
            oppgave: Oppgave,
            behandlingOpplåstHendelse: BehandlingOpplåstHendelse,
        ) {
            oppgave.behandlerIdent = oppgave.sisteSaksbehandler()
            oppgave.endreTilstand(UnderBehandling, behandlingOpplåstHendelse)
        }

        override fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Boolean {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId} i tilstand ${type.name}" }
            return false
        }
    }

    data class UnderKontroll(private var notat: Notat? = null) : Tilstand {
        override val type: Type = UNDER_KONTROLL

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Boolean {
            logger.warn { "Mottok vedtak fattet i tilstand: $type for behandlingId: ${vedtakFattetHendelse.behandlingId}." }
            sikkerlogg.warn {
                "Mottok vedtak fattet i tilstand: $type. VedtakFattetHendelse: $vedtakFattetHendelse "
            }
            return false
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        ) {
            require(godkjentBehandlingHendelse.utførtAv.tilganger.contains(BESLUTTER)) {
                throw Tilstand.ManglendeTilgang("Kan ikke ferdigstille oppgave i tilstand $type uten beslutter-tilgang")
            }
            requireSammeEier(
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
        }

        override fun tildel(
            oppgave: Oppgave,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            requireSammeEier(
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
            require(returnerTilSaksbehandlingHendelse.utførtAv.tilganger.contains(BESLUTTER)) {
                throw Tilstand.ManglendeTilgang(
                    "Kan ikke returnere oppgaven til saksbehandling i tilstand $type " +
                        "uten beslutter-tilgang",
                )
            }
            requireSammeEier(
                oppgave = oppgave,
                saksbehandler = returnerTilSaksbehandlingHendelse.utførtAv,
                hendelseNavn = returnerTilSaksbehandlingHendelse.javaClass.simpleName,
            )
            requireBeslutterUlikSaksbehandler(
                oppgave = oppgave,
                beslutter = returnerTilSaksbehandlingHendelse.utførtAv,
                hendelseNavn = returnerTilSaksbehandlingHendelse.javaClass.simpleName,
            )

            oppgave.endreTilstand(AvventerOpplåsingAvBehandling, returnerTilSaksbehandlingHendelse)
            oppgave.behandlerIdent = null
            oppgave._emneknagger.add("Retur fra kontroll")
            oppgave._emneknagger.remove("Tidligere kontrollert")
        }

        override fun fjernAnsvar(
            oppgave: Oppgave,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(KlarTilKontroll, fjernOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = null
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

        override fun notat(): Notat? = notat
    }

    sealed interface Tilstand {
        val type: Type

        class ManglendeTilgang(
            message: String,
        ) : RuntimeException(message)

        class UlovligTilstandsendringException(
            message: String,
        ) : RuntimeException(message)

        class UgyldigTilstandException(
            message: String,
        ) : RuntimeException(message)

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

                val søkbareTyper = entries.toSet().minus(OPPRETTET)
                val defaultOppgaveListTilstander =
                    setOf(
                        KLAR_TIL_BEHANDLING,
                        UNDER_BEHANDLING,
                    )
            }
        }

        fun notat(): Notat? = null

        fun behov() = emptySet<String>()

        fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Boolean {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om forslag til vedtak i tilstand $type",
            )
        }

        fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Boolean {
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
        ) {
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

        fun klarTilKontroll(
            oppgave: Oppgave,
            behandlingLåstHendelse: BehandlingLåstHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om låst behandling i tilstand $type",
            )
        }

        fun klarTilBehandling(
            oppgave: Oppgave,
            behandlingOpplåstHendelse: BehandlingOpplåstHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om opplåst behandling i tilstand $type",
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

        private fun ulovligTilstandsendring(
            oppgaveId: UUID,
            message: String,
        ): Nothing {
            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                logger.error { message }
            }
            throw UlovligTilstandsendringException(message)
        }
    }
}
