package no.nav.dagpenger.behandling.fastsettelse

import no.nav.dagpenger.behandling.Aktivitetslogg
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.StønadsperiodeResultat
import no.nav.dagpenger.behandling.mengde.Stønadsperiode
import no.nav.dagpenger.behandling.visitor.FastsettelseVisitor
import java.time.LocalDate

internal class Paragraf_4_15_Stønadsperiode(
    private val inntektsId: String,
    private val virkningsdato: LocalDate,
) : Fastsettelse<Paragraf_4_15_Stønadsperiode>(IkkeVurdert) {

    private lateinit var stønadsperiode: Stønadsperiode

    object IkkeVurdert : Tilstand.IkkeVurdert<Paragraf_4_15_Stønadsperiode>() {
        override fun håndter(hendelse: Hendelse, fastsettelse: Paragraf_4_15_Stønadsperiode) {
            hendelse.behov(
                Aktivitetslogg.Aktivitet.Behov.Behovtype.StønadsperiodeBehov,
                "Trenger dagpengeperiode",
                mapOf(
                    "virkningsdato" to fastsettelse.virkningsdato,
                    "inntektsId" to fastsettelse.inntektsId,
                ),
            )
            fastsettelse.endreTilstand(AvventerVurdering)
        }
    }
    object AvventerVurdering : Tilstand.Avventer<Paragraf_4_15_Stønadsperiode>() {
        override fun håndter(
            stønadsperiodeResultat: StønadsperiodeResultat,
            fastsettelse: Paragraf_4_15_Stønadsperiode,
        ) {
            fastsettelse.stønadsperiode = stønadsperiodeResultat.stønadsperiode
            fastsettelse.endreTilstand(Vurdert)
        }
    }
    object Vurdert : Tilstand.Vurdert<Paragraf_4_15_Stønadsperiode>() {
        override fun accept(paragraf: Paragraf_4_15_Stønadsperiode, visitor: FastsettelseVisitor) {
            visitor.visitStønadsperiode(paragraf.stønadsperiode)
        }
    }

    override fun accept(visitor: FastsettelseVisitor) {
        tilstand.accept(this, visitor)
    }

    override fun <T> implementasjon(block: Paragraf_4_15_Stønadsperiode.() -> T) = this.block()
}
