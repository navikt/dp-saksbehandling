package no.nav.dagpenger.saksbehandling.streams.kafka

import io.kotest.matchers.shouldBe
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.CounterSnapshot
import io.prometheus.metrics.model.snapshots.GaugeSnapshot
import org.apache.kafka.streams.KafkaStreams.State
import org.apache.kafka.streams.errors.StreamsException
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse
import org.junit.jupiter.api.Test

class KafkaStreamsMonitorTest {
    private val registry = PrometheusRegistry()
    private val monitor = KafkaStreamsMonitor("test", registry)

    @Test
    fun `running-gauge er 0 før start og følger tilstanden`() {
        running() shouldBe 0.0

        monitor.onStateChange(State.REBALANCING, State.CREATED)
        running() shouldBe 1.0

        monitor.onStateChange(State.RUNNING, State.REBALANCING)
        running() shouldBe 1.0

        monitor.onStateChange(State.PENDING_ERROR, State.RUNNING)
        running() shouldBe 0.0

        monitor.onStateChange(State.ERROR, State.PENDING_ERROR)
        running() shouldBe 0.0
    }

    @Test
    fun `uhåndtert feil telles og stopper klienten`() {
        errors() shouldBe 0.0

        monitor.onUncaughtException(StreamsException(RuntimeException("feil"))) shouldBe
            StreamThreadExceptionResponse.SHUTDOWN_CLIENT

        errors() shouldBe 1.0
    }

    private fun running(): Double =
        (registry.scrape().single { it.metadata.name == "dp_saksbehandling_kafka_streams_kjorer" } as GaugeSnapshot)
            .dataPoints
            .single { it.labels["instans"] == "test" }
            .value

    private fun errors(): Double =
        (registry.scrape().single { it.metadata.name == "dp_saksbehandling_kafka_streams_feil" } as CounterSnapshot)
            .dataPoints
            .single { it.labels["instans"] == "test" }
            .value
}
