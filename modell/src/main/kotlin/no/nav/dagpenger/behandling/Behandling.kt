package no.nav.dagpenger.behandling

abstract class Behandling {
    abstract val vilkårsvurderinger: List<Vilkårsvurdering>

    abstract fun håndter(søknadHendelse: SøknadHendelse)
}

class NyRettighetsbehandling : Behandling() {
    override val vilkårsvurderinger: List<Vilkårsvurdering>
        get() = listOf(
            AldersVilkårsvurdering()
        )

    override fun håndter(søknadHendelse: SøknadHendelse) {
        vilkårsvurderinger.forEach { vurdering ->
            vurdering.håndter(søknadHendelse)
        }
    }
}

