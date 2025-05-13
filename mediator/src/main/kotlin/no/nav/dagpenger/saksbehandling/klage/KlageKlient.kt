package no.nav.dagpenger.saksbehandling.klage

import io.ktor.http.HttpStatusCode
import java.util.UUID

interface KlageKlient {
    suspend fun registrerKlage(
        personIdentId: String,
        fagsakId: String,
        behandlingId: UUID,
        forrigeBehandlendeEnhet: String,
        tilknyttedeJournalposter: List<Journalposter> = listOf(),
        hjemler: List<Hjemler>,
    ): Result<HttpStatusCode>
}
