package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.api.mockSøknadOppgaveDto
import java.util.UUID

interface PersonRepository : OppgaveRepository {
    fun lagre(person: Person)

    fun hent(ident: String): Person?
}

class InMemoryPersonRepository : PersonRepository {
    private val personMap = mutableMapOf<String, Person>()

    init {
        val oppgave =
            Oppgave(
                oppgaveId = mockSøknadOppgaveDto.oppgaveId,
                ident = mockSøknadOppgaveDto.personIdent,
                emneknagger = mockSøknadOppgaveDto.emneknagger.toSet(),
                opprettet = mockSøknadOppgaveDto.tidspunktOpprettet,
                behandlingId = mockSøknadOppgaveDto.behandlingId,
            )
        personMap[mockSøknadOppgaveDto.personIdent] =
            Person(
                ident = mockSøknadOppgaveDto.personIdent,
            ).apply {
                this.behandlinger[mockSøknadOppgaveDto.behandlingId] =
                    Behandling(
                        behandlingId = mockSøknadOppgaveDto.behandlingId,
                        oppgave = oppgave,
                    )
            }
    }

    override fun lagre(person: Person) {
        personMap[person.ident] = person
    }

    override fun hent(ident: String): Person? = personMap[ident]

    override fun hent(oppgaveId: UUID): Oppgave? {
        return hentAlleOppgaver().firstOrNull { oppgave ->
            oppgave.oppgaveId == oppgaveId
        }
    }

    override fun hentAlleOppgaver(): List<Oppgave> =
        personMap.values.flatMap { person ->
            person.behandlinger.values.map { behandling ->
                behandling.oppgave
            }
        }

    override fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave> =
        hentAlleOppgaver().filter { it.tilstand == tilstand }

    fun slettAlt() = personMap.clear()
}
