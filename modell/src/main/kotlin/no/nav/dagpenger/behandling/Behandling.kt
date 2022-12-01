package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.AldersvilkårLøsning
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering
import java.util.UUID

abstract class Behandling(private val behandlingId: UUID) : Aktivitetskontekst {

    abstract val vilkårsvurderinger: List<Vilkårsvurdering>

    abstract fun håndter(hendelse: SøknadHendelse)
    abstract fun håndter(aldersvilkårLøsning: AldersvilkårLøsning)

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst(this.javaClass.simpleName, mapOf("behandlingId" to behandlingId.toString()))
}
