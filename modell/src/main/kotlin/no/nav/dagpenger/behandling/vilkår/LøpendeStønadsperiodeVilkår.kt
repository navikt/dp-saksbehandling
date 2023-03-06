package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.entitet.Dagpengerettighet
import no.nav.dagpenger.behandling.entitet.Periode
import no.nav.dagpenger.behandling.entitet.Prosent
import no.nav.dagpenger.behandling.entitet.Timer
import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.mengde.Stønadsperiode
import no.nav.dagpenger.behandling.rapportering.Arbeidsdag
import no.nav.dagpenger.behandling.rapportering.Dag
import no.nav.dagpenger.behandling.rapportering.Dag.Companion.summer
import no.nav.dagpenger.behandling.rapportering.Helgedag
import no.nav.dagpenger.behandling.vilkår.LøpendeStønadsperiodeVilkår.IkkeOppfylt
import no.nav.dagpenger.behandling.vilkår.LøpendeStønadsperiodeVilkår.Oppfylt
import no.nav.dagpenger.behandling.visitor.PersonVisitor
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class LøpendeStønadsperiodeVilkår(private val person: Person) :
    Vilkårsvurdering<LøpendeStønadsperiodeVilkår>(IkkeVurdert) {

    object IkkeVurdert : Tilstand.IkkeVurdert<LøpendeStønadsperiodeVilkår>() {
        override fun håndter(
            rapporteringsHendelse: RapporteringsHendelse,
            vilkårsvurdering: LøpendeStønadsperiodeVilkår,
        ) {
            val harGjenstående =
                HarGjenstående(vilkårsvurdering.person, rapporteringsHendelse.somPeriode()).harGjenstående()
            val underTerskel =
                HarArbeidetUnderTerskel(vilkårsvurdering.person, rapporteringsHendelse.somPeriode()).underTerskel()
            if (harGjenstående && underTerskel) {
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

    private class HarGjenstående(person: Person, private val periode: Periode) : PersonVisitor {
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
            utfall: Boolean,
        ) {
            if (virkningsdato <= periode.endInclusive) {
                harVedtak = true
            }
        }
    }

    private class HarArbeidetUnderTerskel(person: Person, val periode: Periode) : PersonVisitor {

        private val arbeidsdager = mutableListOf<Dag>()
        lateinit var virkningsdato: LocalDate
        var harDagpengevedtak = false

        init {
            person.accept(this)
        }

        fun underTerskel(): Boolean {
            val tellendeArbeidsdager = arbeidsdager.filter { it >= virkningsdato }

            val arbeidstimer = tellendeArbeidsdager.summer()
            val fastsattarbeidstidForPeriode =
                (fastsattArbeidstidPerDag * tellendeArbeidsdager.filterIsInstance<Arbeidsdag>().size)

            if (arbeidstimer.div(fastsattarbeidstidForPeriode) <= Prosent(50.0)) {
                return true
            }

            return false
        }

        lateinit var fastsattArbeidstidPerDag: Timer
        override fun visitFastsattArbeidstidPerDag(fastsattArbeidstidPerDag: Timer) {
            this.fastsattArbeidstidPerDag = fastsattArbeidstidPerDag
        }

        override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {
            if (arbeidsdag in periode) {
                arbeidsdager.add(arbeidsdag)
            }
        }

        override fun visitVedtakDagpengerettighet(dagpengerettighet: Dagpengerettighet) {
            harDagpengevedtak = true
        }

        override fun postVisitVedtak(
            vedtakId: UUID,
            virkningsdato: LocalDate,
            vedtakstidspunkt: LocalDateTime,
            utfall: Boolean,
        ) {
            if (harDagpengevedtak) {
                this.virkningsdato = virkningsdato
            }
            harDagpengevedtak = false
        }

        override fun visitHelgedag(helgedag: Helgedag) {
            if (helgedag in periode) {
                arbeidsdager.add(helgedag)
            }
        }
    }
}
