package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.AldersbehovLøsning
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.AldersVilkårvurdering

class NyRettighetsbehandling : Behandling() {
    override val vilkårsvurderinger = listOf(
        AldersVilkårvurdering()
    )

    override fun håndter(søknadHendelse: SøknadHendelse) {
        vilkårsvurderinger.forEach { vurdering ->
            vurdering.håndter(søknadHendelse)
        }
    }

    override fun håndter(aldersbehovLøsning: AldersbehovLøsning) {
        vilkårsvurderinger.forEach { vurdering ->
            vurdering.håndter(aldersbehovLøsning)
        }
    }
}
