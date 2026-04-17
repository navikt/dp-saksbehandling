package no.nav.dagpenger.saksbehandling.oppfolging

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.hendelser.OpprettOppfølgingHendelse
import org.junit.jupiter.api.Test

class OpprettOppgaveMottakTest {
    private val testRapid = TestRapid()
    private val oppfølgingMediator = mockk<OppfølgingMediator>(relaxed = true)
    private val ident = "12345678901"

    init {
        OpprettOppgaveMottak(
            rapidsConnection = testRapid,
            oppfølgingMediator = oppfølgingMediator,
        )
    }

    @Test
    fun `Skal parse og delegere opprett_oppgave hendelse til oppfølgingMediator`() {
        testRapid.sendTestMessage(
            opprettOppgaveMelding(
                emneknagg = "MeldekortKorrigering",
                tittel = "Meldekort trenger korrigering",
                beskrivelse = "Meldekortet for perioden 01.03-14.03 må gjennomgås",
            ),
        )

        val hendelseSlot = slot<OpprettOppfølgingHendelse>()
        verify(exactly = 1) { oppfølgingMediator.taImot(capture(hendelseSlot)) }

        val hendelse = hendelseSlot.captured
        hendelse.ident shouldBe ident
        hendelse.aarsak shouldBe "MeldekortKorrigering"
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
              "emneknagg": "PDLFlytting",
              "tittel": "Person har flyttet",
              "strukturertData": {
                "fraAdresse": "Oslo",
                "tilAdresse": "Bergen"
              }
            }
            """.trimIndent(),
        )

        val hendelseSlot = slot<OpprettOppfølgingHendelse>()
        verify(exactly = 1) { oppfølgingMediator.taImot(capture(hendelseSlot)) }

        val hendelse = hendelseSlot.captured
        hendelse.aarsak shouldBe "PDLFlytting"
        hendelse.strukturertData shouldNotBe emptyMap<String, Any>()
        hendelse.strukturertData["fraAdresse"] shouldBe "Oslo"
    }

    @Test
    fun `Skal parse oppgave uten valgfrie felter`() {
        testRapid.sendTestMessage(
            //language=json
            """
            {
              "@event_name": "opprett_oppgave",
              "ident": "$ident",
              "emneknagg": "EnkelOppgave",
              "tittel": "En enkel oppgave"
            }
            """.trimIndent(),
        )

        val hendelseSlot = slot<OpprettOppfølgingHendelse>()
        verify(exactly = 1) { oppfølgingMediator.taImot(capture(hendelseSlot)) }

        val hendelse = hendelseSlot.captured
        hendelse.beskrivelse shouldBe ""
        hendelse.strukturertData shouldBe emptyMap()
    }

    @Test
    fun `Skal ignorere meldinger som ikke er opprett_oppgave`() {
        testRapid.sendTestMessage(
            //language=json
            """
            {
              "@event_name": "noe_annet",
              "ident": "$ident",
              "emneknagg": "Test",
              "tittel": "Test"
            }
            """.trimIndent(),
        )

        verify(exactly = 0) { oppfølgingMediator.taImot(any()) }
    }

    //language=json
    private fun opprettOppgaveMelding(
        emneknagg: String,
        tittel: String,
        beskrivelse: String? = null,
    ) = """
        {
          "@event_name": "opprett_oppgave",
          "ident": "$ident",
          "emneknagg": "$emneknagg",
          "tittel": "$tittel"
          ${beskrivelse?.let { ""","beskrivelse": "$it"""" } ?: ""}
        }
        """.trimIndent()
}
