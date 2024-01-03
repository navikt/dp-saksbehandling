package no.nav.dagpenger.behandling.db

import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.behandling.oppgave.Oppgave
import java.time.LocalDate
import java.util.UUID

class InMemoryOppgaveRepository : OppgaveRepository {
    private val oppgaver = mutableListOf<Oppgave>()

    init {
        SøknadInnsendtHendelse(
            søknadId = UUID.randomUUID(),
            journalpostId = "598137911",
            ident = "12345678910",
            innsendtDato = LocalDate.now(),
        ).let {
            oppgaver.add(
                it.oppgave(
                    Person("12345678910").also { person ->
                        person.håndter(it)
                    },
                ),
            )
        }

        SøknadInnsendtHendelse(
            søknadId = UUID.randomUUID(),
            journalpostId = "598137911",
            ident = "10987654321",
            innsendtDato = LocalDate.now(),
        ).let {
            oppgaver.add(
                it.oppgave(
                    Person("10987654321").also { person ->
                        person.håndter(it)
                    },
                ),
            )
        }

        SøknadInnsendtHendelse(
            søknadId = UUID.randomUUID(),
            journalpostId = "598137911",
            ident = "12837798289",
            innsendtDato = LocalDate.now(),
        ).let {
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
