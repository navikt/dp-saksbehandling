package no.nav.dagpenger.saksbehandling.serder

import no.nav.dagpenger.saksbehandling.hendelser.Hendelse

internal fun Hendelse.tilJson(): String = objectMapper.writeValueAsString(this)

internal inline fun <reified T : Hendelse> String.tilHendelse(): T = objectMapper.readValue(this, T::class.java)
