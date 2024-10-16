package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler

data class NesteOppgaveHendelse(
    override val ansvarligIdent: String,
    override val utførtAv: Saksbehandler,
) : AnsvarHendelse(utførtAv, ansvarligIdent)
