package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class VedtakFattetHendelse(
    val behandlingId: UUID,
    val søknadId: UUID,
    val ident: String,
    val sakId: SakId,
) {
    data class SakId(
        val id: String,
        val kontekst: String,
    ) {
        fun toMap(): Map<String, String> {
            return mapOf("id" to id, "kontekst" to kontekst)
        }
    }
}
