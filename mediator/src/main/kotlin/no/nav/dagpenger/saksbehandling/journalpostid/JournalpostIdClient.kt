package no.nav.dagpenger.saksbehandling.journalpostid

import java.util.UUID

interface JournalpostIdClient {
    suspend fun hentJournalpostIder(
        søknadId: UUID,
        ident: String,
    ): Result<List<String>>
}
