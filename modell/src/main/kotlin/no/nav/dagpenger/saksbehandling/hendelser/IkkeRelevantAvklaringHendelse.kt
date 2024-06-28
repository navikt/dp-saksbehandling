package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class IkkeRelevantAvklaringHendelse(
    val ident: String,
    val behandlingId: UUID,
    val ikkeRelevantEmneknagg: String,
) {
    override fun toString(): String {
        return "IkkeRelevantAvklaringHendelse(behandlingId=$behandlingId, ikkeRelevantEmneknagg='$ikkeRelevantEmneknagg')"
    }
}
