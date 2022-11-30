package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.AldersbehovLøsning
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering

abstract class Behandling {
    abstract val vilkårsvurderinger: List<Vilkårsvurdering>

    abstract fun håndter(søknadHendelse: SøknadHendelse)
    abstract fun håndter(aldersbehovLøsning: AldersbehovLøsning)
}
