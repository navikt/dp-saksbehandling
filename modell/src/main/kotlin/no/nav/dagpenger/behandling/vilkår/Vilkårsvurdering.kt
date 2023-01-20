package no.nav.dagpenger.behandling.vilkår

import mu.KotlinLogging
import no.nav.dagpenger.behandling.Aktivitetskontekst
import no.nav.dagpenger.behandling.SpesifikkKontekst
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import java.util.UUID

private val logger = KotlinLogging.logger { }

abstract class Vilkårsvurdering private constructor(var tilstand: Tilstand, val vilkårsvurderingId: UUID) : Aktivitetskontekst {
    constructor(tilstand: Tilstand) : this(tilstand, UUID.randomUUID())
    companion object {
        fun List<Vilkårsvurdering>.erFerdig() =
            this.none { it.tilstand.tilstandType == Tilstand.Type.AvventerVurdering || it.tilstand.tilstandType == Tilstand.Type.IkkeVurdert }
    }

    fun håndter(søknadHendelse: SøknadHendelse) {
        søknadHendelse.kontekst(this)
        tilstand.håndter(søknadHendelse, this)
    }

    fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_resultat) {
        if (this.vilkårsvurderingId != paragraf423AlderResultat.vilkårsvurderingId) return
        paragraf423AlderResultat.kontekst(this)
        tilstand.håndter(paragraf423AlderResultat, this)
    }

    fun endreTilstand(nyTilstand: Tilstand) {
        loggTilstandsendring(nyTilstand)
        tilstand = nyTilstand
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst =
        SpesifikkKontekst(this.javaClass.simpleName, mapOf("vilkårsvurderingId" to vilkårsvurderingId.toString()))

    interface Tilstand {
        fun håndter(søknadHendelse: SøknadHendelse, vilkårsvurdering: Vilkårsvurdering) {
            feilmelding(søknadHendelse)
        }

        fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_resultat, vilkårsvurdering: Vilkårsvurdering) {
            feilmelding(paragraf423AlderResultat)
        }

        private fun feilmelding(hendelse: Hendelse) =
            hendelse.warn("Kan ikke håndtere ${hendelse.javaClass.simpleName} i tilstand ${this.tilstandType}")

        val tilstandType: Type

        enum class Type {
            Oppfylt,
            IkkeOppfylt,
            IkkeVurdert,
            AvventerVurdering
        }
    }

    private fun loggTilstandsendring(nyTilstand: Tilstand) {
        logger.info { "Vurdering ${this.javaClass.simpleName} endrer tilstand fra ${tilstand.tilstandType} til ny tilstand ${nyTilstand.tilstandType}" }
    }
}
