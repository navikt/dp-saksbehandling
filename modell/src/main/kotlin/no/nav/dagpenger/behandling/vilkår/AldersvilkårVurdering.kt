package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.Aldersbehov
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse

class AldersVilkårvurdering(override var tilstand: Tilstand = IkkeVurdert) : Vilkårsvurdering() {

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
