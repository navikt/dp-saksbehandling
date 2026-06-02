package no.nav.dagpenger.saksbehandling.outbox

import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst

interface Outbox {
    fun send(
        key: String,
        message: String,
        ctx: Transaksjonskontekst.Aktiv,
    )
}
