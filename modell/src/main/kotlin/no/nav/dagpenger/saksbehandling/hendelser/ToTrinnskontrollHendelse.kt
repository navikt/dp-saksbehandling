package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør

data class ToTrinnskontrollHendelse(
    override val ansvarligIdent: String,
    override val utførtAv: Aktør,
) : AnsvarHendelse(ansvarligIdent = ansvarligIdent, utførtAv = utførtAv)
