package no.nav.dagpenger.saksbehandling.hendelser.mottak

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.Meldingsfabrikk.innsendingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.Meldingsfabrikk.testIdent
import no.nav.dagpenger.saksbehandling.hendelser.SøknadInnsendtHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.util.UUID

class SøknadMottakTest {
    @ParameterizedTest
    @ValueSource(strings = ["NySøknad"])
    fun `Skal behandle innsending_ferdigstilt event for type NySøknad`(type: String) {
        val slot = slot<SøknadInnsendtHendelse>()
        val mockMediator =
            mockk<Mediator>().also {
                every { it.behandle(capture(slot)) } just Runs
            }

        TestRapid().let { testRapid ->
            SøknadMottak(
                testRapid,
                mockMediator,
            )
            val søknadId = UUID.randomUUID()
            val journalpostId = "jp1"
            val ident = testIdent
            val innsendtDato = LocalDate.now()

            testRapid.sendTestMessage(
                innsendingFerdigstiltHendelse(
                    søknadId = søknadId,
                    journalpostId = journalpostId,
                    type = type,
                    ident = ident,
                ),
            )

            verify(exactly = 1) { mockMediator.behandle(any<SøknadInnsendtHendelse>()) }

            slot.captured.let { hendelse ->
                hendelse.journalpostId() shouldBe journalpostId
                hendelse.ident() shouldBe ident
                hendelse.søknadId() shouldBe søknadId
                hendelse.innsendtDato shouldBe innsendtDato
            }
        }
    }
}
