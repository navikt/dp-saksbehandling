package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Hubba
import no.nav.dagpenger.behandling.Person
import java.util.UUID

class SøknadHendelse(private val søknadId: UUID, private val journalpostId: String, ident: String) : Hendelse(ident) {

    fun søknadId() = søknadId
    fun journalpostId() = journalpostId
    fun lagBehandling(): Behandling {
        return Hubba.bubba(Person(ident()))
    }
}
