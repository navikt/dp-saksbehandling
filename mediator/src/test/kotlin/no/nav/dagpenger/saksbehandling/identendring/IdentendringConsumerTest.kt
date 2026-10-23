package no.nav.dagpenger.saksbehandling.identendring

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.CounterSnapshot
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.getSnapShot
import org.junit.jupiter.api.Test
import java.util.UUID

class IdentendringConsumerTest {
    private val sakId = UUID.randomUUID()
    private val sakRepository =
        mockk<SakRepository>().also {
            every { it.finnSisteDagpengeSakId(any()) } returns null
            every { it.finnSisteDagpengeSakId("41111111111") } returns sakId
            every { it.finnSisteDagpengeSakId("22222222222") } returns sakId
        }
    private val rapid = TestRapid()
    private val registry = PrometheusRegistry()
    private val consumer = IdentendringConsumer(sakRepository, rapid, registry)

    @Test
    fun `sender ett varsel per sak uten ident når historisk ident har dagpengesak`() {
        consumer.håndter(setOf("41111111111", "22222222222", "33333333333"))

        rapid.inspektør.size shouldBe 1
        rapid.inspektør.message(0).let {
            it["@event_name"].asText() shouldBe "saksbehandling_alert"
            it["alertType"].asText() shouldBe "IDENTENDRING_FOR_SAK"
            it["feilMelding"].asText().let { melding ->
                melding shouldNotContain "41111111111"
                melding shouldNotContain "22222222222"
            }
        }
        rapid.inspektør.message(0).toString() shouldNotContain "41111111111"
        tellerFor("treff") shouldBe 1.0
    }

    @Test
    fun `sender ikke varsel når ingen historiske identer har dagpengesak`() {
        consumer.håndter(setOf("33333333333"))

        rapid.inspektør.size shouldBe 0
        tellerFor("ikke_treff") shouldBe 1.0
    }

    private fun tellerFor(utfall: String) =
        registry
            .getSnapShot<CounterSnapshot> { it == "dp_saksbehandling_identendring" }
            .dataPoints
            .single { it.labels["utfall"] == utfall }
            .value
}
