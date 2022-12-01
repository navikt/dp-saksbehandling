package no.nav.dagpenger.behandling.hendelser

data class AldersvilkårLøsning(val ident: String, val oppfylt: Boolean) : Hendelse(ident)
