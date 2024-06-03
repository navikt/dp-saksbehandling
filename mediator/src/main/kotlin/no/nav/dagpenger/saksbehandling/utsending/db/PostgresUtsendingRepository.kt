package no.nav.dagpenger.saksbehandling.utsending.db

import no.nav.dagpenger.saksbehandling.utsending.Utsending
import java.util.UUID
import javax.sql.DataSource

class PostgresUtsendingRepository(private val ds: DataSource) : UtsendingRepository {
    override fun lagre(utsending: Utsending) {
        TODO("Not yet implemented")
    }

    override fun hent(oppgaveId: UUID): Utsending {
        TODO("Not yet implemented")
    }
}
