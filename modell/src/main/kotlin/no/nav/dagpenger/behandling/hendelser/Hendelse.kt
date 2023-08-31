package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.Aktivitetslogg
import no.nav.dagpenger.aktivitetslogg.IAktivitetslogg
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import java.util.UUID

abstract class Hendelse(
    private val meldingsreferanseId: UUID,
    private val ident: String,
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : Aktivitetskontekst, IAktivitetslogg by aktivitetslogg {
    fun meldingsreferanseId() = meldingsreferanseId
    fun ident() = ident
    final override fun toSpesifikkKontekst() =
        SpesifikkKontekst(this.javaClass.simpleName, mapOf("ident" to ident) + kontekst())

    protected open fun kontekst(): Map<String, String> = emptyMap()
}
