package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering
import java.util.UUID

abstract class Behandling(internal val behandlingId: UUID, internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()) : Aktivitetskontekst {

    abstract val vilkårsvurderinger: List<Vilkårsvurdering<*>>

    abstract fun håndter(hendelse: SøknadHendelse)
    abstract fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_resultat)

    companion object {
        const val kontekstType = "Behandling"
    }
}
