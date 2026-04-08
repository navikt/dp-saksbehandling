package no.nav.dagpenger.saksbehandling.generell

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse
import org.junit.jupiter.api.Test

class OpprettOppgaveMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val ident = "12345678901"

    init {
        OpprettOppgaveMottak(
            rapidsConnection = testRapid,
            oppgaveMediator = oppgaveMediator,
        )
    }

    @Test
    fun `Skal parse og delegere opprett_oppgave hendelse til oppgaveMediator`() {
        testRapid.sendTestMessage(
            opprettOppgaveMelding(
                oppgaveType = "MeldekortKorrigering",
                tittel = "Meldekort trenger korrigering",
                beskrivelse = "Meldekortet for perioden 01.03-14.03 må gjennomgås",
            ),
        )

        val hendelseSlot = slot<OpprettGenerellOppgaveHendelse>()
        verify(exactly = 1) { oppgaveMediator.håndter(capture(hendelseSlot)) }

        val hendelse = hendelseSlot.captured
        hendelse.ident shouldBe ident
        hendelse.oppgaveType shouldBe "MeldekortKorrigering"
        hendelse.tittel shouldBe "Meldekort trenger korrigering"
        hendelse.beskrivelse shouldBe "Meldekortet for perioden 01.03-14.03 må gjennomgås"
    }

    @Test
    fun `Skal parse oppgave med strukturert data`() {
        testRapid.sendTestMessage(
            //language=json
            """
            {
              "@event_name": "opprett_oppgave",
              "ident": "$ident",
              "oppgaveType": "PDLFlytting",
              "tittel": "Person har flyttet",
              "strukturertData": {
                "fraAdresse": "Oslo",
                "tilAdresse": "Bergen"
              }
            }
            """.trimIndent(),
        )

        val hendelseSlot = slot<OpprettGenerellOppgaveHendelse>()
        verify(exactly = 1) { oppgaveMediator.håndter(capture(hendelseSlot)) }

        val hendelse = hendelseSlot.captured
        hendelse.oppgaveType shouldBe "PDLFlytting"
        hendelse.strukturertData shouldNotBe null
        hendelse.strukturertData!!["fraAdresse"].asText() shouldBe "Oslo"
    }

    @Test
    fun `Skal parse oppgave uten valgfrie felter`() {
        testRapid.sendTestMessage(
            //language=json
            """
            {
              "@event_name": "opprett_oppgave",
              "ident": "$ident",
              "oppgaveType": "EnkelOppgave",
              "tittel": "En enkel oppgave"
            }
            """.trimIndent(),
        )

        val hendelseSlot = slot<OpprettGenerellOppgaveHendelse>()
        verify(exactly = 1) { oppgaveMediator.håndter(capture(hendelseSlot)) }

        val hendelse = hendelseSlot.captured
        hendelse.beskrivelse shouldBe null
        hendelse.strukturertData shouldBe null
    }

    @Test
    fun `Skal ignorere meldinger som ikke er opprett_oppgave`() {
        testRapid.sendTestMessage(
            //language=json
            """
            {
              "@event_name": "noe_annet",
              "ident": "$ident",
              "oppgaveType": "Test",
              "tittel": "Test"
            }
            """.trimIndent(),
        )

        verify(exactly = 0) { oppgaveMediator.håndter(any<OpprettGenerellOppgaveHendelse>()) }
    }

    //language=json
    private fun opprettOppgaveMelding(
        oppgaveType: String,
        tittel: String,
        beskrivelse: String? = null,
    ) = """
        {
          "@event_name": "opprett_oppgave",
          "ident": "$ident",
          "oppgaveType": "$oppgaveType",
          "tittel": "$tittel"
          ${beskrivelse?.let { ""","beskrivelse": "$it"""" } ?: ""}
        }
        """.trimIndent()
}
