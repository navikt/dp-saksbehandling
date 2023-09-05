package no.nav.dagpenger.behandling.oppgave

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Meldingsfabrikk.testHendelse
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.Sak
import no.nav.dagpenger.behandling.Steg
import no.nav.dagpenger.behandling.UtførStegKommando
import no.nav.dagpenger.behandling.oppgave.OppgaveTilstand.FerdigBehandlet
import no.nav.dagpenger.behandling.oppgave.OppgaveTilstand.TilBehandling
import org.junit.jupiter.api.Test

class OppgaveTest {
    @Test
    fun foobar() {
        val steg = Steg.fastsettelse<String>("foo")
        val behandling = Behandling(Person("02020256789"), testHendelse, setOf(steg), sak = Sak())
        val oppgave = Oppgave(
            behandling,
        )

        oppgave.tilstand shouldBe TilBehandling
        oppgave.utfør(
            UtførStegKommando(oppgave.uuid, Saksbehandler("X12345"), "") {
                besvar(steg.uuid, "foob", it)
            },
        )
        oppgave.tilstand shouldBe FerdigBehandlet
    }
}
