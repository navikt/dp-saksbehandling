package no.nav.dagpenger.behandling.mottak

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.behandling.Mediator
import no.nav.dagpenger.behandling.Meldingsfabrikk.innsendingFerdigstiltHendelse
import no.nav.dagpenger.behandling.Meldingsfabrikk.testIdent
import no.nav.dagpenger.behandling.modell.hendelser.SøknadInnsendtHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SøknadMottakTest {
    private val testRapid = TestRapid()
    private val slot = slot<SøknadInnsendtHendelse>()
    private val mockMediator =
        mockk<Mediator>().also {
            every { it.behandle(capture(slot)) } just Runs
        }

    init {
        SøknadMottak(
            rapidsConnection = testRapid,
            mediator = mockMediator,
        )
    }

    @Test
    fun `Skal behandle innsending_ferdigstilt event for type NySøknad`() {
        val søknadId = UUID.randomUUID()
        val journalpostId = "jp1"
        val ident = testIdent
        val innsendtDato = LocalDate.now()

        testRapid.sendTestMessage(
            innsendingFerdigstiltHendelse(
                søknadId = søknadId,
                journalpostId = journalpostId,
                type = "NySøknad",
                ident = ident,
            ),
        )

        verify(exactly = 1) { mockMediator.behandle(any<SøknadInnsendtHendelse>()) }

        slot.captured.let { hendelse ->
            hendelse.journalpostId shouldBe journalpostId
            hendelse.ident shouldBe ident
            hendelse.søknadId shouldBe søknadId
            hendelse.innsendtDato shouldBe innsendtDato
        }
    }
}
