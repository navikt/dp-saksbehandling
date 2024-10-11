package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler

sealed class Hendelse(open val utførtAv: Behandler)

sealed class AnsvarHendelse(utførtAv: Behandler, open val ansvarligIdent: String?) : Hendelse(utførtAv)

data object TomHendelse : Hendelse(Applikasjon("dp-saksbehandling")) {
    fun tilJson(): String = "{}"
}
