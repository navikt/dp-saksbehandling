package no.nav.dagpenger.saksbehandling

import java.util.UUID

interface PersonRepository : OppgaveRepository {
    fun lagre(person: Person)

    fun hent(ident: String): Person?
}

class InMemoryPersonRepository : PersonRepository {
    private val personMap = mutableMapOf<String, Person>()

    override fun lagre(person: Person) {
        personMap[person.ident] = person
    }

    override fun hent(ident: String): Person? = personMap[ident]

    override fun hent(oppgaveId: UUID): Oppgave? =
        hentAlleOppgaver().firstOrNull { oppgave ->
            oppgave.oppgaveId == oppgaveId
        }

    override fun hentAlleOppgaver(): List<Oppgave> =
        personMap.values.flatMap { person ->
            person.behandlinger.values.map { behandling ->
                behandling.oppgave
            }
        }

    fun slettAlt() = personMap.clear()
}
