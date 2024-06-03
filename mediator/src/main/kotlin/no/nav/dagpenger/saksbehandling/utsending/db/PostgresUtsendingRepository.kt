package no.nav.dagpenger.saksbehandling.utsending.db

import no.nav.dagpenger.saksbehandling.utsending.Utsending
import javax.sql.DataSource

class PostgresUtsendingRepository(private val ds: DataSource) : UtsendingRepository {
    override fun lagre(utsending: Utsending) {
        TODO("Not yet implemented")
    }
}