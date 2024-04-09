package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import java.time.ZonedDateTime
import java.util.UUID

data class Oppgave private constructor(
    val oppgaveId: UUID,
    val opprettet: ZonedDateTime,
    val ident: String,
    val behandlingId: UUID,
    private val _emneknagger: MutableSet<String>,
    var tilstand: Tilstand.Type,
) {
    constructor(
        oppgaveId: UUID,
        ident: String,
        behandlingId: UUID,
        emneknagger: Set<String> = emptySet(),
        opprettet: ZonedDateTime,
        tilstand: Tilstand.Type = Tilstand.Type.OPPRETTET,
    ) : this(
        oppgaveId = oppgaveId,
        ident = ident,
        opprettet = opprettet,
        _emneknagger = emneknagger.toMutableSet(),
        behandlingId = behandlingId,
        tilstand = tilstand,
    )

    companion object {
        fun rehydrer(
            oppgaveId: UUID,
            ident: String,
            behandlingId: UUID,
            opprettet: ZonedDateTime,
            emneknagger: Set<String>,
            tilstand: Tilstand.Type,
        ): Oppgave {
            return Oppgave(
                oppgaveId = oppgaveId,
                opprettet = opprettet,
                ident = ident,
                behandlingId = behandlingId,
                _emneknagger = emneknagger.toMutableSet(),
                tilstand = tilstand,
            )
        }
    }

    val emneknagger: Set<String>
        get() = _emneknagger.toSet()

    fun håndter(forslagTilVedtakHendelse: ForslagTilVedtakHendelse) {
        // todo Utvide og putt denne logikken inn i tilsand
        if (tilstand == Tilstand.Type.OPPRETTET) {
            tilstand = Tilstand.Type.KLAR_TIL_BEHANDLING
        } else {
            throw IllegalStateException("Kan ikke håndtere hendelse om forslag til vedtak i tilstand $tilstand")
        }
    }

    fun håndter(vedtakFattetHendelse: VedtakFattetHendelse) {
        if (tilstand == Tilstand.Type.KLAR_TIL_BEHANDLING) {
            tilstand = Tilstand.Type.FERDIG_BEHANDLET
        } else {
            throw IllegalStateException("Kan ikke håndtere hendelse om vedtak fattet i tilstand $tilstand")
        }
    }

    interface Tilstand {
        enum class Type {
            OPPRETTET,
            FERDIG_BEHANDLET,
            KLAR_TIL_BEHANDLING,
        }
    }
}
