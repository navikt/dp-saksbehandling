package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilbakeTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilbakeTilUnderKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ToTrinnskontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

data class Oppgave private constructor(
    val oppgaveId: UUID,
    val opprettet: LocalDateTime,
    // TODO: Bedre navn a'la borgerIdent?
    val ident: String,
    // TODO: Bedre navn a'la behandlerIdent?
    var saksbehandlerIdent: String? = null,
    val behandlingId: UUID,
    private val _emneknagger: MutableSet<String>,
    private var tilstand: Tilstand = Opprettet,
    val behandling: Behandling,
    private var utsattTil: LocalDate? = null,
) {
    private val tilstandslogg = Tilstandslogg()

    constructor(
        oppgaveId: UUID,
        ident: String,
        behandlingId: UUID,
        emneknagger: Set<String> = emptySet(),
        opprettet: LocalDateTime,
        tilstand: Tilstand = Opprettet,
        behandling: Behandling,
    ) : this(
        oppgaveId = oppgaveId,
        ident = ident,
        saksbehandlerIdent = null,
        opprettet = opprettet,
        _emneknagger = emneknagger.toMutableSet(),
        behandlingId = behandlingId,
        tilstand = tilstand,
        behandling = behandling,
    )

    companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")

        fun rehydrer(
            oppgaveId: UUID,
            ident: String,
            saksbehandlerIdent: String?,
            behandlingId: UUID,
            opprettet: LocalDateTime,
            emneknagger: Set<String>,
            tilstand: Tilstand,
            behandling: Behandling,
            utsattTil: LocalDate?,
        ): Oppgave =
            Oppgave(
                oppgaveId = oppgaveId,
                opprettet = opprettet,
                ident = ident,
                saksbehandlerIdent = saksbehandlerIdent,
                behandlingId = behandlingId,
                _emneknagger = emneknagger.toMutableSet(),
                tilstand = tilstand,
                behandling = behandling,
                utsattTil = utsattTil,
            )
    }

    val emneknagger: Set<String>
        get() = _emneknagger.toSet()

    fun tilstand() = this.tilstand

    fun utsattTil() = this.utsattTil

    fun oppgaveKlarTilBehandling(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        this._emneknagger += forslagTilVedtakHendelse.emneknagger
        tilstand.oppgaveKlarTilBehandling(this, forslagTilVedtakHendelse)
    }

    fun ferdigstill(vedtakFattetHendelse: VedtakFattetHendelse) {
        tilstand.ferdigstill(this, vedtakFattetHendelse)
    }

    fun ferdigstill(godkjentBehandlingHendelse: GodkjentBehandlingHendelse) {
        tilstand.ferdigstill(this, godkjentBehandlingHendelse)
    }

    fun ferdigstill(godkjennBehandlingMedBrevIArena: GodkjennBehandlingMedBrevIArena) {
        tilstand.ferdigstill(this, godkjennBehandlingMedBrevIArena)
    }

    fun fjernAnsvar(oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
        tilstand.fjernAnsvar(this, oppgaveAnsvarHendelse)
    }

    fun tildel(oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
        tilstand.tildel(this, oppgaveAnsvarHendelse)
    }

    fun utsett(utsettOppgaveHendelse: UtsettOppgaveHendelse) {
        tilstand.utsett(this, utsettOppgaveHendelse)
    }

    fun gjørKlarTilKontroll(tilKontrollHendelse: TilKontrollHendelse) {
        tilstand.gjørKlarTilKontroll(this, tilKontrollHendelse)
    }

    fun tildelTotrinnskontroll(toTrinnskontrollHendelse: ToTrinnskontrollHendelse) {
        tilstand.tildelTotrinnskontroll(this, toTrinnskontrollHendelse)
    }

    fun sendTilbakeTilUnderBehandling(oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
        tilstand.sendTilbakeTilUnderBehandling(this, oppgaveAnsvarHendelse)
    }

    fun sendTilbakeTilKlarTilKontroll(tilbakeTilKontrollHendelse: TilbakeTilKontrollHendelse) {
        tilstand.sendTilbakeTilKlarTilKontroll(this, tilbakeTilKontrollHendelse)
    }

    fun sendTilbakeTilUnderKontroll(tilbakeTilUnderKontrollHendelse: TilbakeTilUnderKontrollHendelse) {
        tilstand.sendTilbakeTilUnderKontroll(this, tilbakeTilUnderKontrollHendelse)
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
        this.tilstandslogg.leggTil(nyTilstand, hendelse)
    }

    fun sisteSaksbehandler(): String? {
        return tilstandslogg.firstOrNull { it.tilstand == UNDER_BEHANDLING }?.let {
            it.hendelse.utførtAv.let { aktør ->
                when (aktør) {
                    is Aktør.Saksbehandler -> aktør.navIdent
                    else -> null
                }
            }
        }
    }

    fun sisteBeslutter(): String? {
        return tilstandslogg.firstOrNull { it.tilstand == UNDER_KONTROLL }?.let {
            it.hendelse.utførtAv.let { aktør ->
                when (aktør) {
                    is Aktør.Saksbehandler -> aktør.navIdent
                    is Aktør.Beslutter -> aktør.navIdent
                    else -> null
                }
            }
        }
    }

    object Opprettet : Tilstand {
        override val type: Type = OPPRETTET

        override fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ) {
            oppgave.endreTilstand(KlarTilBehandling, forslagTilVedtakHendelse)
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ) {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
        }
    }

    object KlarTilBehandling : Tilstand {
        override val type: Type = KLAR_TIL_BEHANDLING

        override fun tildel(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(UnderBehandling, oppgaveAnsvarHendelse)
            oppgave.saksbehandlerIdent = oppgaveAnsvarHendelse.navIdent
        }

        override fun fjernAnsvar(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            oppgave.saksbehandlerIdent = null
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ) {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
        }
    }

    object UnderBehandling : Tilstand {
        override val type: Type = UNDER_BEHANDLING

        override fun gjørKlarTilKontroll(
            oppgave: Oppgave,
            tilKontrollHendelse: TilKontrollHendelse,
        ) {
            oppgave.endreTilstand(KlarTilKontroll, tilKontrollHendelse)
            oppgave.saksbehandlerIdent = null
        }

        override fun fjernAnsvar(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(KlarTilBehandling, oppgaveAnsvarHendelse)
            oppgave.saksbehandlerIdent = null
        }

        override fun tildel(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            if (oppgave.saksbehandlerIdent != oppgaveAnsvarHendelse.navIdent) {
                sikkerlogg.warn {
                    "Kan ikke tildele oppgave med id ${oppgave.oppgaveId} til ${oppgaveAnsvarHendelse.navIdent}. " +
                        "Oppgave er allerede tildelt ${oppgave.saksbehandlerIdent}."
                }
                throw AlleredeTildeltException(
                    "Kan ikke tildele oppgave til annen saksbehandler.Oppgave er allerede tildelt.",
                )
            }
        }

        override fun utsett(
            oppgave: Oppgave,
            utsettOppgaveHendelse: UtsettOppgaveHendelse,
        ) {
            oppgave.endreTilstand(PaaVent, utsettOppgaveHendelse)
            oppgave.saksbehandlerIdent =
                when (utsettOppgaveHendelse.beholdOppgave) {
                    true -> utsettOppgaveHendelse.navIdent
                    false -> null
                }
            oppgave.utsattTil = utsettOppgaveHendelse.utsattTil
        }

        override fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ) {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId}" }
        }

        override fun sendTilbakeTilUnderKontroll(
            oppgave: Oppgave,
            tilbakeTilUnderKontrollHendelse: TilbakeTilUnderKontrollHendelse,
        ) {
            oppgave.endreTilstand(UnderKontroll, tilbakeTilUnderKontrollHendelse)
            oppgave.saksbehandlerIdent = tilbakeTilUnderKontrollHendelse.beslutterIdent
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ) {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        ) {
            oppgave.endreTilstand(FerdigBehandlet, godkjentBehandlingHendelse)
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            godkjennBehandlingMedBrevIArena: GodkjennBehandlingMedBrevIArena,
        ) {
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
        ) {
            logger.warn { "Ferdigstiller allerede ferdib behandlet oppgave for behandlingId: ${vedtakFattetHendelse.behandlingId}" }
            sikkerlogg.warn {
                "Ferdigstiller allerede ferdib behandlet oppgave for behandlingId: ${vedtakFattetHendelse.behandlingId}. " +
                    "med vedtakFattetHendelse: $vedtakFattetHendelse "
            }
        }
    }

    object PaaVent : Tilstand {
        override val type: Type = PAA_VENT

        override fun tildel(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(UnderBehandling, oppgaveAnsvarHendelse)
            oppgave.saksbehandlerIdent = oppgaveAnsvarHendelse.navIdent
            oppgave.utsattTil = null
        }

        override fun fjernAnsvar(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(KlarTilBehandling, oppgaveAnsvarHendelse)
            oppgave.saksbehandlerIdent = null
            oppgave.utsattTil = null
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ) {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
        }
    }

    object KlarTilKontroll : Tilstand {
        override val type: Type = KLAR_TIL_KONTROLL

        override fun tildelTotrinnskontroll(
            oppgave: Oppgave,
            toTrinnskontrollHendelse: ToTrinnskontrollHendelse,
        ) {
            oppgave.endreTilstand(UnderKontroll, toTrinnskontrollHendelse)
            oppgave.saksbehandlerIdent = toTrinnskontrollHendelse.beslutterIdent
        }
    }

    object UnderKontroll : Tilstand {
        override val type: Type = UNDER_KONTROLL

        override fun ferdigstill(
            oppgave: Oppgave,
            godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        ) {
            oppgave.endreTilstand(FerdigBehandlet, godkjentBehandlingHendelse)
        }

        override fun ferdigstill(
            oppgave: Oppgave,
            godkjennBehandlingMedBrevIArena: GodkjennBehandlingMedBrevIArena,
        ) {
            oppgave.endreTilstand(FerdigBehandlet, godkjennBehandlingMedBrevIArena)
        }

        override fun sendTilbakeTilUnderBehandling(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(UnderBehandling, oppgaveAnsvarHendelse)
            oppgave.saksbehandlerIdent = oppgaveAnsvarHendelse.navIdent
        }

        override fun sendTilbakeTilKlarTilKontroll(
            oppgave: Oppgave,
            tilbakeTilKontrollHendelse: TilbakeTilKontrollHendelse,
        ) {
            oppgave.endreTilstand(KlarTilKontroll, tilbakeTilKontrollHendelse)
            oppgave.saksbehandlerIdent = null
        }
    }

    interface Tilstand {
        val type: Type

        class UlovligTilstandsendringException(
            message: String,
        ) : RuntimeException(message)

        class UgyldigTilstandException(
            message: String,
        ) : RuntimeException(message)

        companion object {
            fun fra(type: Type) =
                when (type) {
                    OPPRETTET -> Opprettet
                    KLAR_TIL_BEHANDLING -> KlarTilBehandling
                    UNDER_BEHANDLING -> UnderBehandling
                    FERDIG_BEHANDLET -> FerdigBehandlet
                    PAA_VENT -> PaaVent
                    KLAR_TIL_KONTROLL -> KlarTilKontroll
                    UNDER_KONTROLL -> UnderKontroll
                }

            fun fra(type: String) =
                kotlin
                    .runCatching {
                        fra(Type.valueOf(type))
                    }.getOrElse { t ->
                        throw UgyldigTilstandException("Kunne ikke rehydrere til tilstand: $type ${t.message}")
                    }
        }

        enum class Type {
            OPPRETTET,
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            FERDIG_BEHANDLET,
            PAA_VENT,
            KLAR_TIL_KONTROLL,
            UNDER_KONTROLL,
            ;

            companion object {
                val values
                    get() = Type.entries.toSet()

                val søkbareTyper = Type.entries.toSet().minus(OPPRETTET)
                val defaultOppgaveListTilstander =
                    setOf(
                        KLAR_TIL_BEHANDLING,
                        UNDER_BEHANDLING,
                    )
            }
        }

        fun behov() = emptySet<String>()

        fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ) {
            ulovligTilstandsendring("Kan ikke håndtere hendelse om forslag til vedtak i tilstand $type")
        }

        fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ) {
            ulovligTilstandsendring("Kan ikke ferdigstille oppgave i tilstand $type for ${vedtakFattetHendelse.javaClass.simpleName}")
        }

        fun ferdigstill(
            oppgave: Oppgave,
            godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        ) {
            ulovligTilstandsendring("Kan ikke ferdigstille oppgave i tilstand $type for ${godkjentBehandlingHendelse.javaClass.simpleName}")
        }

        fun ferdigstill(
            oppgave: Oppgave,
            godkjennBehandlingMedBrevIArena: GodkjennBehandlingMedBrevIArena,
        ) {
            ulovligTilstandsendring(
                "Kan ikke ferdigstille oppgave i tilstand $type for ${godkjennBehandlingMedBrevIArena.javaClass.simpleName}",
            )
        }

        fun fjernAnsvar(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            ulovligTilstandsendring("Kan ikke håndtere hendelse om fjerne oppgaveansvar i tilstand $type")
        }

        fun tildel(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            ulovligTilstandsendring("Kan ikke håndtere hendelse om å tildele oppgave i tilstand $type")
        }

        fun utsett(
            oppgave: Oppgave,
            utsettOppgaveHendelse: UtsettOppgaveHendelse,
        ) {
            ulovligTilstandsendring("Kan ikke håndtere hendelse om å utsette oppgave i tilstand $type")
        }

        fun gjørKlarTilKontroll(
            oppgave: Oppgave,
            tilKontrollHendelse: TilKontrollHendelse,
        ) {
            ulovligTilstandsendring("Kan ikke håndtere hendelse om å gjøre klar til kontroll i tilstand $type")
        }

        fun tildelTotrinnskontroll(
            oppgave: Oppgave,
            toTrinnskontrollHendelse: ToTrinnskontrollHendelse,
        ) {
            ulovligTilstandsendring("Kan ikke håndtere hendelse om å tildele totrinnskontroll i tilstand $type")
        }

        fun sendTilbakeTilUnderBehandling(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            ulovligTilstandsendring("Kan ikke håndtere hendelse om å sende tilbake fra kontroll i tilstand $type")
        }

        fun sendTilbakeTilKlarTilKontroll(
            oppgave: Oppgave,
            tilbakeTilKontrollHendelse: TilbakeTilKontrollHendelse,
        ) {
            ulovligTilstandsendring("Kan ikke håndtere hendelse om å sende tilbake til klar til kontroll i tilstand $type")
        }

        fun sendTilbakeTilUnderKontroll(
            oppgave: Oppgave,
            tilbakeTilUnderKontrollHendelse: TilbakeTilUnderKontrollHendelse,
        ) {
            ulovligTilstandsendring("Kan ikke håndtere hendelse om å sende tilbake til under kontroll i tilstand $type")
        }

        private fun ulovligTilstandsendring(message: String): Nothing {
            logger.error { message }
            throw UlovligTilstandsendringException(message)
        }
    }
}
