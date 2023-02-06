package no.nav.dagpenger.behandling.fastsettelse

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sats
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import java.math.BigDecimal
import java.time.LocalDate

class Paragraf_4_12_Størrelse_1_Ledd(
    private val inntektId: String,
    private val virkningsdato: LocalDate,
    private var sats: BigDecimal?,
) : Fastsettelse<Paragraf_4_12_Størrelse_1_Ledd>(IkkeVurdert) {

    constructor(inntektId: String, virkningsdato: LocalDate) :
        this(
            inntektId = inntektId,
            virkningsdato = virkningsdato,
            sats = null
        )

    object IkkeVurdert : Tilstand.IkkeVurdert<Paragraf_4_12_Størrelse_1_Ledd>()
    object AvventerVurdering : Tilstand.Avventer<Paragraf_4_12_Størrelse_1_Ledd>()
    object Vurdert : Tilstand.Vurdert<Paragraf_4_12_Størrelse_1_Ledd>()

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
