package no.nav.dagpenger.behandling.fastsettelse

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sats
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.visitor.FastsettelseVisitor
import java.math.BigDecimal
import java.time.LocalDate

internal class Paragraf_4_12_Sats(
    private val inntektId: String,
    private val virkningsdato: LocalDate,
) : Fastsettelse<Paragraf_4_12_Sats>(IkkeVurdert) {
    private lateinit var sats: BigDecimal
    object IkkeVurdert : Tilstand.IkkeVurdert<Paragraf_4_12_Sats>()
    object AvventerVurdering : Tilstand.Avventer<Paragraf_4_12_Sats>()
    object Vurdert : Tilstand.Vurdert<Paragraf_4_12_Sats>() {
        override fun accept(paragraf: Paragraf_4_12_Sats, visitor: FastsettelseVisitor) {
            visitor.visitDagsats(paragraf.sats)
        }
    }

    override fun accept(visitor: FastsettelseVisitor) {
        tilstand.accept(this, visitor)
    }

    override fun håndter(hendelse: Hendelse) {
        hendelse.behov(
            Sats,
            "Trenger sats",
            mapOf(
                "virkningsdato" to virkningsdato,
                "inntektId" to inntektId
            )
        )
        endreTilstand(AvventerVurdering)
    }

    override fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat) {
        this.sats = grunnlagOgSatsResultat.dagsats
        endreTilstand(Vurdert)
    }
}
