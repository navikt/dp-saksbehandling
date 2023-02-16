package no.nav.dagpenger.behandling.hendelser

import java.time.LocalDate
import java.util.UUID

class AlderVilkårResultat(
    ident: String,
    val vilkårsvurderingId: UUID,
    val oppfylt: Boolean,
    val virkningsdato: LocalDate,
) : VilkårResultatHendelse(vilkårsvurderingId, ident)
