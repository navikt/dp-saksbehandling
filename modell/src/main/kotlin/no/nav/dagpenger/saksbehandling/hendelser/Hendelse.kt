package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler

sealed class Hendelse(
    open val utførtAv: Behandler,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hendelse

        return utførtAv == other.utførtAv
    }

    override fun hashCode(): Int = utførtAv.hashCode()
}

sealed class AnsvarHendelse(
    utførtAv: Behandler,
    open val ansvarligIdent: String?,
) : Hendelse(utførtAv) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnsvarHendelse

        return (ansvarligIdent == other.ansvarligIdent && super.equals(other))
    }

    override fun hashCode(): Int = (ansvarligIdent?.hashCode() ?: 0) + super.hashCode()
}

data object TomHendelse : Hendelse(utførtAv = Applikasjon("dp-saksbehandling")) {
    fun tilJson(): String = "{}"
}
