package no.nav.dagpenger.behandling.vilkår

class TestVilkår : Vilkårsvurdering<TestVilkår>(Oppfylt) {
    object Oppfylt : Tilstand.Oppfylt<TestVilkår>()

    override fun <T> implementasjon(block: TestVilkår.() -> T): T = this.block()
}
