package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Meldingsfabrikk.dagpengerrettighetResultat
import no.nav.dagpenger.behandling.Meldingsfabrikk.`innsending ferdigstilt hendelse`
import no.nav.dagpenger.behandling.db.PersonRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PersonMediatorTest {

    private val testIdent = "12345678901"
    private val testRapid = TestRapid()

    private val inmemoryRepository = object : PersonRepository {
        private val personer = mutableMapOf<String, Person>()
        override fun hentPerson(ident: String): Person? {
            return personer[ident]
        }

        override fun lagrePerson(person: Person) {
            personer[person.ident()] = person
        }
    }
    val personMediator = PersonMediator(testRapid, inmemoryRepository)

    @Test
    fun `Motta søknadhendelse, få aldersbehovløsning, gå til kvalitetssikring`() {
        testRapid.sendTestMessage(
            `innsending ferdigstilt hendelse`(
                søknadId = UUID.randomUUID(),
                journalpostId = "12345",
                ident = testIdent,
                type = "NySøknad",
            ),
        )
        val person: Person? = inmemoryRepository.hentPerson(testIdent)
        assertNotNull(person)
        assertEquals(1, testRapid.inspektør.size)

        testRapid.sendTestMessage(
            dagpengerrettighetResultat(
                ident = testIdent,
                vilkårsvurderingId = testRapid.inspektør.field(0, "vilkårsvurderingId").asText(),
            ),
        )

        // TODO: Rydde
        assertEquals("[\"DagpengerettighetBehov\"]", testRapid.inspektør.message(0)["@behov"].toString())
        assertEquals("[\"KvalitetssikringsBehov\"]", testRapid.inspektør.message(1)["@behov"].toString())
        assertEquals(2, testRapid.inspektør.size)
    }
}
