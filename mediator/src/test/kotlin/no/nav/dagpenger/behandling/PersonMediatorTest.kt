package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PersonMediatorTest {

    private val testIdent = "1234567891"
    private val testRapid = TestRapid()
    val personMediator = PersonMediator(testRapid)

    @Test
    fun `Motta søknadhendelse`() {
        personMediator.behandle(
            SøknadHendelse(
                søknadUUID = UUID.randomUUID(),
                journalpostId = "12345",
                ident = testIdent
            )
        )
    }
}
