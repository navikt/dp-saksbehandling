package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import java.time.ZonedDateTime
import java.util.UUID

data class Oppgave private constructor(
    val oppgaveId: UUID,
    val opprettet: ZonedDateTime,
    // TODO: Bedre navn ala brukerIdent
    val ident: String,
    var saksbehandlerIdent: String? = null,
    val behandlingId: UUID,
    private val _emneknagger: MutableSet<String>,
    var tilstand: Tilstand.Type,
    private var tilstand2: Tilstand = Opprettet,
) {
    constructor(
        oppgaveId: UUID,
        ident: String,
        behandlingId: UUID,
        emneknagger: Set<String> = emptySet(),
        opprettet: ZonedDateTime,
        tilstand: Tilstand.Type = OPPRETTET,
    ) : this(
        oppgaveId = oppgaveId,
        ident = ident,
        saksbehandlerIdent = null,
        opprettet = opprettet,
        _emneknagger = emneknagger.toMutableSet(),
        behandlingId = behandlingId,
        tilstand = tilstand,
    )

    companion object {
        fun rehydrer(
            oppgaveId: UUID,
            ident: String,
            saksbehandlerIdent: String?,
            behandlingId: UUID,
            opprettet: ZonedDateTime,
            emneknagger: Set<String>,
            tilstand: Tilstand.Type,
            tilstand2: Tilstand,
        ): Oppgave {
            return Oppgave(
                oppgaveId = oppgaveId,
                opprettet = opprettet,
                ident = ident,
                saksbehandlerIdent = saksbehandlerIdent,
                behandlingId = behandlingId,
                _emneknagger = emneknagger.toMutableSet(),
                tilstand = tilstand,
                tilstand2 = tilstand2,
            )
        }
    }

    val emneknagger: Set<String>
        get() = _emneknagger.toSet()

    fun tilstand() = this.tilstand2

    fun oppgaveKlarTilBehandling(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        tilstand2.oppgaveKlarTilBehandling(this, forslagTilVedtakHendelse)
    }

    fun håndter(vedtakFattetHendelse: VedtakFattetHendelse) {
        tilstand2.håndter(this, vedtakFattetHendelse)
    }

    fun fjernAnsvar(oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
        tilstand2.fjernAnsvar(this, oppgaveAnsvarHendelse)
    }

    fun tildel(oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
        tilstand2.tildel(this, oppgaveAnsvarHendelse)
    }

    object Opprettet : Tilstand {
        override val type: Tilstand.Type = OPPRETTET
        override fun oppgaveKlarTilBehandling(oppgave: Oppgave, forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
            oppgave.tilstand2 = KlarTilBehandling
            oppgave.tilstand = KLAR_TIL_BEHANDLING
        }
    }

    object KlarTilBehandling : Tilstand {
        override val type: Tilstand.Type = KLAR_TIL_BEHANDLING
        override fun tildel(oppgave: Oppgave, oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
            oppgave.tilstand2 = UnderBehandling
            oppgave.tilstand = UNDER_BEHANDLING
            oppgave.saksbehandlerIdent = oppgaveAnsvarHendelse.navIdent
        }
    }

    object UnderBehandling : Tilstand {
        override val type: Tilstand.Type = UNDER_BEHANDLING
        override fun fjernAnsvar(oppgave: Oppgave, oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
            oppgave.tilstand2 = KlarTilBehandling
            oppgave.tilstand = KLAR_TIL_BEHANDLING
            oppgave.saksbehandlerIdent = null
        }
    }

    object FerdigBehandlet : Tilstand {
        override val type: Tilstand.Type = FERDIG_BEHANDLET
    }

    interface Tilstand {
        val type: Type
        enum class Type {
            OPPRETTET,
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            FERDIG_BEHANDLET,
        }

        fun oppgaveKlarTilBehandling(oppgave: Oppgave, forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
            throw IllegalStateException("Kan ikke håndtere hendelse om forslag til vedtak i tilstand $type")
        }

        fun håndter(oppgave: Oppgave, vedtakFattetHendelse: VedtakFattetHendelse) {
            oppgave.tilstand2 = FerdigBehandlet
            oppgave.tilstand = FERDIG_BEHANDLET
        }

        fun fjernAnsvar(oppgave: Oppgave, oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
            throw IllegalStateException("Kan ikke håndtere hendelse om fjerne oppgaveansvar i tilstand $type")
        }

        fun tildel(oppgave: Oppgave, oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
            throw IllegalStateException("Kan ikke håndtere hendelse om tildele oppgave i tilstand $type")
        }
    }
}
