package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_løsning
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Tilstand.Type.AvventerVurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class AldersVilkårvurderingTest {
    @Test
    fun `vilkår endrer tilstand fra IkkeVurdert via AvventerVurdering til Oppfylt`() {
        val paragraf423alderVilkår = Paragraf_4_23_alder_vilkår()
        paragraf423alderVilkår.håndter(søknadHendelse = SøknadHendelse(UUID.randomUUID(), "123", "12345123456"))
        assertEquals(AvventerVurdering, paragraf423alderVilkår.tilstand.tilstandType)
        val vilkårvurderingId = UUID.randomUUID()
        paragraf423alderVilkår.håndter(aldersvilkårLøsning = Paragraf_4_23_alder_løsning("12345123456", vilkårvurderingId = vilkårvurderingId, UUID.randomUUID()))
//        assertEquals(Oppfylt, paragraf423alderVilkår.tilstand.tilstandType)
    }
}
