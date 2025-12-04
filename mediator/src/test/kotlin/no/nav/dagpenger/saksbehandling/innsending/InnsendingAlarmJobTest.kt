package no.nav.dagpenger.saksbehandling.innsending

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test

class InnsendingAlarmJobTest {
    private val testRapid = TestRapid()

    @Test
    fun senderUtAlert() {
        runBlocking {
            val iDag = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
            val iGår = iDag.minusDays(1)
            val iForgårs = iDag.minusDays(1)
            val innsendingId1 = UUIDv7.ny()
            val innsendingId2 = UUIDv7.ny()
            val personId = UUIDv7.ny()
            val journalpostId = "123"
            InnsendingAlarmJob(
                rapidsConnection = testRapid,
                innsendingAlarmRepository =
                    mockk<InnsendingAlarmRepository>().also {
                        every { it.hentInnsendingerSomIkkeErFerdigstilt(any()) } returns
                                listOf(
                                    AlertManager.InnsendingIkkeFerdigstilt(
                                        innsendingId = innsendingId1,
                                        tilstand = "FERDIGSTILL_STARTET",
                                        sistEndret = iGår,
                                        journalpostId = journalpostId,
                                        personId = personId,
                                    ),
                                    AlertManager.InnsendingIkkeFerdigstilt(
                                        innsendingId = innsendingId2,
                                        tilstand = "FERDIGSTILL_STARTET",
                                        sistEndret = iForgårs,
                                        journalpostId = journalpostId,
                                        personId = personId,
                                    ),
                                )
                    },
            ).executeJob()

            testRapid.inspektør.size shouldBe 2
            testRapid.inspektør.message(0).let { jsonNode ->
                jsonNode["alertType"].asText() shouldBe "INNSENDING_IKKE_FULLFØRT"
                jsonNode["@event_name"].asText() shouldBe "saksbehandling_alert"
                jsonNode["feilMelding"].asText() shouldBe
                        """
                    Innsending ikke fullført for innsendingId: $innsendingId1.
                    Den har vært i tilstand FERDIGSTILL_STARTET i 24 timer (sist endret: $iGår)
                    BehandlingId: $journalpostId
                    PersonId: $personId
                    """.trimIndent()
            }

            testRapid.inspektør.message(1).let { jsonNode ->
                jsonNode["alertType"].asText() shouldBe "UTSENDING_IKKE_FULLFØRT"
                jsonNode["@event_name"].asText() shouldBe "saksbehandling_alert"
                jsonNode["feilMelding"].asText() shouldBe
                        """
                    Utsending ikke fullført for utsendingId: $innsendingId2.
                    Den har vært i tilstand AvventerDistribuering i 24 timer (sist endret: $iGår)
                    BehandlingId: $journalpostId
                    PersonId: $personId
                    """.trimIndent()
            }
        }
    }
}