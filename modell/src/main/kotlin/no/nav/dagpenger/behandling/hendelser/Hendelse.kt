package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.Aktivitetskontekst
import no.nav.dagpenger.behandling.Aktivitetslogg
import no.nav.dagpenger.behandling.IAktivitetslogg
import no.nav.dagpenger.behandling.SpesifikkKontekst

abstract class Hendelse(
    private val ident: String,
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : Aktivitetskontekst, IAktivitetslogg by aktivitetslogg {
    fun ident() = ident
    override fun toSpesifikkKontekst() =
        SpesifikkKontekst(this.javaClass.simpleName, mapOf("ident" to ident))
}
