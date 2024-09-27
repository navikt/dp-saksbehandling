package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Aktør

data class TilbakeTilUnderKontrollHendelse(val beslutterIdent: String) : Hendelse(Aktør.Beslutter(beslutterIdent))
