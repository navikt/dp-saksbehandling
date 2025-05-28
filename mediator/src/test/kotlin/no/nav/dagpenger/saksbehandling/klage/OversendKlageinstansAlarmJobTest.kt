package no.nav.dagpenger.saksbehandling.klage

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

class OversendKlageinstansAlarmJobTest {
    private val testRapid = TestRapid()

    @Test
    fun senderUtAlert() {
        runBlocking {
            val iDag = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
            val iGår = iDag.minusDays(1)
            val behandlingId1 = UUIDv7.ny()
            val behandlingId2 = UUIDv7.ny()
            OversendKlageinstansAlarmJob(
                rapidsConnection = testRapid,
                repository =
                    mockk<OversendKlageinstansAlarmRepository>().also {
                        every { it.hentVentendeOversendelser(any()) } returns
                            listOf(
                                AlertManager.OversendKlageinstansIkkeFullført(
                                    behandlingId = behandlingId1,
                                    tilstand = "OVERSEND_KLAGEINSTANS",
                                    sistEndret = iGår,
                                ),
                                AlertManager.OversendKlageinstansIkkeFullført(
                                    behandlingId = behandlingId2,
                                    tilstand = "OVERSEND_KLAGEINSTANS",
                                    sistEndret = iDag,
                                ),
                            )
                    },
            ).executeJob()

            testRapid.inspektør.size shouldBe 2
            testRapid.inspektør.message(0).let { jsonNode ->
                jsonNode["alertType"].asText() shouldBe "OVERSEND_KLAGEINSTANS_IKKE_FULLFØRT"
                jsonNode["@event_name"].asText() shouldBe "saksbehandling_alert"
                jsonNode["feilMelding"].asText() shouldBe "Oversendelse til klageinstans ikke fullført for " +
                    "klagebehandling $behandlingId1. Den har vært i tilstand OVERSEND_KLAGEINSTANS siden $iGår"
            }

            testRapid.inspektør.message(1).let { jsonNode ->
                jsonNode["alertType"].asText() shouldBe "OVERSEND_KLAGEINSTANS_IKKE_FULLFØRT"
                jsonNode["@event_name"].asText() shouldBe "saksbehandling_alert"
                jsonNode["feilMelding"].asText() shouldBe "Oversendelse til klageinstans ikke fullført for " +
                    "klagebehandling $behandlingId2. Den har vært i tilstand OVERSEND_KLAGEINSTANS siden $iDag"
            }
        }
    }
}
