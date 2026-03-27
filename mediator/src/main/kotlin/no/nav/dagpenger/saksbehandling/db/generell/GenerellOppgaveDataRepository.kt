package no.nav.dagpenger.saksbehandling.db.generell

import no.nav.dagpenger.saksbehandling.GenerellOppgaveData
import java.util.UUID

interface GenerellOppgaveDataRepository {
    fun lagre(data: GenerellOppgaveData)

    fun hent(oppgaveId: UUID): GenerellOppgaveData?
}

object NoopGenerellOppgaveDataRepository : GenerellOppgaveDataRepository {
    override fun lagre(data: GenerellOppgaveData) {}

    override fun hent(oppgaveId: UUID): GenerellOppgaveData? = null
}
