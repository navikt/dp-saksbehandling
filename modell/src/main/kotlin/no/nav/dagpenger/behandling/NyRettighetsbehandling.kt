package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.AldersVilkårvurdering
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering

class NyRettighetsbehandling : Behandling() {
    override val vilkårsvurderinger: List<Vilkårsvurdering>
        get() = listOf(
            AldersVilkårvurdering()
        )

    override fun håndter(søknadHendelse: SøknadHendelse) {
        vilkårsvurderinger.forEach { vurdering ->
            vurdering.håndter(søknadHendelse)
        }
    }
}
