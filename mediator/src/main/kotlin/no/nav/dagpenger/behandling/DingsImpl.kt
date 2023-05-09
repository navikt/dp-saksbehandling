package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.dagpenger.behandling.prosess.Arbeidsprosess

class DingsImpl : Dings {
    override fun oppgaver(hendelse: Hendelse): List<Oppgave> {
        return when (hendelse) {
            is SøknadInnsendtHendelse -> listOf(
                hendelse.oppgave(),
                Oppgave(
                    Behandling(
                        person = Person(hendelse.ident()),
                        hendelse = hendelse,
                        steg = emptySet(),
                    ),
                    Arbeidsprosess(),
                ),
            )

            else -> throw IllegalArgumentException()
        }
    }
}
