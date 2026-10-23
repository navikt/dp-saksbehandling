package no.nav.dagpenger.saksbehandling.streams.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class KafkaStreamsOvervåker(
    private val navn: String,
    registry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
) {
    private val kjører: Gauge =
        Gauge
            .builder()
            .name("dp_saksbehandling_kafka_streams_kjorer")
            .help("1 hvis Kafka Streams-instansen kjører (RUNNING/REBALANCING), ellers 0")
            .labelNames("instans")
            .register(registry)

    private val feil: Counter =
        Counter
            .builder()
            .name("dp_saksbehandling_kafka_streams_feil")
            .help("Antall uhåndterte feil som har stoppet en Kafka Streams-instans")
            .labelNames("instans")
            .register(registry)

    init {
        kjører.labelValues(navn).set(0.0)
        feil.labelValues(navn)
    }

    fun uhåndtertFeil(throwable: Throwable): StreamThreadExceptionResponse {
        feil.labelValues(navn).inc()
        // Exception-meldinger kan inneholde fnr, derfor kun klassenavn i vanlig logg
        log.error { "Kafka Streams '$navn' stoppet pga uhåndtert feil: ${throwable.rotårsak().javaClass.name}. Se sikkerlogg." }
        sikkerlogg.error(throwable) { "Kafka Streams '$navn' stoppet pga uhåndtert feil" }
        return StreamThreadExceptionResponse.SHUTDOWN_CLIENT
    }

    fun tilstandEndret(
        ny: KafkaStreams.State,
        gammel: KafkaStreams.State,
    ) {
        kjører.labelValues(navn).set(if (ny.isRunningOrRebalancing) 1.0 else 0.0)
        when (ny) {
            KafkaStreams.State.ERROR -> log.error { "Kafka Streams '$navn' gikk fra $gammel til $ny" }
            else -> log.info { "Kafka Streams '$navn' gikk fra $gammel til $ny" }
        }
    }

    private fun Throwable.rotårsak(): Throwable = generateSequence(this) { it.cause }.last()
}
