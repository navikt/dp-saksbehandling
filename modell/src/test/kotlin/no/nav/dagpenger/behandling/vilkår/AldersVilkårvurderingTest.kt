package no.nav.dagpenger.behandling.vilkår

import no.nav.dagpenger.behandling.hendelser.AldersbehovLøsning
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Tilstand.Type.Oppfylt
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Tilstand.Type.AvventerVurdering
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

internal class AldersVilkårvurderingTest {
    @Test
    fun `vilkår endrer tilstand fra IkkeVurdert via AvventerVurdering til Oppfylt`() {
        val aldersVilkårvurdering = AldersVilkårvurdering()
        aldersVilkårvurdering.håndter(søknadHendelse = SøknadHendelse(UUID.randomUUID(), "12345123456"))
        assertEquals(AvventerVurdering, aldersVilkårvurdering.tilstand.tilstandType)
        aldersVilkårvurdering.håndter(aldersbehovLøsning = AldersbehovLøsning("12345123456", true))
        assertEquals(Oppfylt, aldersVilkårvurdering.tilstand.tilstandType)
    }
}