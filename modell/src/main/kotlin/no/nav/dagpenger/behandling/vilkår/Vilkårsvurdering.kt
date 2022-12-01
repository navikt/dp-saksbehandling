package no.nav.dagpenger.behandling.vilkår

import mu.KotlinLogging
import no.nav.dagpenger.behandling.hendelser.AldersbehovLøsning
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse

private val logger = KotlinLogging.logger { }

abstract class Vilkårsvurdering(var tilstand: Tilstand) {

    companion object {
        fun List<Vilkårsvurdering>.erFerdig() =
            this.none { it.tilstand.tilstandType == Tilstand.Type.AvventerVurdering || it.tilstand.tilstandType == Tilstand.Type.IkkeVurdert }
    }

    fun håndter(søknadHendelse: SøknadHendelse) {
        tilstand.håndter(søknadHendelse, this)
    }

    fun håndter(aldersbehovLøsning: AldersbehovLøsning) {
        tilstand.håndter(aldersbehovLøsning, this)
    }

    fun endreTilstand(nyTilstand: Tilstand) {
        loggTilstandsendring(nyTilstand)
        tilstand = nyTilstand
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

    private fun loggTilstandsendring(nyTilstand: Tilstand) {
        logger.info { "Vurdering ${this.javaClass.simpleName} endrer tilstand fra ${tilstand.tilstandType} til ny tilstand ${nyTilstand.tilstandType}" }
    }
}
