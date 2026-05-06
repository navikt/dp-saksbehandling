package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadsavklaringLøsningMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveId = UUIDv7.ny()

    @Test
    fun `Skal legge til emneknagger når alle løsninger er true`() {
        val oppgaveMediator =
            mockk<OppgaveMediator>().also {
                every { it.leggTilEmneknagger(any<UUID>(), any()) } returns Unit
            }
        SøknadsavklaringLøsningMottak(testRapid, oppgaveMediator)

        testRapid.sendTestMessage(
            løsningMelding(
                eøs = true,
                sanksjon = true,
                barnOver16 = true,
            ),
        )

        verify(exactly = 1) {
            oppgaveMediator.leggTilEmneknagger(oppgaveId, setOf("EØS", "Mulig sanksjon", "Barn over 16"))
        }
    }

    @Test
    fun `Skal legge til kun relevante emneknagger`() {
        val oppgaveMediator =
            mockk<OppgaveMediator>().also {
                every { it.leggTilEmneknagger(any<UUID>(), any()) } returns Unit
            }
        SøknadsavklaringLøsningMottak(testRapid, oppgaveMediator)

        testRapid.sendTestMessage(
            løsningMelding(
                eøs = true,
                sanksjon = false,
                barnOver16 = false,
            ),
        )

        verify(exactly = 1) {
            oppgaveMediator.leggTilEmneknagger(oppgaveId, setOf("EØS"))
        }
    }

    @Test
    fun `Skal ikke kalle leggTilEmneknagger når alle løsninger er false`() {
        val oppgaveMediator =
            mockk<OppgaveMediator>().also {
                every { it.leggTilEmneknagger(any<UUID>(), any()) } returns Unit
            }
        SøknadsavklaringLøsningMottak(testRapid, oppgaveMediator)

        testRapid.sendTestMessage(
            løsningMelding(
                eøs = false,
                sanksjon = false,
                barnOver16 = false,
            ),
        )

        verify(exactly = 0) { oppgaveMediator.leggTilEmneknagger(any<UUID>(), any()) }
    }

    private fun løsningMelding(
        eøs: Boolean,
        sanksjon: Boolean,
        barnOver16: Boolean,
    ): String {
        //language=JSON
        return """
            {
              "@event_name": "behov",
              "@behov": ["EØSTilknytning", "Sanksjon", "BarnOver16"],
              "@final": true,
              "@løsning": {
                "EØSTilknytning": { "verdi": $eøs },
                "Sanksjon": { "verdi": $sanksjon },
                "BarnOver16": { "verdi": $barnOver16 }
              },
              "oppgaveId": "$oppgaveId",
              "søknadId": "${UUIDv7.ny()}",
              "ident": "12345678901"
            }
            """.trimIndent()
    }
}
