package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.mottak.BehandlingOpprettetMottak
import no.nav.dagpenger.saksbehandling.mottak.VerifiserOpplysningMottak
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class MediatorTest {
    private val testIdent = "12345612345"
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()

    private val testRapid = TestRapid()
    private val personRepository = InMemoryPersonRepository()
    private val mediator = Mediator(personRepository)

    init {
        BehandlingOpprettetMottak(testRapid, mediator)
        VerifiserOpplysningMottak(testRapid, mediator)
    }

    @AfterEach
    fun tearDown() {
        personRepository.slettAlt()
    }

    @Test
    fun `Lagre ny behandling`() {
        testRapid.sendTestMessage(behandlingOpprettetMelding(testIdent))
        val person = personRepository.hent(testIdent)
        requireNotNull(person)
        person.ident shouldBe testIdent
        person.behandlinger.size shouldBe 1
    }

    @Test
    fun `Opprett oppgave når saksbehandler skal verifisere opplysninger`() {
        testRapid.sendTestMessage(behandlingOpprettetMelding(testIdent))
        testRapid.sendTestMessage(verifiserOpplysningMelding(testIdent))
        val person = personRepository.hent(testIdent)
        requireNotNull(person)
        person.ident shouldBe testIdent
        person.behandlinger.size shouldBe 1
        person.behandlinger.get(behandlingId)!!.oppgave shouldNotBe null
    }

    @Language("JSON")
    private fun behandlingOpprettetMelding(ident: String) =
        """
        {
            "@event_name": "behandling_opprettet",
            "@opprettet": "2024-01-30T10:43:32.988331190",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "søknadId": "$søknadId",
            "behandlingId": "$behandlingId",
            "ident": "$ident"
        }
        """

    @Language("JSON")
    private fun verifiserOpplysningMelding(ident: String) =
        """
        {
            "@event_name": "verifiser_opplysning",
            "@opprettet": "2024-01-30T10:43:32.988331190",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "behandlingId": "$behandlingId",
            "ident": "$ident"
        }
        """
}
