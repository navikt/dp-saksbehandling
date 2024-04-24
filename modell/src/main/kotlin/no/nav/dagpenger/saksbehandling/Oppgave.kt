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
        ): Oppgave {
            return Oppgave(
                oppgaveId = oppgaveId,
                opprettet = opprettet,
                ident = ident,
                saksbehandlerIdent = saksbehandlerIdent,
                behandlingId = behandlingId,
                _emneknagger = emneknagger.toMutableSet(),
                tilstand = tilstand,
            )
        }
    }

    val emneknagger: Set<String>
        get() = _emneknagger.toSet()

    fun oppgaveKlarTilBehandling(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        // todo Utvide og putt denne logikken inn i tilsand
        if (tilstand == OPPRETTET) {
            tilstand = Tilstand.Type.KLAR_TIL_BEHANDLING
        } else {
            throw IllegalStateException("Kan ikke håndtere hendelse om forslag til vedtak i tilstand $tilstand")
        }
    }

    fun håndter(vedtakFattetHendelse: VedtakFattetHendelse) {
        tilstand = Tilstand.Type.FERDIG_BEHANDLET
    }

    fun fjernAnsvar(oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
        tilstand2.fjernAnsvar(this, oppgaveAnsvarHendelse)
    }

    fun tildel(oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
        tilstand2.tildel(this, oppgaveAnsvarHendelse)
    }

    fun hentTilstand() = this.tilstand2

    object Opprettet : Tilstand {
        override val tilstandType: Tilstand.Type = OPPRETTET
        override fun oppgaveKlarTilBehandling(oppgave: Oppgave, forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
            oppgave.tilstand2 = KlarTilBehandling
        }
    }

    object KlarTilBehandling : Tilstand {
        override val tilstandType: Tilstand.Type = KLAR_TIL_BEHANDLING
        override fun tildel(oppgave: Oppgave, oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
            oppgave.tilstand2 = UnderBehandling
            oppgave.saksbehandlerIdent = oppgaveAnsvarHendelse.navIdent
        }
    }

    object UnderBehandling : Tilstand {
        override val tilstandType: Tilstand.Type = UNDER_BEHANDLING
        override fun fjernAnsvar(oppgave: Oppgave, oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
            oppgave.tilstand2 = KlarTilBehandling
            oppgave.saksbehandlerIdent = null
        }
    }

    object FerdigBehandlet : Tilstand {
        override val tilstandType: Tilstand.Type = FERDIG_BEHANDLET
    }

    interface Tilstand {
        val tilstandType: Type
        enum class Type {
            OPPRETTET,
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            FERDIG_BEHANDLET,
        }

        fun oppgaveKlarTilBehandling(oppgave: Oppgave, forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
            throw IllegalStateException("Kan ikke håndtere hendelse om forslag til vedtak i tilstand $tilstandType")
        }

        fun håndter(oppgave: Oppgave, vedtakFattetHendelse: VedtakFattetHendelse) {
            oppgave.tilstand2 = FerdigBehandlet
        }

        fun fjernAnsvar(oppgave: Oppgave, oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
            throw IllegalStateException("Kan ikke håndtere hendelse om fjerne oppgaveansvar i tilstand $tilstandType")
        }

        fun tildel(oppgave: Oppgave, oppgaveAnsvarHendelse: OppgaveAnsvarHendelse) {
            throw IllegalStateException("Kan ikke håndtere hendelse om tildele oppgave i tilstand $tilstandType")
        }
    }
}
