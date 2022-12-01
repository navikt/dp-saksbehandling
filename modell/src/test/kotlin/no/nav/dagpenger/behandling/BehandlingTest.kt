package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakAvslåttBehov
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakInnvilgetBehov
import no.nav.dagpenger.behandling.hendelser.AldersvilkårLøsning
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

        val aldersvilkårLøsning = AldersvilkårLøsning(ident, oppfylt = true)
        person.håndter(aldersvilkårLøsning)
        assertEquals(1, aldersvilkårLøsning.behov().size)
        assertEquals(VedtakInnvilgetBehov, aldersvilkårLøsning.behov().first().type)
    }

    @Test
    fun `Ny søknad hendelse fører til avslagsvedtak`() {
        val ident = "12345678901"
        val person = Person(ident)
        val søknadHendelse = SøknadHendelse(UUID.randomUUID(), ident)
        person.håndter(søknadHendelse)
        assertEquals(1, søknadHendelse.behov().size)
        assertTrue(person.harBehandlinger())

        val aldersvilkårLøsning = AldersvilkårLøsning(ident, oppfylt = false)
        person.håndter(aldersvilkårLøsning)
        assertEquals(1, aldersvilkårLøsning.behov().size)
        assertEquals(VedtakAvslåttBehov, aldersvilkårLøsning.behov().first().type)
    }
}
