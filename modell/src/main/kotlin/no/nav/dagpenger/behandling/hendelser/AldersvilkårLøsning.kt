package no.nav.dagpenger.behandling.hendelser

import java.util.UUID

data class AldersvilkårLøsning(
    private val ident: String,
    val oppfylt: Boolean,
    private val behandlingId: UUID
) : LøsningHendelse(behandlingId, ident)
