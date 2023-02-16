package no.nav.dagpenger.behandling

import mu.KotlinLogging
import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse
import no.nav.dagpenger.behandling.hendelser.AlderVilkårResultat
import no.nav.dagpenger.behandling.hendelser.BeslutterHendelse
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.hendelser.StønadsperiodeResultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering
import no.nav.dagpenger.behandling.visitor.BehandlingVisitor
import java.util.UUID

private val logger = KotlinLogging.logger { }

abstract class Behandling<Behandlingstype : Behandling<Behandlingstype>>(
    private val person: Person,
    private val behandlingsId: UUID,
    protected val hendelseId: UUID,
    protected var tilstand: Tilstand<Behandlingstype>,
    protected val vilkårsvurderinger: List<Vilkårsvurdering<*>>,

    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : Aktivitetskontekst {

    internal abstract val fastsettelser: List<Fastsettelse<*>>
    open fun håndter(paragraf423AlderResultat: AlderVilkårResultat) {
        kanIkkeHåndtere(paragraf423AlderResultat)
    }

    open fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat) {
        kanIkkeHåndtere(grunnlagOgSatsResultat)
    }

    open fun håndter(stønadsperiode: StønadsperiodeResultat) {
        kanIkkeHåndtere(stønadsperiode)
    }

    open fun håndter(beslutterHendelse: BeslutterHendelse) {
        kanIkkeHåndtere(beslutterHendelse)
    }

    fun accept(visitor: BehandlingVisitor) {
        visitor.preVisit(behandlingsId, hendelseId)
        visitor.visitTilstand(tilstand.type)
        vilkårsvurderinger.forEach { it.accept(visitor) }
        visitor.postVisit(behandlingsId, hendelseId)
    }

    protected fun kontekst(hendelse: Hendelse, melding: String? = null) {
        hendelse.kontekst(this)
        melding?.let {
            hendelse.info(it)
        }
    }

    private fun kanIkkeHåndtere(hendelse: Hendelse) {
        hendelse.severe("${this.javaClass.simpleName} vet ikke hvordan vi skal behandle ${hendelse.javaClass.simpleName}")
    }

    companion object {
        fun List<Behandling<*>>.harHendelseId(hendelseId: UUID) =
            this.any { it.hendelseId == hendelseId }

        const val kontekstType = "Behandling"
    }

    protected abstract fun <T> implementasjon(block: Behandlingstype.() -> T): T
    protected fun endreTilstand(nyTilstand: Tilstand<Behandlingstype>, søknadHendelse: Hendelse) {
        if (nyTilstand == tilstand) {
            return // Vi er allerede i tilstanden
        }
        // val forrigeTilstand = tilstand
        loggTilstandsendring(nyTilstand)
        tilstand = nyTilstand
        søknadHendelse.kontekst(tilstand)
        implementasjon { tilstand.entering(søknadHendelse, this) }
    }

    private fun loggTilstandsendring(nyTilstand: Tilstand<Behandlingstype>) {
        logger.info { "Behandling av ${this.javaClass.simpleName} endrer tilstand fra ${tilstand.type} til ny tilstand ${nyTilstand.type}" }
    }

    sealed class Tilstand<Behandlingstype : Behandling<Behandlingstype>>(val type: Type) : Aktivitetskontekst {

        enum class Type {
            VurdererVilkår,
            VurdererUtfall,
            Fastsetter,
            Kvalitetssikrer,
            Behandlet
        }

        override fun toSpesifikkKontekst() =
            SpesifikkKontekst(
                kontekstType = "Tilstand",
                mapOf(
                    "type" to type.name
                )
            )

        open fun entering(hendelse: Hendelse, behandling: Behandlingstype) {}

        open fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat, behandling: Behandlingstype) {
            grunnlagOgSatsResultat.tilstandfeil()
        }

        open fun håndter(dagpengeperiode: StønadsperiodeResultat, behandling: Behandlingstype) {
            dagpengeperiode.tilstandfeil()
        }

        open fun håndter(søknadHendelse: SøknadHendelse, behandling: Behandlingstype) {
            søknadHendelse.tilstandfeil()
        }

        open fun håndter(rapporteringsHendelse: RapporteringsHendelse, behandlingstype: Behandlingstype) {
            rapporteringsHendelse.tilstandfeil()
        }

        open fun håndter(
            paragraf423AlderResultat: AlderVilkårResultat,
            behandling: Behandlingstype,
        ) {
            paragraf423AlderResultat.tilstandfeil()
        }

        open fun håndter(beslutterHendelse: BeslutterHendelse, behandling: Behandlingstype) {
            beslutterHendelse.tilstandfeil()
        }

        abstract class VurdererVilkår<Behandlingstype : Behandling<Behandlingstype>> : Tilstand<Behandlingstype>(Type.VurdererVilkår)
        abstract class VurderUtfall<Behandlingstype : Behandling<Behandlingstype>> : Tilstand<Behandlingstype>(Type.VurdererUtfall)
        abstract class Fastsetter<Behandlingstype : Behandling<Behandlingstype>> : Tilstand<Behandlingstype>(Type.Fastsetter)
        abstract class Kvalitetssikrer<Behandlingstype : Behandling<Behandlingstype>> : Tilstand<Behandlingstype>(Type.Kvalitetssikrer)
        abstract class Behandlet<Behandlingstype : Behandling<Behandlingstype>> : Tilstand<Behandlingstype>(Type.Behandlet)

        private fun Hendelse.tilstandfeil() {
            this.warn("Forventet ikke ${this.javaClass.simpleName} i tilstand ${type.name} ")
        }
    }
}
