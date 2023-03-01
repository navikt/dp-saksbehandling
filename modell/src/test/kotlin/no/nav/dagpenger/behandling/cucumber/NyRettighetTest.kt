package no.nav.dagpenger.behandling.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java8.No
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.hendelser.BeslutterHendelse
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.InngangsvilkårResultat
import no.nav.dagpenger.behandling.hendelser.RapporteringsHendelse
import no.nav.dagpenger.behandling.hendelser.Rapporteringsdag
import no.nav.dagpenger.behandling.hendelser.StønadsperiodeResultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.mengde.Enhet.Companion.arbeidsdager
import no.nav.dagpenger.behandling.mengde.Enhet.Companion.arbeidsuker
import no.nav.dagpenger.behandling.mengde.Tid
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering
import no.nav.dagpenger.behandling.visitor.PersonVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class NyRettighetTest : No {
    private val datoformatterer = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private lateinit var person: Person
    private lateinit var ident: String
    private val inspektør get() = Inspektør(person)

    init {
        Gitt("en ny søknad") { søknadHendelse: SøknadHendelseCucumber ->
            ident = søknadHendelse.fødselsnummer
            person = Person(ident)
            person.håndter(SøknadHendelse(UUID.randomUUID(), "journalpostId", ident))
        }

        Og("alle inngangsvilkår er {string} med virkningsdato {string}") { oppfylt: String, virkningsdato: String ->
            håndterInngangsvilkår(oppfylt == "oppfylt", virkningsdato = LocalDate.parse(virkningsdato, datoformatterer))
        }

        Og("sats er {bigdecimal}, grunnlag er {bigdecimal} og stønadsperiode er {int}") { sats: BigDecimal, grunnlag: BigDecimal, stønadsperiode: Int ->
            håndterSatsogGrunnlag(sats, grunnlag)
            håndterStønadsperiode(stønadsperiode)
        }
        Og("beslutter kvalitetssikrer") {
            håndterBeslutterHendelse()
        }
        Så("skal bruker ha {int} vedtak") { antallVedtak: Int ->
            assertEquals(antallVedtak, inspektør.antallVedtak)
        }
        Når("rapporteringshendelse mottas") { rapporteringsHendelse: DataTable ->
            val rapporteringsdager = rapporteringsHendelse.rows(1).asLists(String::class.java).map {
                Rapporteringsdag(dato = LocalDate.parse(it[0], datoformatterer), fravær = it[1].toBooleanStrict(), timer = it[2].toDouble())
            }
            håndterRapporteringsHendelse(rapporteringsdager)
        }
        Så("skal forbruket være {int}") { forbruk: Int ->
            assertEquals(forbruk.arbeidsdager, inspektør.forbruk)
        }
    }

    private fun håndterRapporteringsHendelse(rapporteringsdager: List<Rapporteringsdag>) {
        person.håndter(RapporteringsHendelse(ident, UUID.randomUUID(), rapporteringsdager))
    }

    private fun håndterBeslutterHendelse() {
        person.håndter(BeslutterHendelse("123", ident, inspektør.behandlingsId))
    }

    private data class SøknadHendelseCucumber(val fødselsnummer: String, val behandlingId: String)

    private fun håndterInngangsvilkår(oppfylt: Boolean, virkningsdato: LocalDate) {
        person.håndter(InngangsvilkårResultat(ident, inspektør.vilkårsvurderingId, oppfylt, virkningsdato))
    }

    private fun håndterStønadsperiode(stønadsperiode: Int) {
        person.håndter(StønadsperiodeResultat(ident, inspektør.behandlingsId, stønadsperiode.arbeidsuker))
    }

    private fun håndterSatsogGrunnlag(sats: BigDecimal, grunnlag: BigDecimal) {
        person.håndter(
            GrunnlagOgSatsResultat(
                ident,
                inspektør.behandlingsId,
                sats,
                grunnlag
            )
        )
    }

    private class Inspektør(person: Person) : PersonVisitor {
        init {
            person.accept(this)
        }

        lateinit var vilkårsvurderingId: UUID
        lateinit var behandlingsId: UUID
        var antallVedtak = 0
        lateinit var forbruk: Tid

        override fun <Paragraf : Vilkårsvurdering<Paragraf>> visitVilkårsvurdering(
            vilkårsvurderingId: UUID,
            tilstand: Vilkårsvurdering.Tilstand<Paragraf>,
        ) {
            this.vilkårsvurderingId = vilkårsvurderingId
        }

        override fun preVisit(behandlingsId: UUID, hendelseId: UUID) {
            this.behandlingsId = behandlingsId
        }

        override fun postVisitVedtak(
            vedtakId: UUID,
            virkningsdato: LocalDate,
            vedtakstidspunkt: LocalDateTime,
            utfall: Boolean,
        ) {
            antallVedtak++
        }

        override fun visitForbruk(forbruk: Tid) {
            this.forbruk = forbruk
        }
    }
}
