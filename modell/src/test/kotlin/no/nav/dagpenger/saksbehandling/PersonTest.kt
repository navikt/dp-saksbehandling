package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import org.junit.jupiter.api.Test

class PersonTest {
    @Test
    fun `BehandlingOpprettetHendelse skal føre til at person opprettes med en behandling`() {
        val ident = "12345612345"
        val person = Person(ident)
        val behandlingOpprettetHendelse = BehandlingOpprettetHendelse(søknadId = UUIDv7.ny(), behandlingId = UUIDv7.ny(), ident = ident)
        person.håndter(behandlingOpprettetHendelse)
        person.behandlinger.size shouldBe 1
    }
}
