package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Grunnlag
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sats
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import java.time.LocalDate
import java.util.UUID

abstract class Behandling private constructor(
    internal val behandlingId: UUID,
    private var tilstand: Tilstand,
    protected var virkningsdato: LocalDate?,
    protected var inntektId: String?,
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : Aktivitetskontekst {
    abstract val vilkårsvurderinger: List<no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering<*>>

    abstract fun håndter(hendelse: SøknadHendelse)
    abstract fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_resultat)

    constructor(behandlingId: UUID, tilstand: Tilstand) : this(behandlingId, tilstand, null, null)

    companion object {
        const val kontekstType = "Behandling"
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

        fun entering(søknadHendelse: Hendelse, behandling: Behandling) {}
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
    }

    protected fun endreTilstand(nyTilstand: Tilstand, søknadHendelse: Hendelse) {
        if (nyTilstand == tilstand) {
            return // Vi er allerede i tilstanden
        }
        val forrigeTilstand = tilstand
        tilstand = nyTilstand
        søknadHendelse.kontekst(tilstand)
        tilstand.entering(søknadHendelse, this)
    }
}
