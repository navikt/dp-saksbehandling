package no.nav.dagpenger.behandling

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.application.Application
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.dagpenger.behandling.db.InMemoryPersonRepository
import no.nav.dagpenger.behandling.db.PersonRepository
import no.nav.dagpenger.behandling.hendelser.Paragraf_4_23_alder_Vilkår_resultat
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ApiTest {

    val ident = "12345678901"

    private val inMemoryPersonRepository = InMemoryPersonRepository()
    @BeforeEach
    fun data() {
        val person = Person(ident)
        val søknadHendelse = SøknadHendelse(søknadUUID = UUID.randomUUID(), journalpostId = "123454", ident = ident)
        person.håndter(søknadHendelse)
        val paragraf423AlderResultat = Paragraf_4_23_alder_Vilkår_resultat(
            ident,
            søknadHendelse.behov().first().kontekst()["vilkårsvurderingId"].let { UUID.fromString(it) },
            oppfylt = true
        )
        person.håndter(paragraf423AlderResultat)
        inMemoryPersonRepository.lagrePerson(person)
    }

    @Test
    fun `vis no html da`() {
        testApplication {
            application(mockedApi(inMemoryPersonRepository))

            val response = client.get("behandlinger/$ident")
            println(response.bodyAsText())
        }
    }

    internal fun mockedApi(
        personRepository: PersonRepository = mockk(relaxed = true),
    ): Application.() -> Unit {
        return fun Application.() {
            api(
                personRepository
            )
        }
    }
}
