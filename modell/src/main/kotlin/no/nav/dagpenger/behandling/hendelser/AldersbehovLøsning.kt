package no.nav.dagpenger.behandling.hendelser

data class AldersbehovLøsning(val ident: String, val oppfylt: Boolean) : Hendelse(ident)
