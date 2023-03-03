package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.entitet.Arbeidstimer
import java.time.LocalDate
import java.util.UUID

class InngangsvilkårResultat(
    ident: String,
    val vilkårsvurderingId: UUID,
    val oppfylt: Boolean,
    val virkningsdato: LocalDate,
    val fastsattArbeidstidPerDag: Arbeidstimer,
) : VilkårResultatHendelse(vilkårsvurderingId, ident)
