package no.nav.dagpenger.saksbehandling.sak

class NødbremsetPersonException(
    val ident: String,
) : RuntimeException("Personen er nodbremset")
