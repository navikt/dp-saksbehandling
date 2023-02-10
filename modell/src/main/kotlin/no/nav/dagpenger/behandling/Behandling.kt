package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.BeslutterHendelse
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.StønadsperiodeResultat
import no.nav.dagpenger.behandling.visitor.BehandlingVisitor
import java.util.UUID

abstract class Behandling(
    private val person: Person,
    private val behandlingsId: UUID,
    protected val hendelseId: UUID,
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : Aktivitetskontekst {

    open fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat) {
        kanIkkeHåndtere(paragraf423AlderResultat)
    }

    open fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat) {
        kanIkkeHåndtere(grunnlagOgSatsResultat)
    }

    open fun håndter(stønadsperiode: StønadsperiodeResultat) {
        kanIkkeHåndtere(stønadsperiode)
    }

    open fun håndter(beslutterHendelse: BeslutterHendelse) {
        kanIkkeHåndtere(beslutterHendelse)
    }

    fun accept(visitor: BehandlingVisitor) {

    }

    private fun kanIkkeHåndtere(hendelse: Hendelse) {
        hendelse.severe("${this.javaClass.simpleName} vet ikke hvordan vi skal behandle ${hendelse.javaClass.simpleName}")
    }

    companion object {
        fun List<Behandling>.harHendelseId(hendelseId: UUID) =
            this.any { it.hendelseId == hendelseId }

        const val kontekstType = "Behandling"
    }
}
