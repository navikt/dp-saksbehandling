package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Grunnlag
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sats
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakAvslått
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakInnvilget
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
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
        assertNotNull(vilkårsvurderingBehov.kontekst()["behandlingsId"])
        val behandlingsId = UUID.fromString(vilkårsvurderingBehov.kontekst()["behandlingsId"])
        assertNotNull(vilkårsvurderingBehov.kontekst()["søknad_uuid"])
        val vilkårsvurderingId = vilkårsvurderingBehov.kontekst()["vilkårsvurderingId"]
        assertDoesNotThrow {
            UUID.fromString(vilkårsvurderingId)
        }
        assertNotNull(vilkårsvurderingId)
        assertTrue(person.harBehandlinger())

        val paragraf423AlderResultat = Paragraf_4_23_alder_Vilkår_resultat(
            ident,
            UUID.fromString(vilkårsvurderingId),
            oppfylt = true
        )
        person.håndter(paragraf423AlderResultat)
        assertEquals(3, paragraf423AlderResultat.behov().size)

        val grunnlag = paragraf423AlderResultat.behov()[0]
        val sats = paragraf423AlderResultat.behov()[1]

        assertEquals(Grunnlag, grunnlag.type)
        assertEquals(ident, grunnlag.kontekst()["ident"])
        assertNotNull(grunnlag.kontekst()["behandlingsId"])
        assertNotNull(grunnlag.detaljer()["virkningsdato"].let { LocalDate.parse(it.toString()) })

        assertEquals(Sats, sats.type)
        assertEquals(ident, sats.kontekst()["ident"])
        assertNotNull(sats.kontekst()["behandlingsId"])

        val grunnlagOgSatsResultat = GrunnlagOgSatsResultat(ident, behandlingsId, 250000.toBigDecimal(), 700.toBigDecimal())
        person.håndter(grunnlagOgSatsResultat)
        val vedtakInnvilget = grunnlagOgSatsResultat.behov().first()
        assertEquals(VedtakInnvilget, vedtakInnvilget.type)
        assertEquals(ident, vedtakInnvilget.kontekst()["ident"])
        assertNotNull(vedtakInnvilget.kontekst()["behandlingsId"])
    }

    @Test
    fun `Ny søknad hendelse fører til avslagsvedtak`() {
        person.håndter(søknadHendelse)
        assertEquals(1, søknadHendelse.behov().size)
        assertTrue(person.harBehandlinger())

        val vilkårsvurderingBehov = søknadHendelse.behov().first()
        assertEquals(ident, vilkårsvurderingBehov.kontekst()["ident"])
        assertNotNull(vilkårsvurderingBehov.kontekst()["behandlingsId"])
        val vilkårsvurderingId = vilkårsvurderingBehov.kontekst()["vilkårsvurderingId"]
        assertDoesNotThrow {
            UUID.fromString(vilkårsvurderingId)
        }

        val paragraf423AlderResultat = Paragraf_4_23_alder_Vilkår_resultat(
            ident,
            UUID.fromString(vilkårsvurderingId),
            oppfylt = false
        )
        person.håndter(paragraf423AlderResultat)
        assertEquals(1, paragraf423AlderResultat.behov().size)
        val behov = paragraf423AlderResultat.behov().first()
        assertEquals(VedtakAvslått, behov.type)
        assertEquals(ident, behov.kontekst()["ident"])
        assertNotNull(ident, behov.kontekst()["behandlingsId"])
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
        val paragraf423AlderResultat = Paragraf_4_23_alder_Vilkår_resultat(
            ident,
            UUID.fromString(vilkårsvurderingId),
            oppfylt = false
        )
        person.håndter(paragraf423AlderResultat)
        assertEquals(1, paragraf423AlderResultat.behov().size)
    }
}
