package no.nav.dagpenger.behandling.fastsettelse

import no.nav.dagpenger.behandling.Aktivitetslogg
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.visitor.FastsettelseVisitor
import java.math.BigDecimal
import java.time.LocalDate

internal class Paragraf_4_11_Grunnlag(
    private val inntektId: String,
    private val virkningsdato: LocalDate
) : Fastsettelse<Paragraf_4_11_Grunnlag>(IkkeVurdert) {

    private lateinit var grunnlag: BigDecimal

    object IkkeVurdert : Tilstand.IkkeVurdert<Paragraf_4_11_Grunnlag>()

    object AvventerVurdering : Tilstand.Avventer<Paragraf_4_11_Grunnlag>()

    object Vurdert : Tilstand.Vurdert<Paragraf_4_11_Grunnlag>() {
        override fun accept(paragraf: Paragraf_4_11_Grunnlag, visitor: FastsettelseVisitor) {
            visitor.visitGrunnlag(paragraf.grunnlag)
        }
    }

    override fun accept(visitor: FastsettelseVisitor) {
        tilstand.accept(this, visitor)
    }

    override fun håndter(hendelse: Hendelse) {
        hendelse.behov(
            Aktivitetslogg.Aktivitet.Behov.Behovtype.Grunnlag,
            "Trenger grunnlag",
            mapOf(
                "virkningsdato" to virkningsdato,
                "inntektId" to inntektId
            )
        )
        endreTilstand(AvventerVurdering)
    }

    override fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat) {
        this.grunnlag = grunnlagOgSatsResultat.grunnlag
        endreTilstand(Vurdert)
    }
}
