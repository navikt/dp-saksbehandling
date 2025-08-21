package no.nav.dagpenger.saksbehandling.metrikker

import io.github.oshai.kotlinlogging.KotlinLogging
import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotliquery.queryOf
import kotliquery.sessionOf
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

private val utsendingTilstandGauge: Gauge =
    Gauge.builder()
        .name("dp_saksbehandling_utsending_tilstand_gauge")
        .help("Antall utsendinger i hver tilstand")
        .labelNames("tilstand")
        .register(PrometheusRegistry.defaultRegistry)

fun oppdaterUtsendingTilstandMetrikker(dataSource: DataSource) {
    try {
        val oppgaveTilstandDistribusjon = hentUtsendingTilstandDistribusjon(dataSource)

        oppgaveTilstandDistribusjon.forEach { distribusjon ->
            utsendingTilstandGauge.labelValues(distribusjon.utsendingTilstand).set(distribusjon.antall.toDouble())
        }
    } catch (e: Exception) {
        logger.error(e) { "Henting av utsendingstilstand distribusjon feilet: ${e.message}" }
    }
}

data class UtsendingTilstandDistribusjon(val utsendingTilstand: String, val antall: Int)

fun hentUtsendingTilstandDistribusjon(dataSource: DataSource): List<UtsendingTilstandDistribusjon> {
    //language=PostgreSQL
    val query =
        """
        SELECT tilstand, COUNT(*) as antall
        FROM utsending_v1
        GROUP BY tilstand;
        """.trimIndent()

    return sessionOf(dataSource).use { session ->
        session.run(
            queryOf(query).map { row ->
                UtsendingTilstandDistribusjon(
                    utsendingTilstand = row.string("tilstand"),
                    antall = row.int("antall"),
                )
            }.asList,
        )
    }
}
