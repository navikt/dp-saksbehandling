package no.nav.dagpenger.behandling.cucumber

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.datatable.DataTable
import io.cucumber.java8.No
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering
import no.nav.dagpenger.behandling.visitor.PersonVisitor
import java.lang.reflect.Type
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
            val paragraf423AlderVilkårResultat = Paragraf_4_23_alder_Vilkår_resultat(ident, inspektør.vilkårsvurderingId, true)
            person.håndter(paragraf423AlderVilkårResultat)
        }
        Og("^sats er (\\d+), grunnlag er (\\d+) og stønadsperiode er (\\d+)$") { arg0: Int?, arg1: Int?, arg2: Int? -> }
        Så("^skal bruker ha (\\d+) vedtak$") { arg0: Int? -> }
        Når("^rapporteringshendelse mottas$") { rapporteringsHendelse: DataTable ->
        }

        Så("^skal forbruket være (\\d+)$") { arg0: Int? -> }
    }

    private data class SøknadHendelseCucumber(val fødselsnummer: String, val behandlingId: String)

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

    private class Inspektør(person: Person) : PersonVisitor {
        init {
            person.accept(this)
        }

        lateinit var vilkårsvurderingId: UUID

        override fun <Paragraf : Vilkårsvurdering<Paragraf>> visitVilkårsvurdering(
            vilkårsvurderingId: UUID,
            tilstand: Vilkårsvurdering.Tilstand<Paragraf>
        ) {
            this.vilkårsvurderingId = vilkårsvurderingId
        }
    }
}
