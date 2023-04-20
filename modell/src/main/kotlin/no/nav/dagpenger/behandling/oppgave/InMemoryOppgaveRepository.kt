package no.nav.dagpenger.behandling.oppgave

import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import java.util.UUID

class InMemoryOppgaveRepository : OppgaveRepository {
    private val oppgaver = mutableListOf<Oppgave>()

    init {
        oppgaver.add(SøknadInnsendtHendelse(UUID.randomUUID(), "", "12345678910").oppgave())
        oppgaver.add(SøknadInnsendtHendelse(UUID.randomUUID(), "", "10987654321").oppgave())
    }

    override fun lagreOppgave(oppgave: Oppgave) {
        oppgaver.add(oppgave)
    }

    override fun hentOppgave(uuid: UUID): Oppgave {
        return oppgaver.single { it.uuid == uuid }
    }

    override fun hentOppgaver() = oppgaver

    override fun hentOppgaverFor(fnr: String): List<Oppgave> {
        TODO("Not yet implemented")
    }
}
