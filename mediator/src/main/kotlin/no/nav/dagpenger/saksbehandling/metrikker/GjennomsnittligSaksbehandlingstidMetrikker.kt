package no.nav.dagpenger.saksbehandling.metrikker

import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

private val gjennomsnittligSaksbehandlingstidGauge: Gauge =
    Gauge.builder()
        .name("dp_saksbehandling_avg_saksbehandlingstid_min_gauge")
        .help("Gjennomsnittlig saksbehandlingstid i minutter")
        .register(PrometheusRegistry.defaultRegistry)

private val medianSaksbehandlingstidGauge: Gauge =
    Gauge.builder()
        .name("dp_saksbehandling_median_saksbehandlingstid_min_gauge")
        .help("Median saksbehandlingstid i minutter")
        .register(PrometheusRegistry.defaultRegistry)

fun oppdaterSaksbehandlingstidMetrikker(dataSource: DataSource) {
    try {
        val gjennomsnittligSaksbehandlingstidMinutter = gjennomsnittligSaksbehandlingstidSekunder(dataSource) / 60.0
        gjennomsnittligSaksbehandlingstidGauge.labelValues().set(gjennomsnittligSaksbehandlingstidMinutter)

        val medianSaksbehandlingstidMinutter = medianSaksbehandlingstidSekunder(dataSource) / 60.0
        medianSaksbehandlingstidGauge.labelValues().set(medianSaksbehandlingstidMinutter)
    } catch (e: Exception) {
        logger.error(e) { "Henting av oppgavetilstand distribusjon feilet: ${e.message}" }
    }
}

fun gjennomsnittligSaksbehandlingstidSekunder(dataSource: DataSource): Double {
    //language=PostgreSQL
    val query =
        """
        WITH behandlingstid_per_oppgave AS
                 (SELECT oppgave_id,
                         EXTRACT(epoch FROM AGE(
                                         MIN(tidspunkt) FILTER (WHERE tilstand = 'FERDIG_BEHANDLET'),
                                         MIN(tidspunkt) FILTER (WHERE tilstand = 'UNDER_BEHANDLING')
                                            )) AS behandlingstid_sekunder
                  FROM oppgave_tilstand_logg_v1
                  WHERE tilstand IN ('UNDER_BEHANDLING', 'FERDIG_BEHANDLET')
                  GROUP BY oppgave_id
                  HAVING MIN(tidspunkt) FILTER (WHERE tilstand = 'UNDER_BEHANDLING') IS NOT NULL
                     AND MIN(tidspunkt) FILTER (WHERE tilstand = 'FERDIG_BEHANDLET') IS NOT NULL)

        SELECT AVG(behandlingstid_sekunder) AS gjennomsnitt_behandlingstid_sekunder
        FROM behandlingstid_per_oppgave
        """.trimIndent()

    return sessionOf(dataSource).use { session ->
        session.run(
            queryOf(query).map { row ->
                row.double("gjennomsnitt_behandlingstid_sekunder")
            }.asSingle,
        )
    } ?: 0.0
}

fun medianSaksbehandlingstidSekunder(dataSource: DataSource): Double {
    //language=PostgreSQL
    val query =
        """
        WITH behandlingstid_per_oppgave AS (SELECT oppgave_id,
                                                   EXTRACT(epoch FROM AGE(
                                                                   MIN(tidspunkt) FILTER (WHERE tilstand = 'FERDIG_BEHANDLET'),
                                                                   MIN(tidspunkt) FILTER (WHERE tilstand = 'UNDER_BEHANDLING')
                                                                      )) AS behandlingstid_sekunder
                                            FROM oppgave_tilstand_logg_v1
                                            WHERE tilstand IN ('UNDER_BEHANDLING', 'FERDIG_BEHANDLET')
                                            GROUP BY oppgave_id
                                            HAVING MIN(tidspunkt) FILTER (WHERE tilstand = 'UNDER_BEHANDLING') IS NOT NULL
                                               AND MIN(tidspunkt) FILTER (WHERE tilstand = 'FERDIG_BEHANDLET') IS NOT NULL)

        SELECT ROUND(
                       percentile_cont(0.5) WITHIN GROUP (ORDER BY behandlingstid_sekunder)
               ) AS median_behandlingstid_sekunder
        FROM behandlingstid_per_oppgave;
        """.trimIndent()

    return sessionOf(dataSource).use { session ->
        session.run(
            queryOf(query).map { row ->
                row.double("median_behandlingstid_sekunder")
            }.asSingle,
        )
    } ?: 0.0
}
