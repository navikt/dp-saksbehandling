package no.nav.dagpenger.saksbehandling.journalpostid

import java.util.UUID

interface JournalpostIdClient {
    fun hentJournalpostId(søknadId: UUID): Result<String>
}
