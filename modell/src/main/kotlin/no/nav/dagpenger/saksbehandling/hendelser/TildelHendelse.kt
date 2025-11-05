package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler

data class TildelHendelse(
    override val utførtAv: Saksbehandler,
    override val ansvarligIdent: String,
) : AnsvarHendelse(utførtAv, ansvarligIdent)
