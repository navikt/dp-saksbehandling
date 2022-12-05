package no.nav.dagpenger.behandling.hendelser.mottak

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.dagpenger.behandling.Meldingsfabrikk.aldersbehovLøsning
import no.nav.dagpenger.behandling.PersonMediator
import no.nav.dagpenger.behandling.hendelser.AldersvilkårLøsning
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AldersbehovLøsningMottakTest {
    private val slot = slot<AldersvilkårLøsning>()
    private val mediatorMock = mockk<PersonMediator>().also {
        every { it.behandle(capture(slot)) } just Runs
    }
    private val testRapid = TestRapid().also {
        AldersbehovLøsningMottak(it, mediatorMock)
    }

    @Test
    fun `skal lese løsning på aldersbehov `() {
        testRapid.sendTestMessage(aldersbehovLøsning())
        assertTrue(slot.isCaptured)
        val aldersvilkårLøsning = slot.captured
        assertEquals(
            "a9586759-b71b-4295-a077-89a86453b020", aldersvilkårLøsning.behandlingId().toString(),
        )
        assertEquals(
            "12345678901", aldersvilkårLøsning.ident()
        )

        assertTrue(
            aldersvilkårLøsning.oppfylt
        )
    }
}
