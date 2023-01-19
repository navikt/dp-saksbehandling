package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakAvslåttBehov
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakInnvilgetBehov
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.UUID

class BehandlingTest {
    val ident = "12345678901"
    val person = Person(ident)
    val søknadHendelse = SøknadHendelse(søknadUUID = UUID.randomUUID(), journalpostId = "123454", ident = ident)

    @Test
    fun `Ny søknad hendelse fører til innvilgelsesvedtak`() {
        person.håndter(søknadHendelse)
        assertEquals(1, søknadHendelse.behov().size)
        val vilkårsvurderingBehov = søknadHendelse.behov().first()
        assertEquals(ident, vilkårsvurderingBehov.kontekst()["ident"])
        assertNotNull(vilkårsvurderingBehov.kontekst()["behandlingId"])
        val vilkårsvurderingId = vilkårsvurderingBehov.kontekst()["vilkårsvurderingId"]
        assertDoesNotThrow {
            UUID.fromString(vilkårsvurderingId)
        }
        assertNotNull(vilkårsvurderingId)
        assertTrue(person.harBehandlinger())

        val paragraf423AlderResultat = Paragraf_4_23_alder_resultat(
            ident,
            UUID.fromString(vilkårsvurderingId),
            oppfylt = true
        )
        person.håndter(paragraf423AlderResultat)
        assertEquals(1, paragraf423AlderResultat.behov().size)
        val behov = paragraf423AlderResultat.behov().first()

        assertEquals(VedtakInnvilgetBehov, behov.type)
        assertEquals(ident, behov.kontekst()["ident"])
        assertNotNull(behov.kontekst()["behandlingId"])
    }

    @Test
    fun `Ny søknad hendelse fører til avslagsvedtak`() {
        person.håndter(søknadHendelse)
        assertEquals(1, søknadHendelse.behov().size)
        assertTrue(person.harBehandlinger())

        val vilkårsvurderingBehov = søknadHendelse.behov().first()
        assertEquals(ident, vilkårsvurderingBehov.kontekst()["ident"])
        assertNotNull(vilkårsvurderingBehov.kontekst()["behandlingId"])
        val vilkårsvurderingId = vilkårsvurderingBehov.kontekst()["vilkårsvurderingId"]
        assertDoesNotThrow {
            UUID.fromString(vilkårsvurderingId)
        }

        val paragraf423AlderResultat = Paragraf_4_23_alder_resultat(
            ident,
            UUID.fromString(vilkårsvurderingId),
            oppfylt = false
        )
        person.håndter(paragraf423AlderResultat)
        assertEquals(1, paragraf423AlderResultat.behov().size)
        val behov = paragraf423AlderResultat.behov().first()
        assertEquals(VedtakAvslåttBehov, behov.type)
        assertEquals(ident, behov.kontekst()["ident"])
        assertNotNull(ident, behov.kontekst()["behandlingId"])
    }

    @Test
    fun `En søknadhendelse skal bare behandles en gang`() {
        person.håndter(søknadHendelse)
        person.håndter(søknadHendelse)

        assertEquals(1, person.antallBehandlinger())
    }

    @Test
    fun `Håndtere to unike søknadhendelser`() {
        val søknadHendelse2 = SøknadHendelse(UUID.randomUUID(), "1243", ident)

        person.håndter(søknadHendelse)
        person.håndter(søknadHendelse2)
        val vilkårsvurderingBehov = søknadHendelse.behov().first()
        val vilkårsvurderingId = vilkårsvurderingBehov.kontekst()["vilkårsvurderingId"]
        val paragraf423AlderResultat = Paragraf_4_23_alder_resultat(
            ident,
            UUID.fromString(vilkårsvurderingId),
            oppfylt = false
        )
        person.håndter(paragraf423AlderResultat)
        assertEquals(1, paragraf423AlderResultat.behov().size)
    }
}
