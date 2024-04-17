package no.nav.dagpenger.saksbehandling

import java.util.UUID

class Saksbehandler(
    val navIdent: String,
) {
    private val oppgaver: MutableSet<UUID> = mutableSetOf()

    fun leggTilOppgave(oppgaveId: UUID) {
        oppgaver.add(oppgaveId)
    }
    fun hentAlleOppgaver() = oppgaver.toSet()
}
