package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.KlageOppgave.Tilstand.Type.OPPRETTET
import java.time.LocalDateTime
import java.util.UUID

sealed class GeneriskOppgave(
    val oppgaveId: UUID,
    val opprettet: LocalDateTime,
    private val _emneknagger: MutableSet<String>,
    var behandlerIdent: String? = null,
    private val _tilstandslogg: Tilstandslogg = Tilstandslogg(),
)

class KlageOppgave(
    oppgaveId: UUID,
    opprettet: LocalDateTime,
) : GeneriskOppgave(
        oppgaveId = oppgaveId,
        opprettet = opprettet,
        _emneknagger = mutableSetOf(),
    ) {
    private var tilstand: Tilstand = Opprettet

    fun tilstand() = this.tilstand

    sealed interface Tilstand {
        val type: Type

        enum class Type {
            OPPRETTET,
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            FERDIG_BEHANDLET,
        }
    }

    object Opprettet : Tilstand {
        override val type: Tilstand.Type = OPPRETTET
    }
}
