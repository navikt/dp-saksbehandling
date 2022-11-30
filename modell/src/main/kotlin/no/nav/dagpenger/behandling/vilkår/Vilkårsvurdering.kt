package no.nav.dagpenger.behandling.vilkår

import mu.KotlinLogging
import no.nav.dagpenger.behandling.Hendelse
import no.nav.dagpenger.behandling.hendelser.AldersbehovLøsning
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse

private val logger = KotlinLogging.logger { }

abstract class Vilkårsvurdering(var tilstand: Tilstand) {

    fun håndter(søknadHendelse: SøknadHendelse) {
        tilstand.håndter(søknadHendelse, this)
    }

    fun endreTilstand(nyTilstand: Tilstand) {
        logger.info { "Vurdering ${this.javaClass.simpleName} endrer tilstand fra ${tilstand.tilstandType} til ny tilstand ${nyTilstand.tilstandType}" }
        tilstand = nyTilstand
    }

    fun håndter(aldersbehovLøsning: AldersbehovLøsning) {
        tilstand.håndter(aldersbehovLøsning, this)
    }

    interface Tilstand {
        fun håndter(søknadHendelse: SøknadHendelse, vilkårsvurdering: Vilkårsvurdering) {
            TODO(feilmelding(søknadHendelse))
        }

        fun håndter(aldersbehovLøsning: AldersbehovLøsning, vilkårsvurdering: Vilkårsvurdering) {
            TODO(feilmelding(aldersbehovLøsning))
        }

        private fun feilmelding(søknadHendelse: Hendelse) =
            "Kan ikke håndtere ${søknadHendelse.javaClass.simpleName} i tilstand ${this.tilstandType}"

        val tilstandType: Type

        enum class Type {
            Oppfylt,
            IkkeOppfylt,
            IkkeVurdert,
            AvventerVurdering
        }
    }
}
