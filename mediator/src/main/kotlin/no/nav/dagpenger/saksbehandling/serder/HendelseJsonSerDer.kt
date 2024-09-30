package no.nav.dagpenger.saksbehandling.serder

import no.nav.dagpenger.saksbehandling.hendelser.Hendelse

internal fun Hendelse.tilJson(): String {
    return objectMapper.writeValueAsString(this)
}
