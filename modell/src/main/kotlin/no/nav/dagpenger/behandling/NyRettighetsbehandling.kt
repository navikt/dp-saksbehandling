package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakAvslått
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakInnvilget
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

class NyRettighetsbehandling private constructor(
    private val søknadsId: UUID,
    private val behandlingsId: UUID,
    private var tilstand: Tilstand,
    private var virkningsdato: LocalDate?,
    private var inntektsId: String?,
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : Aktivitetskontekst {

    constructor(søknadUUID: UUID) : this(søknadUUID, UUID.randomUUID(), Vilkårsvurdering, null, null)

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

    fun håndter(hendelse: SøknadHendelse) {
        kontekst(hendelse, "Opprettet ny rettighetsbehandling basert på søknadhendelse")
        vilkårsvurderinger.forEach { vurdering ->
            vurdering.håndter(hendelse)
        }
    }

    fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat) {
        kontekst(paragraf423AlderResultat, "Fått resultat på ${paragraf423AlderResultat.javaClass.simpleName}")
        vilkårsvurderinger.forEach { vurdering ->
            vurdering.håndter(paragraf423AlderResultat)
        }
        ferdigstillRettighetsbehandling(paragraf423AlderResultat)
    }

    fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat) {
        if (grunnlagOgSatsResultat.behandlingId != this.behandlingsId) return
        kontekst(grunnlagOgSatsResultat, "Fått resultat på ${grunnlagOgSatsResultat.javaClass.simpleName}")
        tilstand.håndter(grunnlagOgSatsResultat, this)
    }

    private fun endreTilstand(nyTilstand: Tilstand, søknadHendelse: Hendelse) {
        if (nyTilstand == tilstand) {
            return // Vi er allerede i tilstanden
        }
        val forrigeTilstand = tilstand
        tilstand = nyTilstand
        søknadHendelse.kontekst(tilstand)
        tilstand.entering(søknadHendelse, this)
    }

    private fun ferdigstillRettighetsbehandling(hendelse: Hendelse) {
        if (vilkårsvurderinger.erFerdig()) {

            this.virkningsdato = LocalDate.now()
            this.inntektsId = "en eller annen ULID"

            if (vilkårsvurderinger.erAlleOppfylt()) {
                this.endreTilstand(UnderBeregning, hendelse)
                hendelse.behov(VedtakInnvilget, "Vedtak innvilget", mapOf("sats" to 123))
            } else {
                hendelse.behov(VedtakAvslått, "Vedtak avslått")
            }
        }
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
            Vilkårsvurdering,
            UnderBeregning,
            Behandlet
        }

        override fun toSpesifikkKontekst() =
            SpesifikkKontekst(
                kontekstType = "Tilstand",
                mapOf(
                    "type" to type.name
                )
            )

        fun entering(søknadHendelse: Hendelse, behandling: NyRettighetsbehandling) {}
        fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat, behandling: NyRettighetsbehandling) {
            grunnlagOgSatsResultat.warn("Forventet ikke grunnlag og sats i tilstand ${type.name}.")
        }
    }

    object Vilkårsvurdering : Tilstand {
        override val type: Tilstand.Type
            get() = Tilstand.Type.Vilkårsvurdering
    }

    object UnderBeregning : Tilstand {
        override val type: Tilstand.Type
            get() = Tilstand.Type.UnderBeregning

        override fun entering(søknadHendelse: Hendelse, behandling: NyRettighetsbehandling) {
            val inntektId = requireNotNull(behandling.inntektsId) {
                "Vi forventer at inntektId er satt ved tilstandsendring til ${UnderBeregning.javaClass.simpleName}"
            }.let { mapOf("inntektId" to it) }
            val virkningsdato = requireNotNull(behandling.virkningsdato) {
                "Vi forventer at virkningsdato er satt ved tilstandsendring til ${UnderBeregning.javaClass.simpleName}"
            }.let {
                mapOf("virkningsdato" to it)
            }
            søknadHendelse.behov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Grunnlag, "Trenger grunnlag", virkningsdato + inntektId)
            søknadHendelse.behov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Sats, "Trenger sats")
        }

        override fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat, behandling: NyRettighetsbehandling) {
            grunnlagOgSatsResultat.behov(VedtakInnvilget, "Vedtak innvilget")
        }
    }
}
