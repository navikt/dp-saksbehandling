package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class BehandlingTest {
    @Test
    fun `ny søknad hendelse trigger en behandling`() {
        val ident = "12345678901"
        val person = Person(ident)
        val søknadHendelse = SøknadHendelse(UUID.randomUUID(), ident)
        person.håndter(søknadHendelse)
        assertEquals(1, søknadHendelse.behov().size)
        assertTrue(person.harBehandlinger())
    }
}
