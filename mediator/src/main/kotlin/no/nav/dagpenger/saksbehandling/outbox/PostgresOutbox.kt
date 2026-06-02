package no.nav.dagpenger.saksbehandling.outbox

import kotliquery.queryOf
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst

class PostgresOutbox : Outbox {
    override fun send(
        key: String,
        message: String,
        ctx: Transaksjonskontekst.Aktiv,
    ) {
        ctx.session.run(
            queryOf(
                //language=PostgreSQL
                statement = "INSERT INTO outbox (key, message) VALUES (:key, :message)",
                paramMap = mapOf("key" to key, "message" to message),
            ).asUpdate,
        )
    }
}
