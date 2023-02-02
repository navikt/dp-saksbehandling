package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Tilstand.Type.AvventerVurdering
import no.nav.dagpenger.behandling.visitor.VilkårsvurderingVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class AldersVilkårvurderingTest {

    @Test
    fun `vilkår endrer tilstand fra IkkeVurdert via AvventerVurdering til Oppfylt`() {

        val paragraf423alderVilkår = Paragraf_4_23_alder_vilkår()
        paragraf423alderVilkår.håndter(søknadHendelse = SøknadHendelse(UUID.randomUUID(), "123", "12345123456"))
        val inspektør = Inspektør(paragraf423alderVilkår)
        assertEquals(AvventerVurdering, inspektør.tilstand)

        paragraf423alderVilkår.håndter(
            Paragraf_4_23_alder_Vilkår_resultat(
                "12345123456",
                vilkårsvurderingId = inspektør.vilkårsvurderingId,
                true
            )
        )
        assertEquals(Vilkårsvurdering.Tilstand.Type.Oppfylt, Inspektør(paragraf423alderVilkår).tilstand)
    }

    private class Inspektør(vilkårsvurdering: Vilkårsvurdering<Paragraf_4_23_alder_vilkår>) : VilkårsvurderingVisitor {

        lateinit var tilstand: Vilkårsvurdering.Tilstand.Type
        lateinit var vilkårsvurderingId: UUID

        init {
            vilkårsvurdering.accept(this)
        }

        override fun <Paragraf : Vilkårsvurdering<Paragraf>> visitVilkårsvurdering(
            vilkårsvurderingId: UUID,
            tilstand: Vilkårsvurdering.Tilstand<Paragraf>
        ) {
            this.tilstand = tilstand.tilstandType
            this.vilkårsvurderingId = vilkårsvurderingId
        }
    }
}
