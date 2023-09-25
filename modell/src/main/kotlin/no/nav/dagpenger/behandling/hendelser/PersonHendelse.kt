package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.Aktivitetslogg
import no.nav.dagpenger.aktivitetslogg.IAktivitetslogg
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import java.util.UUID

abstract class PersonHendelse(
    meldingsreferanseId: UUID,
    private val ident: String,
) : Hendelse(meldingsreferanseId) {
    fun ident() = ident
    override fun kontekst(): Map<String, String> = mapOf("ident" to ident)

    override fun equals(other: Any?): Boolean {
        return other is PersonHendelse && other.ident == ident && super.equals(other)
    }

    override fun hashCode(): Int = super.hashCode() + ident.hashCode()
}

abstract class Hendelse(
    private val meldingsreferanseId: UUID,
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : Aktivitetskontekst, IAktivitetslogg by aktivitetslogg {
    fun meldingsreferanseId() = meldingsreferanseId

    final override fun toSpesifikkKontekst() =
        SpesifikkKontekst(this.javaClass.simpleName, kontekst())

    protected open fun kontekst(): Map<String, String> = emptyMap()

    override fun equals(other: Any?): Boolean {
        return other is Hendelse && other.meldingsreferanseId == meldingsreferanseId
    }

    override fun hashCode(): Int {
        return meldingsreferanseId.hashCode()
    }
}
