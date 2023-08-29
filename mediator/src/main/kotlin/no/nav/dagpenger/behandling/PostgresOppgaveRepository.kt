package no.nav.dagpenger.behandling

import kotliquery.action.UpdateQueryAction
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.dagpenger.behandling.oppgave.OppgaveRepository
import java.util.UUID
import javax.sql.DataSource

class PostgresOppgaveRepository(private val ds: DataSource) : OppgaveRepository {

    internal fun hentBehandling(oppgaveId: UUID): Behandling {
        TODO("")
    }

    internal fun lagreBehandling(behandling: Behandling) {
        using(sessionOf(ds)) { session ->
            session.transaction { tx ->
                behandlingInsertStatementBuilder(behandling).forEach {
                    tx.run(it)
                }
            }
        }
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

    private fun behandlingInsertStatementBuilder(behandling: Behandling): List<UpdateQueryAction> {
        //language=PostgreSQL
        val s1 = queryOf(
            statement = """
               INSERT INTO behandling(person_ident, opprettet, uuid, tilstand, sak_id) VALUES (:person_ident,:opprettet, :uuid, :tilstand, :sak_id)
            """.trimIndent(),
            paramMap = mapOf(
                "person_ident" to behandling.person.ident,
                "opprettet" to behandling.opprettet,
                "uuid" to behandling.uuid,
                "tilstand" to behandling.tilstand.javaClass.name,
                "sak_id" to behandling.sak.id,
            ),
        ).asUpdate

        return listOf(s1)
    }
}
