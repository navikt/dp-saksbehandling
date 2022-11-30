package no.nav.dagpenger.behandling.vilkår

import mu.KotlinLogging
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse

private val logger = KotlinLogging.logger { }

abstract class Vilkårsvurdering {
    abstract var tilstand: Tilstand

    fun håndter(søknadHendelse: SøknadHendelse) {
        tilstand.håndter(søknadHendelse, this)
    }

    fun endreTilstand(nyTilstand: Tilstand) {
        logger.info { "Vurdering ${this.javaClass.simpleName} endrer tilstand fra ${tilstand.tilstandType} til ny tilstand ${nyTilstand.tilstandType}" }
        tilstand = nyTilstand
    }

    interface Tilstand {
        fun håndter(søknadHendelse: SøknadHendelse, vilkårsvurdering: Vilkårsvurdering) {
            TODO("Kan ikke håndtere tilstand")
        }

        val tilstandType: Type

        enum class Type {
            Oppfylt,
            IkkeOppfylt,
            IkkeVurdert,
            AvventerVurdering
        }
    }
}
