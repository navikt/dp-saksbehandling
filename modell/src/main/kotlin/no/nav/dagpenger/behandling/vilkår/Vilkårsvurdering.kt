package no.nav.dagpenger.behandling.vilkår

import mu.KotlinLogging
import no.nav.dagpenger.behandling.Aktivitetskontekst
import no.nav.dagpenger.behandling.SpesifikkKontekst
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.InngangsvilkårResultat
import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Tilstand.Type.Oppfylt
import no.nav.dagpenger.behandling.visitor.VilkårsvurderingVisitor
import java.util.UUID

private val logger = KotlinLogging.logger { }

abstract class Vilkårsvurdering<Vilkår : Vilkårsvurdering<Vilkår>> private constructor(
    protected val vilkårsvurderingId: UUID,
    protected var tilstand: Tilstand<Vilkår>,
) : Aktivitetskontekst {
    constructor(tilstand: Tilstand<Vilkår>) : this(UUID.randomUUID(), tilstand)

    companion object {
        fun Vilkårsvurdering<*>.vurdert() =
            this.tilstand.tilstandType != Tilstand.Type.AvventerVurdering || this.tilstand.tilstandType != Tilstand.Type.IkkeVurdert

        fun Vilkårsvurdering<*>.oppfylt() = this.tilstand.tilstandType == Tilstand.Type.Oppfylt
    }

    open fun accept(visitor: VilkårsvurderingVisitor) {
        visitor.visitVilkårsvurdering(vilkårsvurderingId, tilstand)
    }

    fun håndter(søknadHendelse: SøknadHendelse) {
        søknadHendelse.kontekst(this)
        implementasjon { tilstand.håndter(søknadHendelse, this) }
    }

    fun håndter(inngangsvilkårResultat: InngangsvilkårResultat) {
        if (this.vilkårsvurderingId != inngangsvilkårResultat.vilkårsvurderingId) return
        inngangsvilkårResultat.kontekst(this)
        implementasjon { tilstand.håndter(inngangsvilkårResultat, this) }
    }

    fun håndter(rapporteringsHendelse: RapporteringsHendelse) {
        rapporteringsHendelse.kontekst(this)
        implementasjon { tilstand.håndter(rapporteringsHendelse, this) }
    }

    fun endreTilstand(nyTilstand: Tilstand<Vilkår>) {
        loggTilstandsendring(nyTilstand)
        tilstand = nyTilstand
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst =
        SpesifikkKontekst(this.javaClass.simpleName, mapOf("vilkårsvurderingId" to vilkårsvurderingId.toString()))

    protected abstract fun <T> implementasjon(block: Vilkår.() -> T): T

    sealed class Tilstand<Vilkår : Vilkårsvurdering<Vilkår>>(val tilstandType: Type) {

        enum class Type {
            Oppfylt,
            IkkeOppfylt,
            IkkeVurdert,
            AvventerVurdering,
        }

        open fun håndter(søknadHendelse: SøknadHendelse, vilkårsvurdering: Vilkår) {
            feilmelding(søknadHendelse)
        }

        open fun håndter(inngangsvilkårResultat: InngangsvilkårResultat, vilkårsvurdering: Vilkår) {
            feilmelding(inngangsvilkårResultat)
        }

        open fun håndter(rapporteringsHendelse: RapporteringsHendelse, vilkårsvurdering: Vilkår) {
            feilmelding(rapporteringsHendelse)
        }

        private fun feilmelding(hendelse: Hendelse) =
            hendelse.warn("Kan ikke håndtere ${hendelse.javaClass.simpleName} i tilstand ${this.tilstandType}")

        open fun accept(vilkår: Vilkår, visitor: VilkårsvurderingVisitor) {}

        abstract class IkkeVurdert<Vilkår : Vilkårsvurdering<Vilkår>> : Tilstand<Vilkår>(Type.IkkeVurdert)
        abstract class Avventer<Vilkår : Vilkårsvurdering<Vilkår>> : Tilstand<Vilkår>(Type.AvventerVurdering)
        abstract class Oppfylt<Vilkår : Vilkårsvurdering<Vilkår>> : Tilstand<Vilkår>(Oppfylt)
        abstract class IkkeOppfylt<Vilkår : Vilkårsvurdering<Vilkår>> : Tilstand<Vilkår>(Type.IkkeOppfylt)
    }

    private fun loggTilstandsendring(nyTilstand: Tilstand<Vilkår>) {
        logger.info { "Vurdering ${this.javaClass.simpleName} endrer tilstand fra ${tilstand.tilstandType} til ny tilstand ${nyTilstand.tilstandType}" }
    }
}
