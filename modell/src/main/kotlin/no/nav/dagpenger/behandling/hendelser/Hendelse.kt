package no.nav.dagpenger.behandling

abstract class Hendelse(
    private val ident: String,
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : Aktivitetskontekst, IAktivitetslogg by aktivitetslogg {

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst(this.javaClass.simpleName, mapOf("ident" to ident))
    }
}
