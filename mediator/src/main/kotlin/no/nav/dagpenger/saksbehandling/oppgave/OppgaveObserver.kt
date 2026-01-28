package no.nav.dagpenger.saksbehandling.oppgave

import no.nav.dagpenger.saksbehandling.Oppgave

interface OppgaveObserver {
    fun oppgaveEndret(oppgave: Oppgave)
}
