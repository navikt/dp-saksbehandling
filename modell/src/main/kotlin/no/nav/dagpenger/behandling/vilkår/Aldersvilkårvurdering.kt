package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Aldersbehov
import no.nav.dagpenger.behandling.hendelser.AldersvilkårLøsning
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse

class AldersVilkårvurdering : Vilkårsvurdering(IkkeVurdert) {

    object IkkeVurdert : Tilstand {
        override val tilstandType: Tilstand.Type
            get() = Tilstand.Type.IkkeVurdert

        override fun håndter(søknadHendelse: SøknadHendelse, vilkårsvurdering: Vilkårsvurdering) {
            søknadHendelse.behov(Aldersbehov, "Trenger svar på aldersbehov")
            vilkårsvurdering.endreTilstand(nyTilstand = Avventer)
        }
    }

    object Avventer : Tilstand {
        override val tilstandType: Tilstand.Type
            get() = Tilstand.Type.AvventerVurdering

        override fun håndter(aldersvilkårLøsning: AldersvilkårLøsning, vilkårsvurdering: Vilkårsvurdering) {
            if (aldersvilkårLøsning.oppfylt) {
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
