package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.GrunnlagsBehov
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.KvalitetssikringsBehov
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Paragraf_4_23_alder
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.SatsBehov
import no.nav.dagpenger.behandling.NyRettighetsbehandling.Fastsetter
import no.nav.dagpenger.behandling.NyRettighetsbehandling.Kvalitetssikrer
import no.nav.dagpenger.behandling.NyRettighetsbehandling.VurdererVilkår
import no.nav.dagpenger.behandling.hendelser.BeslutterHendelse
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.hendelser.Rapporteringsdag
import no.nav.dagpenger.behandling.hendelser.StønadsperiodeResultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.mengde.Enhet.Companion.arbeidsuker
import no.nav.dagpenger.behandling.mengde.Stønadsperiode
import no.nav.dagpenger.behandling.visitor.PersonVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class NyRettighetsbehandlingTest {
    val ident = "12345678901"
    private val testObserver = TestObserver()
    val person = Person(ident).also { it.addObserver(testObserver) }
    val søknadHendelse = SøknadHendelse(søknadUUID = UUID.randomUUID(), journalpostId = "123454", ident = ident)

    private val inspektør get() = Inspektør(person)

    @Test
    fun `Ny søknad hendelse fører til innvilgelsesvedtak`() {
        person.håndter(søknadHendelse)
        assertTilstand(Behandling.Tilstand.Type.VurdererVilkår)

        assertEquals(1, søknadHendelse.behov().size, "Forventer kun vilkårsvurderingsbehov")

        val vilkårsvurderingBehov = søknadHendelse.behov().first()
        assertBehovInnholdFor(vilkårsvurderingBehov)

        assertEquals(1, inspektør.antallBehandlinger)

        val vilkårsvurderingId = vilkårsvurderingBehov.kontekst()["vilkårsvurderingId"]
        val paragraf423AlderResultat = Paragraf_4_23_alder_Vilkår_resultat(
            ident,
            UUID.fromString(vilkårsvurderingId),
            oppfylt = true
        )
        person.håndter(paragraf423AlderResultat)
        assertTilstand(Behandling.Tilstand.Type.Fastsetter)
        assertEquals(3, paragraf423AlderResultat.behov().size)

        val grunnlagBehov = paragraf423AlderResultat.behov()[0]
        assertBehovInnholdFor(grunnlagBehov)

        val satsBehov = paragraf423AlderResultat.behov()[1]
        assertBehovInnholdFor(satsBehov)

        val stønadsperiodeBehov = paragraf423AlderResultat.behov()[2]
        assertBehovInnholdFor(stønadsperiodeBehov)

        val behandlingsId = UUID.fromString(vilkårsvurderingBehov.kontekst()["behandlingsId"])
        val grunnlagOgSats = GrunnlagOgSatsResultat(ident, behandlingsId, 250000.toBigDecimal(), 700.toBigDecimal())
        person.håndter(grunnlagOgSats)

        val stønadsperiode = StønadsperiodeResultat(ident, behandlingsId, 52.arbeidsuker)
        person.håndter(stønadsperiode)
        assertBehovInnholdFor(stønadsperiode.behov()[0])

        assertTilstand(Behandling.Tilstand.Type.Kvalitetssikrer)

        person.håndter(
            BeslutterHendelse(
                beslutterIdent = "L11111",
                ident = ident,
                behandlingsId = behandlingsId
            )
        )

        assertTilstand(Behandling.Tilstand.Type.Behandlet)

        assertEquals(true, inspektør.vedtakUtfall)
        assertEquals(250000.toBigDecimal(), inspektør.grunnlag)
        assertEquals(700.toBigDecimal(), inspektør.dagsats)
        assertEquals(52.arbeidsuker, inspektør.stønadsperiode)
        assertEquals(1, testObserver.vedtakFattet.size)

        person.håndter(
            RapporteringsHendelse(
                ident,
                UUID.randomUUID(),
                listOf(
                    Rapporteringsdag(LocalDate.now().plusDays(1), false),
                    Rapporteringsdag(LocalDate.now().plusDays(2), false)
                )
            )
        )

        assertEquals(2, inspektør.antallBehandlinger)
        assertEquals(2, testObserver.vedtakFattet.size)
    }

    @Test
    fun `Ny søknad hendelse blir manuelt behandlet og fører til avslagsvedtak`() {
        person.håndter(søknadHendelse)
        assertTilstand(Behandling.Tilstand.Type.VurdererVilkår)
        assertEquals(1, søknadHendelse.behov().size)
        val vilkårsvurderingBehov = søknadHendelse.behov().first()
        assertBehovInnholdFor(vilkårsvurderingBehov)

        assertEquals(1, inspektør.antallBehandlinger)

        val vilkårsvurderingId = vilkårsvurderingBehov.kontekst()["vilkårsvurderingId"]
        val paragraf423AlderResultat = Paragraf_4_23_alder_Vilkår_resultat(
            ident,
            UUID.fromString(vilkårsvurderingId),
            oppfylt = false,
        )
        person.håndter(paragraf423AlderResultat)

        assertEquals(1, inspektør.antallBehandlinger)
        assertTilstand(Behandling.Tilstand.Type.Kvalitetssikrer)

        val kvalitetssikring = paragraf423AlderResultat.behov()[0]
        assertBehovInnholdFor(kvalitetssikring)

        val behandlingsId = UUID.fromString(vilkårsvurderingBehov.kontekst()["behandlingsId"])
        person.håndter(BeslutterHendelse(beslutterIdent = "12345123451", ident, behandlingsId))
        assertTilstand(Behandling.Tilstand.Type.Behandlet)

        assertEquals(false, inspektør.vedtakUtfall)
        assertEquals(1, testObserver.vedtakFattet.size)
    }

    @Test
    fun `En søknadhendelse skal bare behandles en gang`() {
        person.håndter(søknadHendelse)
        person.håndter(søknadHendelse)

        assertEquals(1, inspektør.antallBehandlinger)
    }

    @Test
    fun `Håndtere to unike søknadhendelser`() {
        val søknadHendelse2 = SøknadHendelse(UUID.randomUUID(), "1243", ident)
        person.håndter(søknadHendelse)
        person.håndter(søknadHendelse2)
        assertEquals(2, inspektør.antallBehandlinger)
    }

    private fun assertBehovInnholdFor(behov: Aktivitetslogg.Aktivitet.Behov) =
        when (behov.type) {
            Paragraf_4_23_alder -> assertAldersbehovInnhold(behov)
            GrunnlagsBehov -> assertGrunnlagbehovInnhold(behov)
            SatsBehov -> assertSatsbehovInnhold(behov)
            KvalitetssikringsBehov -> assertKvalitetssikringInnhold(behov)
            Aktivitetslogg.Aktivitet.Behov.Behovtype.StønadsperiodeBehov -> assertStønadsperiodeInnhold(behov)
        }

    private fun assertStønadsperiodeInnhold(behov: Aktivitetslogg.Aktivitet.Behov) {
        assertEquals(Aktivitetslogg.Aktivitet.Behov.Behovtype.StønadsperiodeBehov, behov.type)
        assertEquals(ident, behov.kontekst()["ident"])
        assertNotNull(behov.kontekst()["behandlingsId"])
        assertNotNull(behov.detaljer()["virkningsdato"].let { LocalDate.parse(it.toString()) })
        assertNotNull(behov.detaljer()["inntektsId"])
    }

    private fun assertKvalitetssikringInnhold(behov: Aktivitetslogg.Aktivitet.Behov) {
        assertEquals(KvalitetssikringsBehov, behov.type)
        assertEquals(ident, behov.kontekst()["ident"])
        assertNotNull(behov.kontekst()["behandlingsId"])
    }

    private fun assertGrunnlagbehovInnhold(behov: Aktivitetslogg.Aktivitet.Behov) {
        assertEquals(GrunnlagsBehov, behov.type)
        assertEquals(ident, behov.kontekst()["ident"])
        assertNotNull(behov.kontekst()["behandlingsId"])
        assertNotNull(behov.detaljer()["virkningsdato"].let { LocalDate.parse(it.toString()) })
    }

    private fun assertSatsbehovInnhold(behov: Aktivitetslogg.Aktivitet.Behov) {
        assertEquals(SatsBehov, behov.type)
        assertEquals(ident, behov.kontekst()["ident"])
        assertNotNull(behov.kontekst()["behandlingsId"])
    }

    private fun assertAldersbehovInnhold(vilkårsvurderingBehov: Aktivitetslogg.Aktivitet.Behov) {
        assertEquals(ident, vilkårsvurderingBehov.kontekst()["ident"])
        assertNotNull(vilkårsvurderingBehov.kontekst()["behandlingsId"])
        assertNotNull(vilkårsvurderingBehov.kontekst()["søknad_uuid"])
        val vilkårsvurderingId = vilkårsvurderingBehov.kontekst()["vilkårsvurderingId"]
        assertDoesNotThrow {
            UUID.fromString(vilkårsvurderingId)
        }
        assertNotNull(vilkårsvurderingId)
    }

    private fun assertTilstand(tilstand: Behandling.Tilstand.Type) {
        assertEquals(tilstand, inspektør.behandlingsTilstand)
    }

    private class Inspektør(person: Person) : PersonVisitor {

        init {
            person.accept(this)
        }

        var grunnlag: BigDecimal? = null
        var dagsats: BigDecimal? = null
        var stønadsperiode: Stønadsperiode? = null
        var antallBehandlinger = 0
        var vedtakUtfall: Boolean? = null
        lateinit var behandlingsTilstand: Behandling.Tilstand.Type

        override fun preVisit(behandlingsId: UUID, hendelseId: UUID) {
            antallBehandlinger++
        }

        override fun visitTilstand(tilstand: Behandling.Tilstand.Type) {
            this.behandlingsTilstand = tilstand
        }

        override fun preVisitVedtak(
            vedtakId: UUID,
            virkningsdato: LocalDate,
            vedtakstidspunkt: LocalDateTime,
            utfall: Boolean
        ) {
            this.vedtakUtfall = utfall
        }

        override fun visitVedtakGrunnlag(grunnlag: BigDecimal) {
            this.grunnlag = grunnlag
        }

        override fun visitVedtakDagsats(dagsats: BigDecimal) {
            this.dagsats = dagsats
        }

        override fun visitVedtakStønadsperiode(stønadsperiode: Stønadsperiode) {
            this.stønadsperiode = stønadsperiode
        }
    }

    private class TestObserver() : PersonObserver {
        val vedtakFattet = mutableListOf<PersonObserver.VedtakFattet>()
        override fun vedtakFattet(vedtakFattet: PersonObserver.VedtakFattet) {
            this.vedtakFattet.add(vedtakFattet)
        }
    }
}
