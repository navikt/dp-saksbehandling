package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør

sealed class Hendelse(val utførtAv: Aktør)

sealed class AnsvarHendelse(utførtAv: Aktør, val ansvarligIdent: String?) : Hendelse(utførtAv)

data object TomHendelse : Hendelse(Aktør.System.dpSaksbehandling)
