package no.nav.dagpenger.saksbehandling.streams.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class KafkaStreamsMonitor(
    private val name: String,
    registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
) {
    private val running: Gauge =
        Gauge
            .builder()
            .name("dp_saksbehandling_kafka_streams_kjorer")
            .help("1 hvis Kafka Streams-instansen kjører (RUNNING/REBALANCING), ellers 0")
            .labelNames("instans")
            .register(registry)

    private val errors: Counter =
        Counter
            .builder()
            .name("dp_saksbehandling_kafka_streams_feil")
            .help("Antall uhåndterte feil som har stoppet en Kafka Streams-instans")
            .labelNames("instans")
            .register(registry)

    init {
        running.labelValues(name).set(0.0)
        errors.labelValues(name)
    }

    fun onUncaughtException(throwable: Throwable): StreamThreadExceptionResponse {
        errors.labelValues(name).inc()
        log.error { "Kafka Streams '$name' stoppet pga uhåndtert feil. Se sikkerlogg." }
        sikkerlogg.error(throwable) { "Kafka Streams '$name' stoppet pga uhåndtert feil" }
        return StreamThreadExceptionResponse.SHUTDOWN_CLIENT
    }

    fun onStateChange(
        newState: KafkaStreams.State,
        oldState: KafkaStreams.State,
    ) {
        running.labelValues(name).set(if (newState.isRunningOrRebalancing) 1.0 else 0.0)
        when (newState) {
            KafkaStreams.State.ERROR -> log.error { "Kafka Streams '$name' gikk fra $oldState til $newState" }
            else -> log.info { "Kafka Streams '$name' gikk fra $oldState til $newState" }
        }
    }
}
