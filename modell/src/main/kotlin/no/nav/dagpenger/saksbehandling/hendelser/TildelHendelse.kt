package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Behandler

class TildelHendelse(
    utførtAv: Behandler,
    override val ansvarligIdent: String?,
) : AnsvarHendelse(utførtAv, ansvarligIdent)
