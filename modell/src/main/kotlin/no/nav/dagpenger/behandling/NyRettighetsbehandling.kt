package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakAvslåttBehov
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakInnvilgetBehov
import no.nav.dagpenger.behandling.hendelser.AldersbehovLøsning
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.AldersVilkårvurdering
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.erFerdig
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Tilstand.Type.Oppfylt
import java.util.UUID

class NyRettighetsbehandling : Behandling(UUID.randomUUID()) {
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
                hendelse.behov(VedtakInnvilgetBehov, "Vedtak innvilget", mapOf("sats" to 123))
            } else {
                hendelse.behov(VedtakAvslåttBehov, "Vedtak avslått")
            }
        }
    }
}
