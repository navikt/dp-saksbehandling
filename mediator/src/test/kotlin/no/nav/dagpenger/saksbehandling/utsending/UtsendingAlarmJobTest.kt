package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.OppgaveAlertManager
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class UtsendingAlarmJobTest {
    private val testRapid = TestRapid()

    @Test
    fun senderUtAlert() {
        val idag = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        val igår = idag.minusDays(1)
        val utsendingId1 = UUIDv7.ny()
        val utsendingId2 = UUIDv7.ny()
        UtsendingAlarmJob(
            rapidsConnection = testRapid,
            utsendingAlarmRepository =
                mockk<UtsendingAlarmRepository>().also {
                    every { it.hentVentendeUtsendinger(any()) } returns
                        listOf(
                            OppgaveAlertManager.UtsendingIkkeFullført(
                                utsendingId = utsendingId1,
                                tilstand = "AvventerArkiverbarVersjonAvBrev",
                                sistEndret = igår,
                            ),
                            OppgaveAlertManager.UtsendingIkkeFullført(
                                utsendingId = utsendingId2,
                                tilstand = "AvventerDistribuering",
                                sistEndret = idag,
                            ),
                        )
                },
        ).sjekkVentendeTilstander()

        testRapid.inspektør.size shouldBe 2
        testRapid.inspektør.message(0).let { jsonNode ->
            jsonNode["alertType"].asText() shouldBe "UTSENDING_IKKE_FULLFØRT"
            jsonNode["@event_name"].asText() shouldBe "saksbehandling_alert"
            jsonNode["feilMelding"].asText() shouldBe "Utsending ikke fullført for $utsendingId1. Den har vært " +
                "i tilstand AvventerArkiverbarVersjonAvBrev siden $igår"
        }

        testRapid.inspektør.message(1).let { jsonNode ->
            jsonNode["alertType"].asText() shouldBe "UTSENDING_IKKE_FULLFØRT"
            jsonNode["@event_name"].asText() shouldBe "saksbehandling_alert"
            jsonNode["feilMelding"].asText() shouldBe "Utsending ikke fullført for $utsendingId2. Den har vært " +
                "i tilstand AvventerDistribuering siden $idag"
        }
    }
}
