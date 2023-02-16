package no.nav.dagpenger.behandling.cucumber

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.datatable.DataTable
import io.cucumber.java8.No
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.hendelser.BeslutterHendelse
import no.nav.dagpenger.behandling.hendelser.GrunnlagOgSatsResultat
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.StønadsperiodeResultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.mengde.Enhet.Companion.arbeidsuker
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering
import no.nav.dagpenger.behandling.visitor.PersonVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class Stepdefs : No {

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        val datoformatterer = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }

    private lateinit var person: Person
    private lateinit var ident: String
    private val inspektør get() = Inspektør(person)

    init {
        Gitt("^en ny søknad$") { søknadHendelse: SøknadHendelseCucumber ->
            ident = søknadHendelse.fødselsnummer
            person = Person(ident)
            person.håndter(SøknadHendelse(UUID.randomUUID(), "journalpostId", ident))
        }
        Og("^alle inngangsvilkår er oppfylt$") {
            håndterInngangsvilkår(true)
        }
        Og("^sats er (\\d+), grunnlag er (\\d+) og stønadsperiode er (\\d+)$") { sats: Int, grunnlag: Int, stønadsperiode: Int ->
            håndterSatsogGrunnlag(sats, grunnlag)
            håndterStønadsperiode(stønadsperiode)
        }
        Og("^beslutter kvalitetssikrer$") {
            håndterBeslutterHendelse()
        }
        Så("^skal bruker ha (\\d+) vedtak$") { antallVedtak: Int ->
            assertEquals(antallVedtak, inspektør.antallVedtak)
        }
        Når("^rapporteringshendelse mottas$") { rapporteringsHendelse: DataTable ->

        }

        Så("^skal forbruket være (\\d+)$") { arg0: Int? -> }
    }

    private fun håndterBeslutterHendelse() {
        person.håndter(BeslutterHendelse("123", ident, inspektør.behandlingsId))
    }

    private data class SøknadHendelseCucumber(val fødselsnummer: String, val behandlingId: String)

    private fun håndterInngangsvilkår(oppfylt: Boolean) {
        person.håndter(Paragraf_4_23_alder_Vilkår_resultat(ident, inspektør.vilkårsvurderingId, oppfylt))
    }

    private fun håndterStønadsperiode(stønadsperiode: Int) {
        person.håndter(StønadsperiodeResultat(ident, inspektør.behandlingsId, stønadsperiode.arbeidsuker))
    }

    private fun håndterSatsogGrunnlag(sats: Int, grunnlag: Int) {
        person.håndter(GrunnlagOgSatsResultat(ident, inspektør.behandlingsId, sats.toBigDecimal(), grunnlag.toBigDecimal()))
    }

    private class Inspektør(person: Person) : PersonVisitor {
        init {
            person.accept(this)
        }

        lateinit var vilkårsvurderingId: UUID
        lateinit var behandlingsId: UUID
        var antallVedtak = 0

        override fun <Paragraf : Vilkårsvurdering<Paragraf>> visitVilkårsvurdering(
            vilkårsvurderingId: UUID,
            tilstand: Vilkårsvurdering.Tilstand<Paragraf>
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
            utfall: Boolean
        ) {
            antallVedtak++
        }
    }

    init {
        DefaultParameterTransformer { fromValue: String?, toValueType: Type? ->
            objectMapper.convertValue(
                fromValue,
                objectMapper.constructType(toValueType)
            )
        }
        DefaultDataTableCellTransformer { fromValue: String?, toValueType: Type? ->
            objectMapper.convertValue(
                fromValue,
                objectMapper.constructType(toValueType)
            )
        }
        DefaultDataTableEntryTransformer { fromValue: Map<String?, String?>?, toValueType: Type? ->
            objectMapper.convertValue(
                fromValue,
                objectMapper.constructType(toValueType)
            )
        }
    }
}
