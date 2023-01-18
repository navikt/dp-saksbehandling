package no.nav.dagpenger.behandling.vilkår

import mu.KotlinLogging
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_løsning
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import java.util.UUID

private val logger = KotlinLogging.logger { }

abstract class Vilkårsvurdering(var tilstand: Tilstand, var vilkårsvurderingId: UUID? = null) {

    companion object {
        fun List<Vilkårsvurdering>.erFerdig() =
            this.none { it.tilstand.tilstandType == Tilstand.Type.AvventerVurdering || it.tilstand.tilstandType == Tilstand.Type.IkkeVurdert }
    }

    fun håndter(søknadHendelse: SøknadHendelse) {
        tilstand.håndter(søknadHendelse, this)
    }

    fun håndter(aldersvilkårLøsning: Paragraf_4_23_alder_løsning) {
        tilstand.håndter(aldersvilkårLøsning, this)
    }

    fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_resultat) {
        tilstand.håndter(paragraf423AlderResultat, this)
    }

    fun endreTilstand(nyTilstand: Tilstand) {
        loggTilstandsendring(nyTilstand)
        tilstand = nyTilstand
    }

    interface Tilstand {
        fun håndter(søknadHendelse: SøknadHendelse, vilkårsvurdering: Vilkårsvurdering) {
            TODO(feilmelding(søknadHendelse))
        }

        fun håndter(aldersvilkårLøsning: Paragraf_4_23_alder_løsning, vilkårsvurdering: Vilkårsvurdering) {
            TODO(feilmelding(aldersvilkårLøsning))
        }
        fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_resultat, vilkårsvurdering: Vilkårsvurdering) {
            TODO(feilmelding(paragraf423AlderResultat))
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
