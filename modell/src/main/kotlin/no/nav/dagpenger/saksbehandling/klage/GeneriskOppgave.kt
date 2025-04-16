package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.Tilstandslogg
import java.time.LocalDateTime
import java.util.UUID

// TODO: SLETTES - denne har sannsynligvis ikke livets rett. Oppgave er generisk i seg selv.
sealed class GeneriskOppgave(
    val oppgaveId: UUID,
    val opprettet: LocalDateTime,
    private val _emneknagger: MutableSet<String>,
    var behandlerIdent: String? = null,
    private val _tilstandslogg: Tilstandslogg = Tilstandslogg(),
)
