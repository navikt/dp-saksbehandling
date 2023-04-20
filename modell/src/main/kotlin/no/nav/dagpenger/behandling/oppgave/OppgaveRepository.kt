package no.nav.dagpenger.behandling.oppgave

import java.util.UUID

interface OppgaveRepository {
    fun lagre(oppgave: Oppgave)
    fun hentOppgave(uuid: UUID): Oppgave
}
