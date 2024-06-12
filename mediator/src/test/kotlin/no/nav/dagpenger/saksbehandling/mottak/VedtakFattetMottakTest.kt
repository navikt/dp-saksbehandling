package no.nav.dagpenger.saksbehandling.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class VedtakFattetMottakTest {
    val testIdent = "12345678901"
    val søknadId = UUID.randomUUID()
    val behandlingId = UUID.randomUUID()
    val sak = Sak("12342", "Arena")
    val opprettet = LocalDateTime.parse("2024-02-27T10:41:52.800935377")

    private val testRapid = TestRapid()
    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)

    init {
        VedtakFattetMottak(testRapid, oppgaveMediatorMock)
    }

    @Test
    fun `Skal behandle vedtak fattet hendelse`() {
        testRapid.sendTestMessage(vedtakFattetHendelse())
        val vedtakFattetHendelse =
            VedtakFattetHendelse(
                behandlingId = behandlingId,
                søknadId = søknadId,
                ident = testIdent,
                sak = sak,
            )
        verify(exactly = 1) {
            oppgaveMediatorMock.startUtsending(vedtakFattetHendelse)
        }
//        testRapid.inspektør.size shouldBe 1
//        testRapid.inspektør.message(0).let { jsonNode ->
//            jsonNode.path("@event_name").asText() shouldBe "start_utsending"
//            jsonNode["sak"].let { sakIdNode ->
//                sakIdNode["id"].asText() shouldBe sak.id
//                sakIdNode["kontekst"].asText() shouldBe sak.kontekst
//            }
//        }
    }

    @Language("JSON")
    private fun vedtakFattetHendelse(ident: String = testIdent) =
        """{
  "@event_name": "vedtak_fattet",
  "søknadId": "$søknadId",
  "behandlingId": "$behandlingId",
  "ident": "$ident",
  "sakId": {
    "id": "12342",
    "kontekst": "Arena"
  }
}"""
}
