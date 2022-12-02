package no.nav.dagpenger.behandling.hendelser

import java.util.UUID

class SøknadHendelse(private val søknadUUID: UUID, private val journalpostId: String, ident: String) : Hendelse(ident) {

    fun søknadUUID() = søknadUUID
    fun journalpostId() = journalpostId
}
