package no.nav.dagpenger.behandling.fastsettelse

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.SatsBehov
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.visitor.FastsettelseVisitor
import java.math.BigDecimal
import java.time.LocalDate

internal class Paragraf_4_12_Sats(
    private val inntektsId: String,
    private val virkningsdato: LocalDate,
) : Fastsettelse<Paragraf_4_12_Sats>(IkkeVurdert) {
    private lateinit var sats: BigDecimal
    object IkkeVurdert : Tilstand.IkkeVurdert<Paragraf_4_12_Sats>() {
        override fun håndter(hendelse: Hendelse, fastsettelse: Paragraf_4_12_Sats) {
            hendelse.behov(
                SatsBehov,
                "Trenger sats",
                mapOf(
                    "virkningsdato" to fastsettelse.virkningsdato,
                    "inntektsId" to fastsettelse.inntektsId,
                ),
            )
            fastsettelse.endreTilstand(AvventerVurdering)
        }
    }
    object AvventerVurdering : Tilstand.Avventer<Paragraf_4_12_Sats>() {
        override fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat, fastsettelse: Paragraf_4_12_Sats) {
            fastsettelse.sats = grunnlagOgSatsResultat.dagsats
            fastsettelse.endreTilstand(Vurdert)
        }
    }
    object Vurdert : Tilstand.Vurdert<Paragraf_4_12_Sats>() {
        override fun accept(paragraf: Paragraf_4_12_Sats, visitor: FastsettelseVisitor) {
            visitor.visitDagsats(paragraf.sats)
        }
    }

    override fun accept(visitor: FastsettelseVisitor) {
        tilstand.accept(this, visitor)
    }

    override fun <T> implementasjon(block: Paragraf_4_12_Sats.() -> T) = this.block()
}
