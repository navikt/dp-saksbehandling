package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Paragraf_4_23_alder
import no.nav.dagpenger.behandling.hendelser.AlderVilkårResultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.visitor.VilkårsvurderingVisitor
import java.time.LocalDate

class Inngangsvilkår : Vilkårsvurdering<Inngangsvilkår>(IkkeVurdert) {

    private lateinit var virkningsdato: LocalDate

    override fun accept(visitor: VilkårsvurderingVisitor) {
        super.accept(visitor)
        tilstand.accept(this, visitor)
    }
    object IkkeVurdert : Tilstand.IkkeVurdert<Inngangsvilkår>() {
        override fun håndter(søknadHendelse: SøknadHendelse, vilkårsvurdering: Inngangsvilkår) {
            søknadHendelse.behov(Paragraf_4_23_alder, "Trenger svar på aldersbehov")
            vilkårsvurdering.endreTilstand(nyTilstand = Avventer)
        }
    }
    object Avventer : Tilstand.Avventer<Inngangsvilkår>() {
        override fun håndter(alderVilkårResultat: AlderVilkårResultat, vilkårsvurdering: Inngangsvilkår) {
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
    object Oppfylt : Tilstand.Oppfylt<Inngangsvilkår>() {
        override fun accept(paragraf: Inngangsvilkår, visitor: VilkårsvurderingVisitor) {
            visitor.visitAlderOppfylt(virkningsdato = paragraf.virkningsdato)
        }
    }
    object IkkeOppfylt : Tilstand.IkkeOppfylt<Inngangsvilkår>() {
        override fun accept(paragraf: Inngangsvilkår, visitor: VilkårsvurderingVisitor) {
            visitor.visitAlderIkkeOppfylt(virkningsdato = paragraf.virkningsdato)
        }
    }
    override fun <T> implementasjon(block: Inngangsvilkår.() -> T): T = this.block()
}
