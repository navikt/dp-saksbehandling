package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.DagpengerettighetBehov
import no.nav.dagpenger.behandling.entitet.Arbeidstimer
import no.nav.dagpenger.behandling.hendelser.Avslått
import no.nav.dagpenger.behandling.hendelser.InngangsvilkårResultat
import no.nav.dagpenger.behandling.hendelser.Innvilget
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.visitor.VilkårsvurderingVisitor
import java.time.LocalDate

class Inngangsvilkår : Vilkårsvurdering<Inngangsvilkår>(IkkeVurdert) {

    private lateinit var virkningsdato: LocalDate
    private lateinit var fastsattArbeidstimer: Arbeidstimer

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
                when (inngangsvilkårResultat) {
                    is Innvilget -> {
                        vilkårsvurdering.fastsattArbeidstimer = inngangsvilkårResultat.fastsattArbeidstidPerDag
                        vilkårsvurdering.endreTilstand(Oppfylt)
                    }
                    is Avslått -> vilkårsvurdering.endreTilstand(IkkeOppfylt)
                }
            }
        }
    }
    object Oppfylt : Tilstand.Oppfylt<Inngangsvilkår>() {
        override fun accept(vilkår: Inngangsvilkår, visitor: VilkårsvurderingVisitor) {
            visitor.visitInngangsvilkårOppfylt(virkningsdato = vilkår.virkningsdato)
            visitor.visitInngangsvilkårOppfylt(fastsattArbeidstidPerDag = vilkår.fastsattArbeidstimer)
        }
    }
    object IkkeOppfylt : Tilstand.IkkeOppfylt<Inngangsvilkår>() {
        override fun accept(vilkår: Inngangsvilkår, visitor: VilkårsvurderingVisitor) {
            visitor.visitInngangvilkårIkkeOppfylt(virkningsdato = vilkår.virkningsdato)
        }
    }
    override fun <T> implementasjon(block: Inngangsvilkår.() -> T): T = this.block()
}
