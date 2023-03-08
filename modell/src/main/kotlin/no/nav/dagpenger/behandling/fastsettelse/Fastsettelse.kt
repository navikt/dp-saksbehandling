package no.nav.dagpenger.behandling.fastsettelse

import no.nav.dagpenger.behandling.Aktivitetskontekst
import no.nav.dagpenger.behandling.SpesifikkKontekst
import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse.Tilstand.Type.Vurdert
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Rapporteringshendelse
import no.nav.dagpenger.behandling.hendelser.StønadsperiodeResultat
import no.nav.dagpenger.behandling.rapportering.Dag
import no.nav.dagpenger.behandling.visitor.FastsettelseVisitor
import java.util.UUID

internal abstract class Fastsettelse<Paragraf : Fastsettelse<Paragraf>>(
    protected val fastsettelseId: UUID,
    protected var tilstand: Tilstand<Paragraf>,
) : Aktivitetskontekst {

    constructor(tilstand: Tilstand<Paragraf>) : this(UUID.randomUUID(), tilstand)

    companion object {
        fun List<Fastsettelse<*>>.vurdert() =
            this.all { it.tilstand.tilstandType == Vurdert }
    }

    abstract fun accept(visitor: FastsettelseVisitor)
    fun håndter(hendelse: Hendelse) {
        hendelse.kontekst(this)
        implementasjon { tilstand.håndter(hendelse, this) }
    }
    fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat) {
        grunnlagOgSatsResultat.kontekst(this)
        implementasjon { tilstand.håndter(grunnlagOgSatsResultat, this) }
    }
    fun håndter(stønadsperiodeResultat: StønadsperiodeResultat) {
        stønadsperiodeResultat.kontekst(this)
        implementasjon { tilstand.håndter(stønadsperiodeResultat, this) }
    }
    fun håndter(rapporteringsHendelse: Rapporteringshendelse, tellendeDager: List<Dag>) {
        rapporteringsHendelse.kontekst(this)
        implementasjon { tilstand.håndter(rapporteringsHendelse, tellendeDager, this) }
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst =
        SpesifikkKontekst(this.javaClass.simpleName)

    protected abstract fun <T> implementasjon(block: Paragraf.() -> T): T

    sealed class Tilstand<Paragraf : Fastsettelse<Paragraf>>(val tilstandType: Type) {
        open fun accept(paragraf: Paragraf, visitor: FastsettelseVisitor) {}

        open fun håndter(hendelse: Hendelse, fastsettelse: Paragraf) {
            hendelse.tilstandfeil()
        }

        open fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat, fastsettelse: Paragraf) {
            grunnlagOgSatsResultat.tilstandfeil()
        }

        open fun håndter(stønadsperiodeResultat: StønadsperiodeResultat, fastsettelse: Paragraf) {
            stønadsperiodeResultat.tilstandfeil()
        }

        open fun håndter(rapporteringsHendelse: Rapporteringshendelse, tellendeDager: List<Dag>, fastsettelse: Paragraf) {
            rapporteringsHendelse.tilstandfeil()
        }

        enum class Type {
            IkkeVurdert,
            AvventerVurdering,
            Vurdert,
        }

        abstract class IkkeVurdert<Paragraf : Fastsettelse<Paragraf>> : Tilstand<Paragraf>(Type.IkkeVurdert)
        abstract class Avventer<Paragraf : Fastsettelse<Paragraf>> : Tilstand<Paragraf>(Type.AvventerVurdering)
        abstract class Vurdert<Paragraf : Fastsettelse<Paragraf>> : Tilstand<Paragraf>(Vurdert)

        private fun Hendelse.tilstandfeil() {
            this.warn("Forventet ikke ${this.javaClass.simpleName} i tilstand ${tilstandType.name} ")
        }
    }

    internal fun endreTilstand(nyTilstand: Tilstand<Paragraf>) {
        tilstand = nyTilstand
    }
}
