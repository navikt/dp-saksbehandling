package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler

data class FjernAnsvarHendelse(
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
