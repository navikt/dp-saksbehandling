package no.nav.dagpenger.saksbehandling.db.generell

import no.nav.dagpenger.saksbehandling.generell.GenerellOppgave
import java.util.UUID

interface GenerellOppgaveRepository {
    fun lagre(generellOppgave: GenerellOppgave)

    fun hent(id: UUID): GenerellOppgave

    fun finn(id: UUID): GenerellOppgave?

    fun finnForPerson(ident: String): List<GenerellOppgave>
}
