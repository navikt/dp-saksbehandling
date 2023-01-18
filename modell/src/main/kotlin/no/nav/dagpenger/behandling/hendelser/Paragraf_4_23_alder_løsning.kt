package no.nav.dagpenger.behandling.hendelser

import java.util.UUID

data class Paragraf_4_23_alder_løsning(
    private val ident: String,
    val vilkårvurderingId: UUID,
    private val behandlingId: UUID
) : LøsningHendelse(behandlingId, ident)
