package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Grunnlag
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sats
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakInnvilget
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse

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

        fun entering(søknadHendelse: Hendelse, behandling: Behandling) {}
        fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat, behandling: Behandling) {
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

        override fun entering(søknadHendelse: Hendelse, behandling: Behandling) {
            val inntektId = requireNotNull(behandling.inntektId) {
                "Vi forventer at inntektId er satt ved tilstandsendring til ${UnderBeregning.javaClass.simpleName}"
            }.let { mapOf("inntektId" to it) }
            val virkningsdato = requireNotNull(behandling.virkningsdato) {
                "Vi forventer at virkningsdato er satt ved tilstandsendring til ${UnderBeregning.javaClass.simpleName}"
            }.let {
                mapOf("virkningsdato" to it)
            }
            søknadHendelse.behov(Grunnlag, "Trenger grunnlag", virkningsdato + inntektId)
            søknadHendelse.behov(Sats, "Trenger sats", )
        }
        override fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat, behandling: Behandling) {
            grunnlagOgSatsResultat.behov(VedtakInnvilget, "Vedtak innvilget")
        }
    }

