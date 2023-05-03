package no.nav.dagpenger.behandling.oppgave

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Meldingsfabrikk.testHendelse
import no.nav.dagpenger.behandling.Meldingsfabrikk.testSporing
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.Steg
import no.nav.dagpenger.behandling.prosess.Arbeidsprosesser
import org.junit.jupiter.api.Test

class OppgaveTest {
    @Test
    fun foobar() {
        val steg = Steg.fastsettelse<String>("foo")
        val behandling = Behandling(Person("02020256789"), testHendelse, setOf(steg))
        val oppgave = Oppgave(
            behandling,
            Arbeidsprosesser.totrinnsprosess(behandling).apply { start("TilBehandling") },
        )

        oppgave.tilstand() shouldBe "TilBehandling"
        oppgave.besvar(steg.uuid, "foob", testSporing)

        oppgave.gåTil("Innstilt")
        oppgave.tilstand() shouldBe "Innstilt"
    }
}
