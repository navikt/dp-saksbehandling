package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.entitet.Periode
import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.mengde.Stønadsperiode
import no.nav.dagpenger.behandling.visitor.PersonVisitor
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class LøpendeStønadsperiodeVilkår(private val person: Person) :
    Vilkårsvurdering<LøpendeStønadsperiodeVilkår>(IkkeVurdert) {

    object IkkeVurdert : Tilstand.IkkeVurdert<LøpendeStønadsperiodeVilkår>() {
        override fun håndter(
            rapporteringsHendelse: RapporteringsHendelse,
            vilkårsvurdering: LøpendeStønadsperiodeVilkår
        ) {
            if (HarLøpendeVedtakVisitor(vilkårsvurdering.person, rapporteringsHendelse.tilPeriode()).harGjenstående()) {
                vilkårsvurdering.endreTilstand(nyTilstand = Oppfylt)
            } else {
                vilkårsvurdering.endreTilstand(nyTilstand = IkkeOppfylt)
            }
        }
    }

    object Oppfylt : Tilstand.Oppfylt<LøpendeStønadsperiodeVilkår>()

    object IkkeOppfylt : Tilstand.IkkeOppfylt<LøpendeStønadsperiodeVilkår>()

    override fun <T> implementasjon(block: LøpendeStønadsperiodeVilkår.() -> T): T {
        return this.block()
    }

    private class HarLøpendeVedtakVisitor(person: Person, private val periode: Periode) : PersonVisitor {
        init {
            person.accept(this)
        }

        fun harGjenstående() = harGjenstående && harVedtak

        private var harGjenstående = false
        private var harVedtak = false

        override fun visitGjenståendeStønadsperiode(gjenståendePeriode: Stønadsperiode) {
            harGjenstående = true
        }

        override fun preVisitVedtak(
            vedtakId: UUID,
            virkningsdato: LocalDate,
            vedtakstidspunkt: LocalDateTime,
            utfall: Boolean
        ) {
            if (virkningsdato <= periode.endInclusive) {
                harVedtak = true
            }
        }
    }
}
