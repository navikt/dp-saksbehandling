package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_resultat
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

        paragraf423alderVilkår.håndter(
            Paragraf_4_23_alder_resultat(
                "12345123456",
                vilkårsvurderingId = paragraf423alderVilkår.vilkårsvurderingId,
                true
            )
        )
        assertEquals(Vilkårsvurdering.Tilstand.Type.Oppfylt, paragraf423alderVilkår.tilstand.tilstandType)
    }
}
