package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.AldersbehovLøsning
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class BehandlingTest {
    @Test
    fun `Ny søknad hendelse fører til innvilgelsesvedtak`() {
        val ident = "12345678901"
        val person = Person(ident)
        val søknadHendelse = SøknadHendelse(UUID.randomUUID(), ident)
        person.håndter(søknadHendelse)
        assertEquals(1, søknadHendelse.behov().size)
        assertTrue(person.harBehandlinger())

        val aldersbehovLøsning = AldersbehovLøsning(ident, oppfylt = true)
        person.håndter(aldersbehovLøsning)
        assertEquals(1, aldersbehovLøsning.behov().size)
        assertTrue(aldersbehovLøsning.behov().first() is VedtakInnvilgetBehov)
    }

    @Test
    fun `Ny søknad hendelse fører til avslagsvedtak`() {
        val ident = "12345678901"
        val person = Person(ident)
        val søknadHendelse = SøknadHendelse(UUID.randomUUID(), ident)
        person.håndter(søknadHendelse)
        assertEquals(1, søknadHendelse.behov().size)
        assertTrue(person.harBehandlinger())

        val aldersbehovLøsning = AldersbehovLøsning(ident, oppfylt = false)
        person.håndter(aldersbehovLøsning)
        assertEquals(1, aldersbehovLøsning.behov().size)
        assertTrue(aldersbehovLøsning.behov().first() is VedtakAvslåttBehov)
    }
}
