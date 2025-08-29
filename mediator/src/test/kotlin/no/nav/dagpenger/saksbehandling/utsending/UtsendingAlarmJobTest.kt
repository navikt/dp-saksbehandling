package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class UtsendingAlarmJobTest {
    private val testRapid = TestRapid()

    @Test
    fun senderUtAlert() {
        runBlocking {
            val idag = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
            val igår = idag.minusDays(1)
            val utsendingId1 = UUIDv7.ny()
            val utsendingId2 = UUIDv7.ny()
            val personId = UUIDv7.ny()
            val behandlingId = UUIDv7.ny()
            UtsendingAlarmJob(
                rapidsConnection = testRapid,
                utsendingAlarmRepository =
                    mockk<UtsendingAlarmRepository>().also {
                        every { it.hentVentendeUtsendinger(any()) } returns
                            listOf(
                                AlertManager.UtsendingIkkeFullført(
                                    utsendingId = utsendingId1,
                                    tilstand = "AvventerArkiverbarVersjonAvBrev",
                                    sistEndret = igår,
                                    behandlingId = behandlingId,
                                    personId = personId,
                                ),
                                AlertManager.UtsendingIkkeFullført(
                                    utsendingId = utsendingId2,
                                    tilstand = "AvventerDistribuering",
                                    sistEndret = igår,
                                    behandlingId = behandlingId,
                                    personId = personId,
                                ),
                            )
                    },
            ).executeJob()

            testRapid.inspektør.size shouldBe 2
            testRapid.inspektør.message(0).let { jsonNode ->
                jsonNode["alertType"].asText() shouldBe "UTSENDING_IKKE_FULLFØRT"
                jsonNode["@event_name"].asText() shouldBe "saksbehandling_alert"
                jsonNode["feilMelding"].asText() shouldBe
                    """
                    Utsending ikke fullført for utsendingId: $utsendingId1.
                    Den har vært i tilstand AvventerArkiverbarVersjonAvBrev i 24 timer (sist endret: $igår)
                    BehandlingId: $behandlingId
                    PersonId: $personId
                    """.trimIndent()
            }

            testRapid.inspektør.message(1).let { jsonNode ->
                jsonNode["alertType"].asText() shouldBe "UTSENDING_IKKE_FULLFØRT"
                jsonNode["@event_name"].asText() shouldBe "saksbehandling_alert"
                jsonNode["feilMelding"].asText() shouldBe
                    """
                    Utsending ikke fullført for utsendingId: $utsendingId2.
                    Den har vært i tilstand AvventerDistribuering i 24 timer (sist endret: $igår)
                    BehandlingId: $behandlingId
                    PersonId: $personId
                    """.trimIndent()
            }
        }
    }
}
