package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.DagpengerettighetBehov
import no.nav.dagpenger.behandling.hendelser.InngangsvilkårResultat
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
            søknadHendelse.behov(DagpengerettighetBehov, "Trenger svar på retten til dagpenger")
            vilkårsvurdering.endreTilstand(nyTilstand = Avventer)
        }
    }
    object Avventer : Tilstand.Avventer<Inngangsvilkår>() {
        override fun håndter(inngangsvilkårResultat: InngangsvilkårResultat, vilkårsvurdering: Inngangsvilkår) {
            vilkårsvurdering.virkningsdato = inngangsvilkårResultat.virkningsdato
            if (vilkårsvurdering.vilkårsvurderingId == inngangsvilkårResultat.vilkårsvurderingId) {
                if (inngangsvilkårResultat.oppfylt) {
                    vilkårsvurdering.endreTilstand(Oppfylt)
                } else {
                    vilkårsvurdering.endreTilstand(IkkeOppfylt)
                }
            }
        }
    }
    object Oppfylt : Tilstand.Oppfylt<Inngangsvilkår>() {
        override fun accept(paragraf: Inngangsvilkår, visitor: VilkårsvurderingVisitor) {
            visitor.visitInngangsvilkårOppfylt(virkningsdato = paragraf.virkningsdato)
        }
    }
    object IkkeOppfylt : Tilstand.IkkeOppfylt<Inngangsvilkår>() {
        override fun accept(paragraf: Inngangsvilkår, visitor: VilkårsvurderingVisitor) {
            visitor.visitInngangvilkårIkkeOppfylt(virkningsdato = paragraf.virkningsdato)
        }
    }
    override fun <T> implementasjon(block: Inngangsvilkår.() -> T): T = this.block()
}
