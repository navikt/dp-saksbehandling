package no.nav.dagpenger.saksbehandling.utboks

import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst

interface Utboks {
    fun send(
        key: String,
        message: String,
        ctx: Transaksjonskontekst.Aktiv,
    )
}
