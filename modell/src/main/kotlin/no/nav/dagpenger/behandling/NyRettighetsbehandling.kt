package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakAvslått
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakInnvilget
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Paragraf_4_23_alder_vilkår
import no.nav.dagpenger.behandling.vilkår.TestVilkår
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.erFerdig
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Tilstand.Type.Oppfylt
import java.time.LocalDate
import java.util.UUID

class NyRettighetsbehandling private constructor(private val søknadUUID: UUID, behandlingsId: UUID, private val tilstand: Tilstand) :
    Behandling(behandlingsId, tilstand) {

    companion object {
        fun List<Behandling>.harSøknadUUID(søknadUUID: UUID) =
            this.any { it is NyRettighetsbehandling && it.søknadUUID == søknadUUID }
    }

    constructor(søknadUUID: UUID) : this(søknadUUID, UUID.randomUUID(), Vilkårsvurdering)

    override val vilkårsvurderinger by lazy {
        listOf(
            Paragraf_4_23_alder_vilkår(),
            TestVilkår(),
        )
    }

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst(
            kontekstType = kontekstType,
            mapOf(
                "behandlingId" to behandlingId.toString(),
                "type" to this.javaClass.simpleName,
                "søknad_uuid" to søknadUUID.toString()
            )
        )

    override fun håndter(hendelse: SøknadHendelse) {
        kontekst(hendelse, "Opprettet ny rettighetsbehandling basert på søknadhendelse")
        vilkårsvurderinger.forEach { vurdering ->
            vurdering.håndter(hendelse)
        }
    }

    override fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat) {
        kontekst(paragraf423AlderResultat, "Fått resultat på ${paragraf423AlderResultat.javaClass.simpleName}")
        vilkårsvurderinger.forEach { vurdering ->
            vurdering.håndter(paragraf423AlderResultat)
        }
        ferdigstillRettighetsbehandling(paragraf423AlderResultat)
    }

    override fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat) {
        if (grunnlagOgSatsResultat.behandlingId != this.behandlingId) return
        kontekst(grunnlagOgSatsResultat, "Fått resultat på ${grunnlagOgSatsResultat.javaClass.simpleName}")
        tilstand.håndter(grunnlagOgSatsResultat, this)
    }

    private fun ferdigstillRettighetsbehandling(hendelse: Hendelse) {
        if (vilkårsvurderinger.erFerdig()) {

            this.virkningsdato = LocalDate.now()
            this.inntektId = "en eller annen ULID"

            if (vilkårsvurderinger.all { it.tilstand.tilstandType == Oppfylt }) {
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
}
