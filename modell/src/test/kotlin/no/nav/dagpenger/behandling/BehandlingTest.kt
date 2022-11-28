package no.nav.dagpenger.behandling

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class BehandlingTest {
    @Test
    fun `ny søknad hendelse trigger en behandling`() {
        val ident = "12345678901"
        val person = Person(ident)
        person.håndter(SøknadHendelse(UUID.randomUUID(), ident))
        assertTrue(person.harSaker())
    }
}
