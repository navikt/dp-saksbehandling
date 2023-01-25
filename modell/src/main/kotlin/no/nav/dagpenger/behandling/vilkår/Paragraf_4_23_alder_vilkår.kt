package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Paragraf_4_23_alder
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse

class Paragraf_4_23_alder_vilkår : Vilkårsvurdering<Paragraf_4_23_alder_vilkår>(IkkeVurdert) {
    object IkkeVurdert : Tilstand.IkkeVurdert<Paragraf_4_23_alder_vilkår>() {
        override fun håndter(søknadHendelse: SøknadHendelse, vilkårsvurdering: Paragraf_4_23_alder_vilkår) {
            søknadHendelse.behov(Paragraf_4_23_alder, "Trenger svar på aldersbehov")
            vilkårsvurdering.endreTilstand(nyTilstand = Avventer)
        }
    }
    object Avventer : Tilstand.Avventer<Paragraf_4_23_alder_vilkår>() {
        override fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_resultat, vilkårsvurdering: Paragraf_4_23_alder_vilkår) {
            if (vilkårsvurdering.vilkårsvurderingId == paragraf423AlderResultat.vilkårsvurderingId) {
                if (paragraf423AlderResultat.oppfylt) {
                    vilkårsvurdering.endreTilstand(Oppfylt)
                } else {
                    vilkårsvurdering.endreTilstand(IkkeOppfylt)
                }
            }
        }
    }
    object Oppfylt : Tilstand.Oppfylt<Paragraf_4_23_alder_vilkår>()
    object IkkeOppfylt : Tilstand.IkkeOppfylt<Paragraf_4_23_alder_vilkår>()
    override fun <T> implementasjon(block: Paragraf_4_23_alder_vilkår.() -> T): T = this.block()
}
