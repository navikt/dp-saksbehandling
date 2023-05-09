package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.oppgave.Oppgave

interface Dings {
    fun oppgaver(hendelse: Hendelse): List<Oppgave>
}
