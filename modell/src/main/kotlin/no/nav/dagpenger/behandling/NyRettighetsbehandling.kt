package no.nav.dagpenger.behandling

import mu.KotlinLogging
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Kvalitetssikring
import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse
import no.nav.dagpenger.behandling.fastsettelse.Fastsettelse.Companion.vurdert
import no.nav.dagpenger.behandling.fastsettelse.Paragraf_4_11_Grunnlag
import no.nav.dagpenger.behandling.fastsettelse.Paragraf_4_12_Sats
import no.nav.dagpenger.behandling.fastsettelse.Paragraf_4_15_Stønadsperiode
import no.nav.dagpenger.behandling.hendelser.BeslutterHendelse
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.StønadsperiodeResultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Paragraf_4_23_alder_vilkår
import no.nav.dagpenger.behandling.vilkår.TestVilkår
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.erAlleOppfylt
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.vurdert
import no.nav.dagpenger.behandling.visitor.FastsettelseVisitor
import no.nav.dagpenger.behandling.visitor.NyRettighetsbehandlingVisitor
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger { }

class NyRettighetsbehandling private constructor(
    private val person: Person,
    private val søknadsId: UUID,
    private val behandlingsId: UUID,
    private var tilstand: Tilstand,
    private var virkningsdato: LocalDate?,
    private var inntektsId: String?,
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : Aktivitetskontekst {

    constructor(person: Person, søknadUUID: UUID) : this(
        person = person,
        søknadsId = søknadUUID,
        behandlingsId = UUID.randomUUID(),
        tilstand = VurdererVilkår,
        virkningsdato = null,
        inntektsId = null
    )

    companion object {
        fun List<NyRettighetsbehandling>.harSøknadUUID(søknadUUID: UUID) =
            this.any { it.søknadsId == søknadUUID }

        const val kontekstType = "Behandling"
    }

    private val vilkårsvurderinger by lazy {
        listOf(
            Paragraf_4_23_alder_vilkår(),
            TestVilkår(),
        )
    }

    private val fastsettelser by lazy {
        listOf(
            Paragraf_4_11_Grunnlag(
                requireNotNull(this.inntektsId),
                requireNotNull(this.virkningsdato)
            ),
            Paragraf_4_12_Sats(
                requireNotNull(this.inntektsId),
                requireNotNull(this.virkningsdato)
            ),
            Paragraf_4_15_Stønadsperiode(
                requireNotNull(this.inntektsId),
                requireNotNull(this.virkningsdato)
            )
        )
    }

    fun håndter(hendelse: SøknadHendelse) {
        kontekst(hendelse, "Opprettet ny rettighetsbehandling basert på søknadhendelse")
        tilstand.håndter(hendelse, this)
    }

    fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat) {
        kontekst(paragraf423AlderResultat, "Fått resultat på ${paragraf423AlderResultat.javaClass.simpleName}")
        tilstand.håndter(paragraf423AlderResultat, this)
    }

    fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat) {
        if (grunnlagOgSatsResultat.behandlingId != this.behandlingsId) return
        kontekst(grunnlagOgSatsResultat, "Fått resultat på ${grunnlagOgSatsResultat.javaClass.simpleName}")
        tilstand.håndter(grunnlagOgSatsResultat, this)
    }

    fun håndter(stønadsperiode: StønadsperiodeResultat) {
        if (stønadsperiode.behandlingId != this.behandlingsId) return
        kontekst(stønadsperiode, "Fått resultat på ${stønadsperiode.javaClass.simpleName}")
        tilstand.håndter(stønadsperiode, this)
    }

    fun håndter(beslutterHendelse: BeslutterHendelse) {
        if (beslutterHendelse.behandlingId != this.behandlingsId) return
        kontekst(beslutterHendelse, "Beslutter har fattet et vedtak")
        tilstand.håndter(beslutterHendelse, this)
    }

    fun accept(visitor: NyRettighetsbehandlingVisitor) {
        visitor.visitNyRettighetsbehandling(søknadsId, behandlingsId, tilstand, virkningsdato, inntektsId)
        vilkårsvurderinger.forEach {
            it.accept(visitor)
        }
    }

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst(
            kontekstType = kontekstType,
            mapOf(
                "behandlingsId" to behandlingsId.toString(),
                "type" to this.javaClass.simpleName,
                "søknad_uuid" to søknadsId.toString()
            )
        )

    private fun endreTilstand(nyTilstand: Tilstand, søknadHendelse: Hendelse) {
        if (nyTilstand == tilstand) {
            return // Vi er allerede i tilstanden
        }
        // val forrigeTilstand = tilstand
        loggTilstandsendring(nyTilstand)
        tilstand = nyTilstand
        søknadHendelse.kontekst(tilstand)
        tilstand.entering(søknadHendelse, this)
    }

    private fun kontekst(hendelse: Hendelse, melding: String? = null) {
        hendelse.kontekst(this)
        melding?.let {
            hendelse.info(it)
        }
    }

    interface Tilstand : Aktivitetskontekst {
        val type: Type

        enum class Type {
            VurdererVilkår,
            VurdererUtfall,
            UtførerBeregning,
            Kvalitetssikrer,
            FattetVedtak
        }

        override fun toSpesifikkKontekst() =
            SpesifikkKontekst(
                kontekstType = "Tilstand",
                mapOf(
                    "type" to type.name
                )
            )

        fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {}

        fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat, behandling: NyRettighetsbehandling) {
            grunnlagOgSatsResultat.tilstandfeil()
        }

        fun håndter(stønadsperiode: StønadsperiodeResultat, nyRettighetsbehandling: NyRettighetsbehandling) {
            stønadsperiode.tilstandfeil()
        }

        fun håndter(søknadHendelse: SøknadHendelse, behandling: NyRettighetsbehandling) {
            søknadHendelse.tilstandfeil()
        }

        fun håndter(
            paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat,
            behandling: NyRettighetsbehandling,
        ) {
            paragraf423AlderResultat.tilstandfeil()
        }

        fun håndter(beslutterHendelse: BeslutterHendelse, behandling: NyRettighetsbehandling) {
            beslutterHendelse.tilstandfeil()
        }

        private fun Hendelse.tilstandfeil() {
            this.warn("Forventet ikke ${this.javaClass.simpleName} i tilstand ${type.name} ")
        }
    }

    object VurdererVilkår : Tilstand {
        override val type: Tilstand.Type
            get() = Tilstand.Type.VurdererVilkår

        override fun håndter(søknadHendelse: SøknadHendelse, behandling: NyRettighetsbehandling) {
            behandling.vilkårsvurderinger.forEach { vurdering ->
                vurdering.håndter(søknadHendelse)
            }
        }

        override fun håndter(
            paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat,
            behandling: NyRettighetsbehandling,
        ) {
            behandling.vilkårsvurderinger.forEach { vurdering ->
                vurdering.håndter(paragraf423AlderResultat)
            }
            if (behandling.vilkårsvurderinger.vurdert()) {
                behandling.endreTilstand(VurdererUtfall, paragraf423AlderResultat)
            }
        }
    }

    object VurdererUtfall : Tilstand {
        override val type: Tilstand.Type
            get() = Tilstand.Type.VurdererUtfall

        override fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {
            behandling.virkningsdato = LocalDate.now() // Må være satt i vilkårsvurderinger
            require(behandling.vilkårsvurderinger.vurdert()) { "Vilkårsvurderinger må være ferdig vurdert på dette tidspunktet" }
            if (behandling.vilkårsvurderinger.erAlleOppfylt()) {
                behandling.endreTilstand(UtførerBeregning, hendelse)
            } else {
                behandling.endreTilstand(Kvalitetssikrer, hendelse)
            }
        }
    }

    object UtførerBeregning : Tilstand {
        override val type: Tilstand.Type
            get() = Tilstand.Type.UtførerBeregning

        override fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {
            behandling.inntektsId = "ULID"
            behandling.fastsettelser.forEach { it.håndter(hendelse) }
        }

        override fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat, behandling: NyRettighetsbehandling) {
            behandling.fastsettelser.forEach { it.håndter(grunnlagOgSatsResultat) }
            if (behandling.fastsettelser.vurdert()) {
                behandling.endreTilstand(Kvalitetssikrer, grunnlagOgSatsResultat)
            }
        }

        override fun håndter(stønadsperiode: StønadsperiodeResultat, behandling: NyRettighetsbehandling) {
            behandling.fastsettelser.forEach { it.håndter(stønadsperiode) }
            if (behandling.fastsettelser.vurdert()) {
                behandling.endreTilstand(Kvalitetssikrer, stønadsperiode)
            }
        }
    }

    object Kvalitetssikrer : Tilstand {
        override val type: Tilstand.Type
            get() = Tilstand.Type.Kvalitetssikrer

        override fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {
            hendelse.behov(Kvalitetssikring, "Behøver kvalitetssikring i form av totrinnskontroll fra en beslutter")
        }

        override fun håndter(beslutterHendelse: BeslutterHendelse, behandling: NyRettighetsbehandling) {
            behandling.endreTilstand(FattetVedtak, beslutterHendelse)
        }
    }

    object FattetVedtak : Tilstand {
        override val type: Tilstand.Type
            get() = Tilstand.Type.FattetVedtak

        override fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {
            behandling.opprettVedtak()
        }
    }

    private fun opprettVedtak() {
        require(vilkårsvurderinger.vurdert()) { " Alle vilkår må være vurdert når en skal opprette vedtak" }
        val vedtak = when (vilkårsvurderinger.erAlleOppfylt()) {
            true -> {
                val visitor = VedtakFastsettelseVisitor(fastsettelser)
                Vedtak.innvilgelse(
                    requireNotNull(virkningsdato),
                    grunnlag = visitor.grunnlag,
                    dagsats = visitor.dagsats,
                    stønadsperiode = visitor.stønadsperiode
                )
            }

            false -> Vedtak.avslag(requireNotNull(virkningsdato))
        }
        this.person.leggTilVedtak(vedtak)
    }

    private fun loggTilstandsendring(nyTilstand: Tilstand) {
        logger.info { "Behandling av ${this.javaClass.simpleName} endrer tilstand fra ${tilstand.type} til ny tilstand ${nyTilstand.type}" }
    }

    private class VedtakFastsettelseVisitor(fastsettelser: List<Fastsettelse<*>>) : FastsettelseVisitor {
        lateinit var grunnlag: BigDecimal
        lateinit var dagsats: BigDecimal
        lateinit var stønadsperiode: BigDecimal

        init {
            fastsettelser.forEach { it.accept(this) }
        }

        override fun visitGrunnlag(grunnlag: BigDecimal) {
            this.grunnlag = grunnlag
        }

        override fun visitDagsats(dagsats: BigDecimal) {
            this.dagsats = dagsats
        }

        override fun visitStønadsperiode(stønadsperiode: BigDecimal) {
            this.stønadsperiode = stønadsperiode
        }
    }
}
