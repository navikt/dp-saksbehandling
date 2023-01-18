package no.nav.dagpenger.behandling.hendelser

import java.util.UUID

class Paragraf_4_23_alder_resultat(
    ident: String,
    val vilkårvurderingId: UUID,
    behandlingId: UUID,
    val oppfylt: Boolean,
) : LøsningHendelse(behandlingId, ident)
