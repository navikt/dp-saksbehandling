package no.nav.dagpenger.saksbehandling.streams.kafka

import io.kotest.matchers.shouldBe
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.CounterSnapshot
import io.prometheus.metrics.model.snapshots.GaugeSnapshot
import org.apache.kafka.streams.KafkaStreams.State
import org.apache.kafka.streams.errors.StreamsException
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse
import org.junit.jupiter.api.Test

class KafkaStreamsOvervåkerTest {
    private val registry = PrometheusRegistry()
    private val overvåker = KafkaStreamsOvervåker("test", registry)

    @Test
    fun `kjører-gauge er 0 før start og følger tilstanden`() {
        kjører() shouldBe 0.0

        overvåker.tilstandEndret(State.REBALANCING, State.CREATED)
        kjører() shouldBe 1.0

        overvåker.tilstandEndret(State.RUNNING, State.REBALANCING)
        kjører() shouldBe 1.0

        overvåker.tilstandEndret(State.PENDING_ERROR, State.RUNNING)
        kjører() shouldBe 0.0

        overvåker.tilstandEndret(State.ERROR, State.PENDING_ERROR)
        kjører() shouldBe 0.0
    }

    @Test
    fun `uhåndtert feil telles og stopper klienten`() {
        feil() shouldBe 0.0

        overvåker.uhåndtertFeil(StreamsException(RuntimeException("feil"))) shouldBe
            StreamThreadExceptionResponse.SHUTDOWN_CLIENT

        feil() shouldBe 1.0
    }

    private fun kjører(): Double =
        (registry.scrape().single { it.metadata.name == "dp_saksbehandling_kafka_streams_kjorer" } as GaugeSnapshot)
            .dataPoints
            .single { it.labels["instans"] == "test" }
            .value

    private fun feil(): Double =
        (registry.scrape().single { it.metadata.name == "dp_saksbehandling_kafka_streams_feil" } as CounterSnapshot)
            .dataPoints
            .single { it.labels["instans"] == "test" }
            .value
}
