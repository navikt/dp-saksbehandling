package no.nav.dagpenger.saksbehandling.klage

import io.ktor.http.HttpStatusCode

interface KlageKlient {
    suspend fun registrerKlage(
        klageBehandling: KlageBehandling,
        ident: String,
        fagsakId: String,
        tilknyttedeJournalposter: List<Journalposter> = emptyList(),
    ): Result<HttpStatusCode>
}
