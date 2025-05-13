package no.nav.dagpenger.saksbehandling.klage

import io.ktor.http.HttpStatusCode

interface KlageKlient {
    suspend fun registrerKlage(
        klageBehandling: KlageBehandling,
        personIdentId: String,
        fagsakId: String,
        forrigeBehandlendeEnhet: String,
        tilknyttedeJournalposter: List<Journalposter> = listOf(),
    ): Result<HttpStatusCode>
}
