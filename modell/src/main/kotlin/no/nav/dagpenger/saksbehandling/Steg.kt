package no.nav.dagpenger.saksbehandling

import java.util.UUID

class Steg private constructor(val stegId: UUID, val navn: String, private val opplysninger: List<Opplysning>) {
    constructor(navn: String, vararg opplysninger: Opplysning) : this(UUIDv7.ny(), navn, opplysninger.toList())
}
