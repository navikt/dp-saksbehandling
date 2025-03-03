package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon

data class SkriptHendelse(
    override val utførtAv: Applikasjon,
) : Hendelse(utførtAv)
