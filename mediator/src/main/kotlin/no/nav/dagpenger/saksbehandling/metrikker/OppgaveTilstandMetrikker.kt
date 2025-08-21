package no.nav.dagpenger.saksbehandling.metrikker

import io.github.oshai.kotlinlogging.KotlinLogging
import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotliquery.queryOf
import kotliquery.sessionOf
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

private val oppgaveTilstandGauge: Gauge =
    Gauge.builder()
        .name("dp_saksbehandling_oppgave_tilstand_gauge")
        .help("Antall oppgaver i hver tilstand")
        .labelNames("tilstand")
        .register(PrometheusRegistry.defaultRegistry)

fun oppdaterOppgaveTilstandMetrikker(dataSource: DataSource) {
    try {
        val oppgaveTilstandDistribusjon = hentOppgaveTilstandDistribusjon(dataSource)

        oppgaveTilstandDistribusjon.forEach { distribusjon ->
            oppgaveTilstandGauge.labelValues(distribusjon.oppgaveTilstand).set(distribusjon.antall.toDouble())
        }
    } catch (e: Exception) {
        logger.error(e) { "Henting av oppgavetilstand distribusjon feilet: ${e.message}" }
    }
}

data class OppgaveTilstandDistribusjon(val oppgaveTilstand: String, val antall: Int)

fun hentOppgaveTilstandDistribusjon(dataSource: DataSource): List<OppgaveTilstandDistribusjon> {
    //language=PostgreSQL
    val query =
        """
        SELECT tilstand, COUNT(*) as antall
        FROM oppgave_v1
        GROUP BY tilstand;
        """.trimIndent()

    return sessionOf(dataSource).use { session ->
        session.run(
            queryOf(query).map { row ->
                OppgaveTilstandDistribusjon(
                    oppgaveTilstand = row.string("tilstand"),
                    antall = row.int("antall"),
                )
            }.asList,
        )
    }
}
