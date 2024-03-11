package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.api.behandlingId1
import no.nav.dagpenger.saksbehandling.api.behandlingId2
import no.nav.dagpenger.saksbehandling.api.personIdent
import no.nav.dagpenger.saksbehandling.api.personIdent2
import java.time.ZonedDateTime
import java.util.UUID

class InMemoryRepository : Repository {
    private val personMap = mutableMapOf<String, Person>()
    private val behandlinger = mutableMapOf<UUID, Behandling>()

    init {
        opprettMockData()
    }

    override fun lagre(person: Person) {
        personMap[person.ident] = person
    }

    override fun lagre(behandling: Behandling) {
        behandlinger[behandling.behandlingId] = behandling
    }

    override fun hentBehandlingFra(oppgaveId: UUID): Behandling {
        return behandlinger.values.first { behandling ->
            behandling.oppgaver.any { it.oppgaveId == oppgaveId }
        }
    }

    override fun hentBehandling(behandlingId: UUID): Behandling {
        return behandlinger[behandlingId] ?: throw IllegalArgumentException("Fant ikke behandling med id $behandlingId")
    }

    override fun finnPerson(ident: String): Person? {
        return personMap[ident]
    }

    override fun hentPerson(ident: String): Person {
        return personMap[ident]!!
    }

    override fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave> =
        behandlinger.values.flatMap { it.oppgaver }.filter { it.tilstand == tilstand }

    override fun hentOppgave(oppgaveId: UUID): Oppgave? {
        return behandlinger.values.flatMap { it.oppgaver }.singleOrNull { it.oppgaveId == oppgaveId }
    }

    override fun finnOppgaverFor(ident: String): List<Oppgave> {
        TODO("Not yet implemented")
    }

    fun slettAlt() {
        personMap.clear()
        behandlinger.clear()
    }

    private fun opprettMockData() {
        val person = Person(ident = personIdent)
        personMap[person.ident] = person

        val person2 = Person(ident = personIdent2)
        personMap[person2.ident] = person2

        val behandling = Behandling(
            behandlingId = behandlingId1,
            person = person,
            opprettet = ZonedDateTime.now(),
        )
        behandlinger[behandling.behandlingId] = behandling

        val behandling2 = Behandling(
            behandlingId = behandlingId2,
            person = person2,
            opprettet = ZonedDateTime.now(),
        )
        behandlinger[behandling2.behandlingId] = behandling2
    }
}
