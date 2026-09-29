package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.klage.KlageBehandlingSammendrag
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class KlageAlarmJobTest {
    private val testRapid = TestRapid()

    @Test
    fun `sender alert for behandlinger som ikke er ferdigstilt`() {
        val behandlingId = UUIDv7.ny()
        val sistEndret = LocalDateTime.now().minusMonths(5).truncatedTo(ChronoUnit.HOURS)
        val repository =
            mockk<KlageRepository> {
                every {
                    hentBehandlingerIkkeFerdigstilt(
                        type = KlageBehandling.KlageTilstand.Type.BEHANDLES_AV_KLAGEINSTANS,
                        sistEndretEldreEnn = any(),
                    )
                } returns
                    listOf(
                        KlageBehandlingSammendrag(
                            behandlingId = behandlingId,
                            tilstand = KlageBehandling.KlageTilstand.Type.BEHANDLES_AV_KLAGEINSTANS.name,
                            sistEndret = sistEndret,
                        ),
                    )
            }

        runBlocking {
            KlageAlarmJob(testRapid, repository).executeJob()
        }

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).let { message ->
            message["@event_name"].stringValue() shouldBe "saksbehandling_alert"
            message["alertType"].stringValue() shouldBe "KLAGEBEHANDLING_IKKE_FERDIGSTILT_ALERT"
            message["feilMelding"].stringValue() shouldContain "BehandlingId: $behandlingId"
            message["feilMelding"].stringValue() shouldContain "Sist endret: $sistEndret"
            message["utvidetFeilMelding"].stringValue() shouldBe message["feilMelding"].stringValue()
        }
    }

    @Test
    fun `sender ingen alert når ingen behandlinger er funnet`() {
        val repository =
            mockk<KlageRepository> {
                every { hentBehandlingerIkkeFerdigstilt(any(), any()) } returns emptyList()
            }

        runBlocking {
            KlageAlarmJob(testRapid, repository).executeJob()
        }

        testRapid.inspektør.size shouldBe 0
    }
}
