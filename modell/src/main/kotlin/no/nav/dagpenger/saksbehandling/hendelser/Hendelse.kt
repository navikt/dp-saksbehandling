package no.nav.dagpenger.saksbehandling.hendelser

sealed class Hendelse(open val utførtAv: String)

sealed class AnsvarHendelse(utførtAv: String, open val ansvarligIdent: String?) : Hendelse(utførtAv)

data object TomHendelse : Hendelse("dp-saksbehandling") {
    fun tilJson(): String = "{}"
}
