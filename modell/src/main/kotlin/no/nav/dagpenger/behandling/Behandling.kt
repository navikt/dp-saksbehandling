package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Grunnlag
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sats
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import java.time.LocalDate
import java.util.UUID

abstract class Behandling private constructor(
    internal val behandlingId: UUID,
    private var tilstand: Tilstand,
    internal var virkningsdato: LocalDate?,
    internal var inntektId: String?,
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : Aktivitetskontekst {
    abstract val vilkårsvurderinger: List<no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering<*>>

    abstract fun håndter(hendelse: SøknadHendelse)
    abstract fun håndter(paragraf423AlderResultat: Paragraf_4_23_alder_Vilkår_resultat)
    abstract fun håndter(grunnlagOgSatsResultat: GrunnlagOgSatsResultat)

    constructor(behandlingId: UUID, tilstand: Tilstand) : this(behandlingId, tilstand, null, null)

    companion object {
        const val kontekstType = "Behandling"
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
