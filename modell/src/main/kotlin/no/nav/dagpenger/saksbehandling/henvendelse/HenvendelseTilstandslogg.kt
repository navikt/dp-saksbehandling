package no.nav.dagpenger.saksbehandling.henvendelse

import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand
import java.util.UUID

class HenvendelseTilstandslogg(
    tilstandsendringer: List<Tilstandsendring<Tilstand.Type>> = listOf(),
) : Tilstandslogg<Tilstand.Type>(tilstandsendringer.toMutableList()) {
    fun inneholderHendelseMedSøknadId(søknadId: UUID): Boolean {
        TODO("Dette fikser Giao i kveld :) ")
    }

    constructor(vararg tilstandsendringer: Tilstandsendring<Tilstand.Type>) : this(tilstandsendringer.toMutableList())
}
