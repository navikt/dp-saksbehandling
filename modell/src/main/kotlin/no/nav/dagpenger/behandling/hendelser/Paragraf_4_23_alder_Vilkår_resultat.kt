package no.nav.dagpenger.behandling.hendelser

import java.util.UUID

class Paragraf_4_23_alder_Vilkår_resultat(
    ident: String,
    val vilkårsvurderingId: UUID,
    val oppfylt: Boolean,
) : VilkårResultatHendelse(vilkårsvurderingId, ident)
