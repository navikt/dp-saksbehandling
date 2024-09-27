package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør

sealed class Hendelse(val utførtAv: Aktør)

data object TomHendelse : Hendelse(Aktør.System.dpSaksbehandling)
