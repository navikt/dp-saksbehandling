package no.nav.dagpenger.behandling

import mu.KotlinLogging
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Paragraf_4_23_alder_vilkår
import no.nav.dagpenger.behandling.vilkår.TestVilkår
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.erAlleOppfylt
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.erFerdig
import no.nav.dagpenger.behandling.visitor.NyRettighetsbehandlingVisitor
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
        person,
        søknadUUID,
        UUID.randomUUID(),
        VurdererVilkår,
        null,
        null
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
    private lateinit var grunnlagOgSatsResultat: GrunnlagOgSatsResultat

    private var foreløpigInnstilling: ForeløpigInnstilling? = null

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

    fun accept(visitor: NyRettighetsbehandlingVisitor) {
        visitor.visitNyRettighetsbehandling(søknadsId, behandlingsId, tilstand, virkningsdato, inntektsId)
        vilkårsvurderinger.forEach {
            it.accept(visitor)
        }
        foreløpigInnstilling?.accept(visitor)
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

        fun håndter(søknadHendelse: SøknadHendelse, behandling: NyRettighetsbehandling) {
            søknadHendelse.tilstandfeil()
        }

        fun håndter(
            paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat,
            behandling: NyRettighetsbehandling,
        ) {
            paragraf423AlderResultat.tilstandfeil()
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
            if (behandling.vilkårsvurderinger.erFerdig()) {
                behandling.endreTilstand(VurdererUtfall, paragraf423AlderResultat)
            }
        }
    }

    object VurdererUtfall : Tilstand {
        override val type: Tilstand.Type
            get() = Tilstand.Type.VurdererUtfall

        override fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {
            require(behandling.vilkårsvurderinger.erFerdig()) { "Vilkårsvurderinger må være ferdig vurdert på dette tidspunktet" }
            if (behandling.vilkårsvurderinger.erAlleOppfylt()) {
                behandling.foreløpigInnstilling = ForeløpigInnstilling(utfall = true)
                behandling.endreTilstand(UtførerBeregning, hendelse)
            } else {
                behandling.foreløpigInnstilling = ForeløpigInnstilling(utfall = false)
                behandling.endreTilstand(Kvalitetssikrer, hendelse)
            }
        }
    }

    object UtførerBeregning : Tilstand {
        override val type: Tilstand.Type
            get() = Tilstand.Type.UtførerBeregning

        override fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {
            behandling.inntektsId = "ULID"
            behandling.virkningsdato = LocalDate.now()

            val inntektId = requireNotNull(behandling.inntektsId) {
                "Vi forventer at inntektId er satt ved tilstandsendring til ${UtførerBeregning.javaClass.simpleName}"
            }.let { mapOf("inntektId" to it) }
            val virkningsdato = requireNotNull(behandling.virkningsdato) {
                "Vi forventer at virkningsdato er satt ved tilstandsendring til ${UtførerBeregning.javaClass.simpleName}"
            }.let {
                mapOf("virkningsdato" to it)
            }
            hendelse.behov(
                Aktivitetslogg.Aktivitet.Behov.Behovtype.Grunnlag,
                "Trenger grunnlag",
                virkningsdato + inntektId
            )
            hendelse.behov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Sats, "Trenger sats")
        }

        override fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat, behandling: NyRettighetsbehandling) {
            behandling.foreløpigInnstilling?.grunnlag = grunnlagOgSatsResultat.grunnlag
            behandling.foreløpigInnstilling?.sats = grunnlagOgSatsResultat.dagsats
            behandling.endreTilstand(Kvalitetssikrer, grunnlagOgSatsResultat)
        }
    }

    object Kvalitetssikrer : Tilstand {
        override val type: Tilstand.Type
            get() = Tilstand.Type.Kvalitetssikrer

        override fun entering(hendelse: Hendelse, behandling: NyRettighetsbehandling) {
        }
    }

    private fun loggTilstandsendring(nyTilstand: Tilstand) {
        logger.info { "Behandling av ${this.javaClass.simpleName} endrer tilstand fra ${tilstand.type} til ny tilstand ${nyTilstand.type}" }
    }
}
