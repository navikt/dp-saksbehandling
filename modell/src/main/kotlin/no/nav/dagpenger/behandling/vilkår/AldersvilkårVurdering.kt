package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.Aldersbehov
import no.nav.dagpenger.behandling.hendelser.AldersbehovLøsning
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse

class AldersVilkårvurdering : Vilkårsvurdering(IkkeVurdert) {

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

        override fun håndter(aldersbehovLøsning: AldersbehovLøsning, vilkårsvurdering: Vilkårsvurdering) {
            if (aldersbehovLøsning.oppfylt) {
                vilkårsvurdering.endreTilstand(Oppfylt)
            } else {
                vilkårsvurdering.endreTilstand(
                    IkkeOppfylt
                )
            }
        }
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
