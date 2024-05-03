package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import java.time.LocalDateTime
import java.util.UUID

data class Oppgave private constructor(
    val oppgaveId: UUID,
    val opprettet: LocalDateTime,
    // TODO: Bedre navn ala brukerIdent
    val ident: String,
    var saksbehandlerIdent: String? = null,
    val behandlingId: UUID,
    private val _emneknagger: MutableSet<String>,
    private var tilstand: Tilstand = Opprettet,
    val behandling: Behandling,
) {
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
        ): Oppgave {
            return Oppgave(
                oppgaveId = oppgaveId,
                opprettet = opprettet,
                ident = ident,
                saksbehandlerIdent = saksbehandlerIdent,
                behandlingId = behandlingId,
                _emneknagger = emneknagger.toMutableSet(),
                tilstand = tilstand,
                behandling = behandling,
            )
        }
    }

    val emneknagger: Set<String>
        get() = _emneknagger.toSet()

    fun tilstand() = this.tilstand.type

    fun oppgaveKlarTilBehandling(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        tilstand.oppgaveKlarTilBehandling(this, forslagTilVedtakHendelse)
    }

    fun ferdigstill(vedtakFattetHendelse: VedtakFattetHendelse) {
        tilstand.ferdigstill(this, vedtakFattetHendelse)
    }

    fun fjernAnsvar(oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
        tilstand.fjernAnsvar(this, oppgaveAnsvarHendelse)
    }

    fun tildel(oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
        tilstand.tildel(this, oppgaveAnsvarHendelse)
    }

    object Opprettet : Tilstand {
        override val type: Tilstand.Type = OPPRETTET

        override fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ) {
            oppgave.tilstand = KlarTilBehandling
        }
    }

    object KlarTilBehandling : Tilstand {
        override val type: Tilstand.Type = KLAR_TIL_BEHANDLING

        override fun tildel(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            oppgave.tilstand = UnderBehandling
            oppgave.saksbehandlerIdent = oppgaveAnsvarHendelse.navIdent
        }
    }

    object UnderBehandling : Tilstand {
        override val type: Tilstand.Type = UNDER_BEHANDLING

        override fun fjernAnsvar(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            oppgave.tilstand = KlarTilBehandling
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
    }

    class AlleredeTildeltException(message: String) : RuntimeException(message)

    object FerdigBehandlet : Tilstand {
        override val type: Tilstand.Type = FERDIG_BEHANDLET
    }

    interface Tilstand {
        val type: Type

        class UlovligTilstandsendringException(message: String) : RuntimeException(message)

        class UkjentTilstandException(message: String) : RuntimeException(message)

        companion object {
            fun fra(type: Type) =
                when (type) {
                    OPPRETTET -> Opprettet
                    KLAR_TIL_BEHANDLING -> KlarTilBehandling
                    UNDER_BEHANDLING -> UnderBehandling
                    FERDIG_BEHANDLET -> FerdigBehandlet
                }

            fun fra(type: String) =
                kotlin.runCatching {
                    fra(Type.valueOf(type))
                }.getOrElse {
                    throw UkjentTilstandException("Kunne ikke rehydrere med ugyldig tilstand: $type")
                }
        }

        enum class Type {
            OPPRETTET,
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            FERDIG_BEHANDLET,
            ;

            companion object {
                val values
                    get() = Type.entries.toSet()
            }
        }

        fun oppgaveKlarTilBehandling(
            oppgave: Oppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ) {
            throw UlovligTilstandsendringException("Kan ikke håndtere hendelse om forslag til vedtak i tilstand $type")
        }

        fun ferdigstill(
            oppgave: Oppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ) {
            oppgave.tilstand = FerdigBehandlet
        }

        fun fjernAnsvar(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            throw UlovligTilstandsendringException("Kan ikke håndtere hendelse om fjerne oppgaveansvar i tilstand $type")
        }

        fun tildel(
            oppgave: Oppgave,
            oppgaveAnsvarHendelse: OppgaveAnsvarHendelse,
        ) {
            throw UlovligTilstandsendringException("Kan ikke håndtere hendelse om å tildele oppgave i tilstand $type")
        }
    }
}
