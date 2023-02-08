package no.nav.dagpenger.behandling.vilkår

import mu.KotlinLogging
import no.nav.dagpenger.behandling.Aktivitetskontekst
import no.nav.dagpenger.behandling.SpesifikkKontekst
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Tilstand.Type.Oppfylt
import no.nav.dagpenger.behandling.visitor.VilkårsvurderingVisitor
import java.util.UUID

private val logger = KotlinLogging.logger { }

abstract class Vilkårsvurdering<Paragraf : Vilkårsvurdering<Paragraf>> private constructor(
    protected val vilkårsvurderingId: UUID,
    private var tilstand: Tilstand<Paragraf>
) : Aktivitetskontekst {
    constructor(tilstand: Tilstand<Paragraf>) : this(UUID.randomUUID(), tilstand)
    companion object {
        fun List<Vilkårsvurdering<*>>.vurdert() =
            this.none { it.tilstand.tilstandType == Tilstand.Type.AvventerVurdering || it.tilstand.tilstandType == Tilstand.Type.IkkeVurdert }

        fun List<Vilkårsvurdering<*>>.erAlleOppfylt() =
            this.all { it.tilstand.tilstandType == Oppfylt }
    }

    fun accept(visitor: VilkårsvurderingVisitor) {
        visitor.visitVilkårsvurdering(vilkårsvurderingId, tilstand)
    }

    fun håndter(søknadHendelse: SøknadHendelse) {
        søknadHendelse.kontekst(this)
        implementasjon { tilstand.håndter(søknadHendelse, this) }
    }
    fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat) {
        if (this.vilkårsvurderingId != paragraf423AlderResultat.vilkårsvurderingId) return
        paragraf423AlderResultat.kontekst(this)
        implementasjon { tilstand.håndter(paragraf423AlderResultat, this) }
    }

    fun endreTilstand(nyTilstand: Tilstand<Paragraf>) {
        loggTilstandsendring(nyTilstand)
        tilstand = nyTilstand
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst =
        SpesifikkKontekst(this.javaClass.simpleName, mapOf("vilkårsvurderingId" to vilkårsvurderingId.toString()))

    protected abstract fun <T> implementasjon(block: Paragraf.() -> T): T

    sealed class Tilstand<Paragraf : Vilkårsvurdering<Paragraf>>(val tilstandType: Type) {

        enum class Type {
            Oppfylt,
            IkkeOppfylt,
            IkkeVurdert,
            AvventerVurdering;
        }
        open fun håndter(søknadHendelse: SøknadHendelse, vilkårsvurdering: Paragraf) {
            feilmelding(søknadHendelse)
        }
        open fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat, vilkårsvurdering: Paragraf) {
            feilmelding(paragraf423AlderResultat)
        }
        private fun feilmelding(hendelse: Hendelse) =
            hendelse.warn("Kan ikke håndtere ${hendelse.javaClass.simpleName} i tilstand ${this.tilstandType}")
        abstract class IkkeVurdert<Paragraf : Vilkårsvurdering<Paragraf>> : Tilstand<Paragraf>(Type.IkkeVurdert)
        abstract class Avventer<Paragraf : Vilkårsvurdering<Paragraf>> : Tilstand<Paragraf>(Type.AvventerVurdering)
        abstract class Oppfylt<Paragraf : Vilkårsvurdering<Paragraf>> : Tilstand<Paragraf>(Oppfylt)
        abstract class IkkeOppfylt<Paragraf : Vilkårsvurdering<Paragraf>> : Tilstand<Paragraf>(Type.IkkeOppfylt)
    }

    private fun loggTilstandsendring(nyTilstand: Tilstand<Paragraf>) {
        logger.info { "Vurdering ${this.javaClass.simpleName} endrer tilstand fra ${tilstand.tilstandType} til ny tilstand ${nyTilstand.tilstandType}" }
    }
}
