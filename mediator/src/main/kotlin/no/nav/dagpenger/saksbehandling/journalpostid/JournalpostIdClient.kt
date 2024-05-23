package no.nav.dagpenger.saksbehandling.journalpostid

import java.util.UUID

interface JournalpostIdClient {
    suspend fun hentJournalpostId(søknadId: UUID): Result<String>
}
