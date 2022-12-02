package no.nav.dagpenger.behandling.hendelser.mottak

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.dagpenger.behandling.Meldingsfabrikk.`innsending ferdigstilt hendelse`
import no.nav.dagpenger.behandling.PersonMediator
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class SøknadMottakTest {
    @ParameterizedTest
    @ValueSource(strings = ["NySøknad"])
    fun `Skal behandle innsending_ferdigstilt event for type NySøknad`(type: String) {
        TestRapid().let { testRapid ->
            val slot = slot<SøknadHendelse>()
            SøknadMottak(
                testRapid,
                mockk<PersonMediator>().also {
                    every { it.behandle(capture(slot)) } just Runs
                }
            )
            val søknadId = UUID.randomUUID()
            val journalpostId = "jp1"
            val ident = "ident1"

            testRapid.sendTestMessage(
                `innsending ferdigstilt hendelse`(
                    søknadId = søknadId,
                    journalpostId = journalpostId,
                    type = type,
                    ident = ident
                )
            )
            assertTrue(slot.isCaptured)
            with(slot.captured) {
                assertEquals(this.journalpostId(), journalpostId)
                assertEquals(this.ident(), ident)
                assertEquals(this.søknadUUID(), søknadId)
            }
        }
    }
}
