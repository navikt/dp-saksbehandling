package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.api.minsteinntektOppgaveFerdigBehandletId
import no.nav.dagpenger.saksbehandling.api.minsteinntektOppgaveTilBehandlingId
import no.nav.dagpenger.saksbehandling.api.personIdent
import no.nav.dagpenger.saksbehandling.api.personIdent2
import java.util.UUID

class InMemoryRepository : PersonRepository, OppgaveRepository {
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

    override fun hentPerson(ident: String): Person? {
        return personMap[ident]
    }

    override fun lagre(oppgave: Oppgave) {
        behandlinger.values.single { behandling ->
            behandling.oppgaver.any { it.oppgaveId == oppgave.oppgaveId }
        }.let { behandling ->
            behandling.oppgaver.removeIf { it.oppgaveId == oppgave.oppgaveId }
            behandling.oppgaver.add(oppgave)
        }
    }

    override fun hentAlleOppgaver(): List<Oppgave> {
        return behandlinger.values.flatMap { it.oppgaver }
    }

    override fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave> =
        hentAlleOppgaver().filter { it.tilstand == tilstand }

    override fun hentOppgave(oppgaveId: UUID): Oppgave? {
        return behandlinger.values.flatMap { it.oppgaver }.singleOrNull { it.oppgaveId == oppgaveId }
    }

    fun slettAlt() {
        personMap.clear()
        behandlinger.clear()
    }

    private fun opprettMockData() {
        val person = Person(personIdent)
        personMap[person.ident] = person

        val person2 = Person(personIdent2)
        personMap[person2.ident] = person2

        val behandling = Behandling(
            behandlingId = minsteinntektOppgaveTilBehandlingId,
            person = person,
        )
        behandlinger[behandling.behandlingId] = behandling

        val behandling2 = Behandling(
            behandlingId = minsteinntektOppgaveFerdigBehandletId,
            person = person2,
        )
        behandlinger[behandling2.behandlingId] = behandling2
    }
}
