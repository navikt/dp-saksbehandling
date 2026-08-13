package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.KlageinstansVedtakHendelse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class KlageinstansVedtakMottakForOppgaveTest {
    private val testRapid = TestRapid()
    private val eventId = UUID.randomUUID().toString()
    private val klageId = UUID.randomUUID().toString()
    private val klageinstansVedtakId = UUID.randomUUID().toString()
    private val avsluttet = LocalDateTime.now().toString()

    @Test
    fun `Skal håndtere klageinstans-vedtak av type klage`() {
        val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
        KlageinstansVedtakMottakForOppgave(
            rapidsConnection = testRapid,
            oppgaveMediator = oppgaveMediator,
        )

        @Language("JSON")
        val vedtakMelding =
            """
            {
              "@event_name": "KlageAnkeVedtak", 
              "eventId": "$eventId",
              "kildeReferanse": "$klageId",
              "kilde": "DAGPENGER",
              "kabalReferanse": "$klageinstansVedtakId",
              "type": "KLAGEBEHANDLING_AVSLUTTET",
              "detaljer": {
                "klagebehandlingAvsluttet": {
                  "avsluttet": "$avsluttet",
                  "utfall": "STADFESTELSE",
                  "journalpostReferanser": [
                    "jp1",
                    "jp2"
                  ]
                }
              }
            }
            """.trimIndent()

        testRapid.sendTestMessage(vedtakMelding)
        verify(exactly = 1) {
            oppgaveMediator.håndterUtfallFraKlageinstans(
                KlageinstansVedtakHendelse(
                    type = KlageinstansVedtakHendelse.KlageinstansVedtakType.KLAGE,
                    klageId = UUID.fromString(klageId),
                    klageinstansVedtakId = UUID.fromString(klageinstansVedtakId),
                    avsluttet = LocalDateTime.parse(avsluttet),
                    utfall = "STADFESTELSE",
                    journalpostIder = listOf("jp1", "jp2"),
                ),
            )
        }
    }
}
