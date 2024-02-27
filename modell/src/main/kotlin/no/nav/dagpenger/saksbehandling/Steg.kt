package no.nav.dagpenger.saksbehandling

import java.util.UUID

class Steg private constructor(val navn: String, private val opplysninger: List<Opplysning>) {
    constructor(navn: String, vararg opplysninger: Opplysning) : this(navn, opplysninger.toList())
}
