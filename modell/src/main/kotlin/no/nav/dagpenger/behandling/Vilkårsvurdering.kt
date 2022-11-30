package no.nav.dagpenger.behandling

import mu.KotlinLogging

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
}

class AldersVilkårsvurdering(override var tilstand: Tilstand = IkkeVurdert) : Vilkårsvurdering() {

    object IkkeVurdert : Tilstand {
        override val tilstandType: Tilstand.Type
            get() = Tilstand.Type.IkkeVurdert

        override fun håndter(søknadHendelse: SøknadHendelse, vilkårsvurdering: Vilkårsvurdering) {
            søknadHendelse.behov(Aldersbehov)
            vilkårsvurdering.endreTilstand(nyTilstand = Avventer)
        }
    }

    object Avventer : Tilstand {
        override val tilstandType: Tilstand.Type
            get() = Tilstand.Type.AvventerVurdering
    }

    object Oppfylt : Tilstand {
        override val tilstandType: Tilstand.Type
            get() = Tilstand.Type.Oppfylt
    }

    object IkkeOppfylt : Tilstand {
        override val tilstandType: Tilstand.Type
            get() = Tilstand.Type.IkkeOppfylt
    }
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
