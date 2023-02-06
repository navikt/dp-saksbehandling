package no.nav.dagpenger.behandling.fastsettelse

import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import java.util.UUID

abstract class Fastsettelse<Paragraf : Fastsettelse<Paragraf>>(
    protected val fastsettelseId: UUID,
    private var tilstand: Tilstand<Paragraf>
) {
    abstract fun håndter(hendelse: Hendelse)
    abstract fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat)

    constructor(tilstand: Tilstand<Paragraf>) : this(UUID.randomUUID(), tilstand)
    sealed class Tilstand<Paragraf : Fastsettelse<Paragraf>>(val tilstandType: Type) {
        enum class Type {
            IkkeVurdert,
            AvventerVurdering,
            Vurdert
        }

        abstract class IkkeVurdert<Paragraf : Fastsettelse<Paragraf>> : Tilstand<Paragraf>(Type.IkkeVurdert)
        abstract class Avventer<Paragraf : Fastsettelse<Paragraf>> : Tilstand<Paragraf>(Type.AvventerVurdering)
        abstract class Vurdert<Paragraf : Fastsettelse<Paragraf>> : Tilstand<Paragraf>(Type.Vurdert)
    }

    fun endreTilstand(nyTilstand: Tilstand<Paragraf>) {
        tilstand = nyTilstand
    }
}
