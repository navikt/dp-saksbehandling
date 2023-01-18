package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Paragraf_4_23_alder
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_løsning
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse

class Paragraf_4_23_alder_vilkår : Vilkårsvurdering(IkkeVurdert) {

    object IkkeVurdert : Tilstand {
        override val tilstandType: Tilstand.Type
            get() = Tilstand.Type.IkkeVurdert

        override fun håndter(søknadHendelse: SøknadHendelse, vilkårsvurdering: Vilkårsvurdering) {
            søknadHendelse.behov(Paragraf_4_23_alder, "Trenger svar på aldersbehov")
            vilkårsvurdering.endreTilstand(nyTilstand = Avventer)
        }
    }

    object Avventer : Tilstand {
        override val tilstandType: Tilstand.Type
            get() = Tilstand.Type.AvventerVurdering

        override fun håndter(aldersvilkårLøsning: Paragraf_4_23_alder_løsning, vilkårsvurdering: Vilkårsvurdering) {
            vilkårsvurdering.vilkårsvurderingId = aldersvilkårLøsning.vilkårvurderingId
        }

        override fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_resultat, vilkårsvurdering: Vilkårsvurdering) {
            if (vilkårsvurdering.vilkårsvurderingId == paragraf423AlderResultat.vilkårvurderingId) {
                if (paragraf423AlderResultat.oppfylt) {
                    vilkårsvurdering.endreTilstand(Oppfylt)
                } else {
                    vilkårsvurdering.endreTilstand(IkkeOppfylt)
                }
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
