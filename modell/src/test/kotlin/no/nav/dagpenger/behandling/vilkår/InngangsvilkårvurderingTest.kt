package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.hendelser.InngangsvilkårResultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Tilstand.Type.AvventerVurdering
import no.nav.dagpenger.behandling.visitor.VilkårsvurderingVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class InngangsvilkårvurderingTest {

    @Test
    fun `vilkår endrer tilstand fra IkkeVurdert via AvventerVurdering til Oppfylt`() {

        val inngangsvilkår = Inngangsvilkår()
        inngangsvilkår.håndter(søknadHendelse = SøknadHendelse(UUID.randomUUID(), "123", "12345123456"))
        val inspektør = Inspektør(inngangsvilkår)
        assertEquals(AvventerVurdering, inspektør.tilstand)

        inngangsvilkår.håndter(
            InngangsvilkårResultat(
                "12345123456",
                vilkårsvurderingId = inspektør.vilkårsvurderingId,
                true,
                LocalDate.now()
            )
        )
        assertEquals(Vilkårsvurdering.Tilstand.Type.Oppfylt, Inspektør(inngangsvilkår).tilstand)
    }

    private class Inspektør(vilkårsvurdering: Vilkårsvurdering<Inngangsvilkår>) : VilkårsvurderingVisitor {

        init {
            vilkårsvurdering.accept(this)
        }

        lateinit var tilstand: Vilkårsvurdering.Tilstand.Type
        lateinit var vilkårsvurderingId: UUID

        override fun <Paragraf : Vilkårsvurdering<Paragraf>> visitVilkårsvurdering(
            vilkårsvurderingId: UUID,
            tilstand: Vilkårsvurdering.Tilstand<Paragraf>
        ) {
            this.tilstand = tilstand.tilstandType
            this.vilkårsvurderingId = vilkårsvurderingId
        }
    }
}
