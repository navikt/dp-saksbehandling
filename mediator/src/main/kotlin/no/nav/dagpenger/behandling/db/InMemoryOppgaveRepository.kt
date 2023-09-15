package no.nav.dagpenger.behandling.db

import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.behandling.oppgave.Oppgave
import java.util.UUID

class InMemoryOppgaveRepository : OppgaveRepository {
    private val oppgaver = mutableListOf<Oppgave>()

    init {
        SøknadInnsendtHendelse(UUID.randomUUID(), "598137911", "12345678910").let {
            oppgaver.add(
                it.oppgave(
                    Person("12345678910").also { person ->
                        person.håndter(it)
                    },
                ),
            )
        }

        SøknadInnsendtHendelse(UUID.randomUUID(), "598137911", "10987654321").let {
            oppgaver.add(
                it.oppgave(
                    Person("10987654321").also { person ->
                        person.håndter(it)
                    },
                ),
            )
        }

        SøknadInnsendtHendelse(UUID.randomUUID(), "598137911", "12837798289").let {
            oppgaver.add(
                it.oppgave(
                    Person("12837798289").also { person ->
                        person.håndter(it)
                    },
                ),
            )
        }
    }

    override fun lagreOppgave(oppgave: Oppgave) {
        if (oppgaver.contains(oppgave)) return
        oppgaver.add(oppgave)
    }

    override fun hentOppgave(uuid: UUID) = oppgaver.single { it.uuid == uuid }

    override fun hentOppgaver() = oppgaver

    override fun hentOppgaverFor(fnr: String) = oppgaver.filter { it.person.ident == fnr }
}
