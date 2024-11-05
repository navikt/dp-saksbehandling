package no.nav.dagpenger.saksbehandling.metrikker

import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.dataSource
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import javax.sql.DataSource
import kotlin.concurrent.fixedRateTimer

private val logger = KotlinLogging.logger {}

private val oppgaveTilstandGauge: Gauge =
    Gauge.builder()
        .name("dp_saksbehandling_oppgave_tilstand_gauge")
        .help("Antall oppgaver i hver tilstand")
        .labelNames("tilstand")
        .register(PrometheusRegistry.defaultRegistry)

private val Int.Minutt get() = this * 1000L * 60L

fun startOppgaveTilstandMetrikkJob() {
    val nå = Date.from(Instant.now().atZone(ZoneId.of("Europe/Oslo")).toInstant())

    fixedRateTimer(
        name = "OppgaveTilstandMetrikkJob",
        daemon = true,
        startAt = nå,
        period = 1.Minutt,
        action = {
            try {
                oppdaterOppgaveTilstandMetrikker(dataSource)
            } catch (e: Exception) {
                logger.error(e) { "Oppdatering av oppgave tilstand metrikker feilet: ${e.message}" }
            }
        },
    )
}

private fun oppdaterOppgaveTilstandMetrikker(dataSource: DataSource) {
    val oppgaveTilstandDistribusjon = hentOppgaveTilstandDistribusjon(dataSource)

    oppgaveTilstandDistribusjon.forEach { distribusjon ->
        oppgaveTilstandGauge.labelValues(distribusjon.oppgaveTilstand).set(distribusjon.antall.toDouble())
    }
}

data class OppgaveTilstandDistribusjon(val oppgaveTilstand: String, val antall: Int)

fun hentOppgaveTilstandDistribusjon(dataSource: DataSource): List<OppgaveTilstandDistribusjon> {
    //language=PostgreSQL
    val query =
        """
        SELECT tilstand, COUNT(*) as antall
        FROM public.oppgave_v1
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
