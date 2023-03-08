package no.nav.dagpenger.behandling.fastsettelse

import no.nav.dagpenger.behandling.Aktivitetslogg
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.visitor.FastsettelseVisitor
import java.math.BigDecimal
import java.time.LocalDate

internal class Paragraf_4_11_Grunnlag(
    private val inntektsId: String,
    private val virkningsdato: LocalDate,
) : Fastsettelse<Paragraf_4_11_Grunnlag>(IkkeVurdert) {

    private lateinit var grunnlag: BigDecimal

    object IkkeVurdert : Tilstand.IkkeVurdert<Paragraf_4_11_Grunnlag>() {
        override fun håndter(hendelse: Hendelse, fastsettelse: Paragraf_4_11_Grunnlag) {
            hendelse.behov(
                Aktivitetslogg.Aktivitet.Behov.Behovtype.GrunnlagsBehov,
                "Trenger grunnlag",
                mapOf(
                    "virkningsdato" to fastsettelse.virkningsdato,
                    "inntektsId" to fastsettelse.inntektsId,
                ),
            )
            fastsettelse.endreTilstand(AvventerVurdering)
        }
    }

    object AvventerVurdering : Tilstand.Avventer<Paragraf_4_11_Grunnlag>() {
        override fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat, fastsettelse: Paragraf_4_11_Grunnlag) {
            fastsettelse.grunnlag = grunnlagOgSatsResultat.grunnlag
            fastsettelse.endreTilstand(Vurdert)
        }
    }

    object Vurdert : Tilstand.Vurdert<Paragraf_4_11_Grunnlag>() {
        override fun accept(paragraf: Paragraf_4_11_Grunnlag, visitor: FastsettelseVisitor) {
            visitor.visitGrunnlag(paragraf.grunnlag)
        }
    }

    override fun <T> implementasjon(block: Paragraf_4_11_Grunnlag.() -> T) = this.block()

    override fun accept(visitor: FastsettelseVisitor) {
        tilstand.accept(this, visitor)
    }
}
