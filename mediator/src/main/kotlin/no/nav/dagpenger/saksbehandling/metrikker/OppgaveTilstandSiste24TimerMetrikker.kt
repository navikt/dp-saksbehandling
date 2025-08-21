package no.nav.dagpenger.saksbehandling.metrikker

import io.github.oshai.kotlinlogging.KotlinLogging
import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotliquery.queryOf
import kotliquery.sessionOf
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

private val oppgaveTilstandSiste24TimerGauge: Gauge =
    Gauge.builder()
        .name("dp_saksbehandling_oppgave_tilstand_siste_24_t_gauge")
        .help("Antall oppgaver i hver tilstand siste 24t")
        .labelNames("tilstand")
        .register(PrometheusRegistry.defaultRegistry)

fun oppdaterOppgaveTilstandSiste24TimerMetrikker(dataSource: DataSource) {
    try {
        val oppgaveTilstandDistribusjon = hentOppgaveTilstandSiste24TimerDistribusjon(dataSource)

        oppgaveTilstandDistribusjon.forEach { distribusjon ->
            oppgaveTilstandSiste24TimerGauge.labelValues(distribusjon.oppgaveTilstand).set(distribusjon.antall.toDouble())
        }
    } catch (e: Exception) {
        logger.error(e) { "Henting av oppgavetilstand distribusjon for siste 24t feilet: ${e.message}" }
    }
}

data class OppgaveTilstandSiste24TimerDistribusjon(val oppgaveTilstand: String, val antall: Int)

fun hentOppgaveTilstandSiste24TimerDistribusjon(dataSource: DataSource): List<OppgaveTilstandSiste24TimerDistribusjon> {
    //language=PostgreSQL
    val query =
        """
        SELECT tilstand, COUNT(*) as antall
        FROM oppgave_v1
        WHERE endret_tidspunkt >= NOW() - INTERVAL '24 hours'
        GROUP BY tilstand;
        """.trimIndent()

    return sessionOf(dataSource).use { session ->
        session.run(
            queryOf(query).map { row ->
                OppgaveTilstandSiste24TimerDistribusjon(
                    oppgaveTilstand = row.string("tilstand"),
                    antall = row.int("antall"),
                )
            }.asList,
        )
    }
}
