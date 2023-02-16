package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Paragraf_4_23_alder
import no.nav.dagpenger.behandling.hendelser.AlderVilkårResultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.visitor.VilkårsvurderingVisitor
import java.time.LocalDate

class AlderVilkår : Vilkårsvurdering<AlderVilkår>(IkkeVurdert) {

    private lateinit var virkningsdato: LocalDate

    override fun accept(visitor: VilkårsvurderingVisitor) {
        super.accept(visitor)
        tilstand.accept(this, visitor)
    }
    object IkkeVurdert : Tilstand.IkkeVurdert<AlderVilkår>() {
        override fun håndter(søknadHendelse: SøknadHendelse, vilkårsvurdering: AlderVilkår) {
            søknadHendelse.behov(Paragraf_4_23_alder, "Trenger svar på aldersbehov")
            vilkårsvurdering.endreTilstand(nyTilstand = Avventer)
        }
    }
    object Avventer : Tilstand.Avventer<AlderVilkår>() {
        override fun håndter(alderVilkårResultat: AlderVilkårResultat, vilkårsvurdering: AlderVilkår) {
            vilkårsvurdering.virkningsdato = alderVilkårResultat.virkningsdato
            if (vilkårsvurdering.vilkårsvurderingId == alderVilkårResultat.vilkårsvurderingId) {
                if (alderVilkårResultat.oppfylt) {
                    vilkårsvurdering.endreTilstand(Oppfylt)
                } else {
                    vilkårsvurdering.endreTilstand(IkkeOppfylt)
                }
            }
        }
    }
    object Oppfylt : Tilstand.Oppfylt<AlderVilkår>() {
        override fun accept(paragraf: AlderVilkår, visitor: VilkårsvurderingVisitor) {
            visitor.visitAlderOppfylt(virkningsdato = paragraf.virkningsdato)
        }
    }
    object IkkeOppfylt : Tilstand.IkkeOppfylt<AlderVilkår>() {
        override fun accept(paragraf: AlderVilkår, visitor: VilkårsvurderingVisitor) {
            visitor.visitAlderIkkeOppfylt(virkningsdato = paragraf.virkningsdato)
        }
    }
    override fun <T> implementasjon(block: AlderVilkår.() -> T): T = this.block()
}
