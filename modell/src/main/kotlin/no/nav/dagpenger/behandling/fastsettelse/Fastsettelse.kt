package no.nav.dagpenger.behandling.fastsettelse

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
) {

    constructor(tilstand: Tilstand<Paragraf>) : this(UUID.randomUUID(), tilstand)

    companion object {
        fun List<Fastsettelse<*>>.vurdert() =
            this.all { it.tilstand.tilstandType == Vurdert }
    }

    abstract fun accept(visitor: FastsettelseVisitor)
    open fun håndter(hendelse: Hendelse) {}
    open fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat) {}
    open fun håndter(stønadsperiodeResultat: StønadsperiodeResultat) {}
    open fun håndter(rapporteringsHendelse: Rapporteringshendelse, tellendeDager: List<Dag>) {}

    sealed class Tilstand<Paragraf : Fastsettelse<Paragraf>>(val tilstandType: Type) {
        open fun accept(paragraf: Paragraf, visitor: FastsettelseVisitor) {}

        enum class Type {
            IkkeVurdert,
            AvventerVurdering,
            Vurdert,
        }

        abstract class IkkeVurdert<Paragraf : Fastsettelse<Paragraf>> : Tilstand<Paragraf>(Type.IkkeVurdert)
        abstract class Avventer<Paragraf : Fastsettelse<Paragraf>> : Tilstand<Paragraf>(Type.AvventerVurdering)
        abstract class Vurdert<Paragraf : Fastsettelse<Paragraf>> : Tilstand<Paragraf>(Vurdert)
    }

    fun endreTilstand(nyTilstand: Tilstand<Paragraf>) {
        tilstand = nyTilstand
    }
}
