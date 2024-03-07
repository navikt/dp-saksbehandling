package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import java.time.ZonedDateTime
import java.util.UUID

data class Oppgave private constructor(
    val oppgaveId: UUID,
    val opprettet: ZonedDateTime,
    val ident: String,
    private val _emneknagger: MutableSet<String>,
    val behandlingId: UUID,
    val steg: MutableList<Steg>,
    var tilstand: Tilstand.Type,
) {
    constructor(
        oppgaveId: UUID,
        ident: String,
        emneknagger: Set<String> = emptySet(),
        opprettet: ZonedDateTime,
        behandlingId: UUID,
        tilstand: Tilstand.Type = Tilstand.Type.OPPRETTET,
    ) : this(
        oppgaveId = oppgaveId,
        ident = ident,
        opprettet = opprettet,
        _emneknagger = emneknagger.toMutableSet(),
        steg = mutableListOf(),
        behandlingId = behandlingId,
        tilstand = tilstand,
    )

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

    interface Tilstand {
        enum class Type {
            OPPRETTET,
            FERDIG_BEHANDLET,
            KLAR_TIL_BEHANDLING,
        }
    }
}
