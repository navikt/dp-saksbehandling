package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.AldersbehovLøsning
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.AldersVilkårvurdering
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.erFerdig
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Tilstand.Type.Oppfylt

class NyRettighetsbehandling : Behandling() {
    override val vilkårsvurderinger = listOf(
        AldersVilkårvurdering(),
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
        ferdigstillRettighetsbehandling(aldersbehovLøsning)
    }

    private fun ferdigstillRettighetsbehandling(hendelse: Hendelse) {
        if (vilkårsvurderinger.erFerdig()) {
            if (vilkårsvurderinger.all { it.tilstand.tilstandType == Oppfylt }) {
                hendelse.behov(VedtakInnvilgetBehov(32423.toBigDecimal()))
            } else {
                hendelse.behov(VedtakAvslåttBehov("matte"))
            }
        }
    }
}
