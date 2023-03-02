package no.nav.dagpenger.behandling.hendelser.mottak

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.dagpenger.behandling.Meldingsfabrikk.dagpengerrettighetResultat
import no.nav.dagpenger.behandling.PersonMediator
import no.nav.dagpenger.behandling.hendelser.InngangsvilkårResultat
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InngangsvilkårBehovLøsningMottakTest {
    private val slot = slot<InngangsvilkårResultat>()
    private val mediatorMock = mockk<PersonMediator>().also {
        every { it.behandle(capture(slot)) } just Runs
    }
    private val testRapid = TestRapid().also {
        InngangsvilkårBehovLøsningMottak(it, mediatorMock)
    }

    @BeforeEach
    fun setUp() {
        testRapid.reset()
        slot.clear()
    }

    @Test
    fun `skal lese løsning på aldersbehov `() {
        testRapid.sendTestMessage(dagpengerrettighetResultat(vilkårsvurderingId = "a9586759-b71b-4295-a077-89a86453b020"))
        assertTrue(slot.isCaptured)
        val paragraf423AlderLøsning = slot.captured
        assertEquals(
            "a9586759-b71b-4295-a077-89a86453b020",
            paragraf423AlderLøsning.vilkårsvurderingId().toString(),
        )
        assertEquals(
            "12345678901",
            paragraf423AlderLøsning.ident(),
        )
    }

    @Test
    fun `plukker ikke opp ukjente vilkårsresultater`() {
        testRapid.sendTestMessage(dagpengerrettighetResultat(vilkårsvurderingId = "a9586759-b71b-4295-a077-89a86453b020", versjonNavn = "Noe tull"))
        assertFalse(slot.isCaptured)
    }
}
