package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.dagpenger.behandling.oppgave.OppgaveRepository
import java.util.UUID
import javax.sql.DataSource

class PostgresOppgaveRepository(dataSource: DataSource) : OppgaveRepository {

    internal fun hentBehandling(oppgaveId: UUID): Behandling {
        TODO("")
    }

    internal fun lagreBehandling(behandling: Behandling) {
        TODO("")
    }

    override fun lagreOppgave(oppgave: Oppgave) {
        TODO("Not yet implemented")
    }

    override fun hentOppgave(uuid: UUID): Oppgave {
        TODO("Not yet implemented")
    }

    override fun hentOppgaver(): List<Oppgave> {
        TODO("Not yet implemented")
    }

    override fun hentOppgaverFor(fnr: String): List<Oppgave> {
        TODO("Not yet implemented")
    }
}
