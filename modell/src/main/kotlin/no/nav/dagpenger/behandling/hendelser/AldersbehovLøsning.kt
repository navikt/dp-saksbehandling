package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.Hendelse

data class AldersbehovLøsning(val ident: String, val oppfylt: Boolean) : Hendelse(ident)
