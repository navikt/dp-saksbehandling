package no.nav.dagpenger.saksbehandling.outbox

import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst

/**
 * Mediator-seam for å enqueue meldinger til outbox i en delt transaksjon.
 * Delegerer persistens til [OutboxRepository].
 */
class PostgresOutbox(
    private val repository: OutboxRepository,
) : Outbox {
    override fun send(
        key: String,
        message: String,
        ctx: Transaksjonskontekst.Aktiv,
    ) = repository.lagre(key = key, message = message, ctx = ctx)
}
