package no.nav.dagpenger.saksbehandling.job

import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.core.metrics.Histogram
import java.time.Instant

/**
 * Generiske metrikker for bakgrunnsjobber ([Job]). Registreres på defaultRegistry
 * (samme mønster som [no.nav.dagpenger.saksbehandling.db.DbMetrics]).
 *
 * Jobbene kan kjøre med svært ulike intervaller (sekunder til timer). Derfor:
 * - [executions] er en counter — intervall-agnostisk, "hvor ofte" hentes via rate().
 * - [period] eksponeres som gauge slik at staleness-alarmer kan gjøres
 *   intervall-relative: `time() - last_success > 3 * period`.
 */
internal object JobMetrics {
    private const val PREFIX = "dp_saksbehandling_job_"
    private const val NANOS_PER_SECOND = 1_000_000_000.0
    private const val MILLIS_PER_SECOND = 1_000.0

    fun success(jobName: String) {
        executions.labelValues(jobName, "success").inc()
        lastSuccess.labelValues(jobName).set(Instant.now().epochSecond.toDouble())
    }

    /**
     * Seeder [lastSuccess]-gaugen ved jobbstart slik at tidsserien alltid eksisterer.
     * Uten dette får en jobb som feiler på hver kjøring siden pod-start aldri noen
     * last_success-serie, og staleness-alarmen (som joiner på `job`) kan ikke fyre.
     * Forankres i boot-tid: en jobb ødelagt-fra-boot blir gammel og alarmen fyrer.
     */
    fun seedLastSuccess(jobName: String) {
        lastSuccess.labelValues(jobName).set(Instant.now().epochSecond.toDouble())
    }

    fun failure(jobName: String) {
        executions.labelValues(jobName, "failure").inc()
    }

    fun skippedOverlap(jobName: String) {
        executions.labelValues(jobName, "skipped_overlap").inc()
    }

    fun duration(
        jobName: String,
        durationNanos: Long?,
    ) {
        durationNanos?.let {
            duration.labelValues(jobName).observe(it / NANOS_PER_SECOND)
        }
    }

    fun period(
        jobName: String,
        periodMillis: Long,
    ) {
        period.labelValues(jobName).set(periodMillis / MILLIS_PER_SECOND)
    }

    /** status: success | failure | skipped_overlap. Kun lederen kjører, så sum() over pods = total. */
    val executions: Counter =
        Counter
            .builder()
            .name("${PREFIX}executions_total")
            .help("Antall jobb-kjøringer per status")
            .labelNames("job", "status")
            .register()

    val duration: Histogram =
        Histogram
            .builder()
            .name("${PREFIX}duration_seconds")
            .help("Kjøretid for jobb-kjøring")
            .labelNames("job")
            .register()

    val lastSuccess: Gauge =
        Gauge
            .builder()
            .name("${PREFIX}last_success_timestamp_seconds")
            .help("Tidspunkt (epoch seconds) for siste vellykkede kjøring")
            .labelNames("job")
            .register()

    val period: Gauge =
        Gauge
            .builder()
            .name("${PREFIX}period_seconds")
            .help("Konfigurert intervall for jobb i sekunder")
            .labelNames("job")
            .register()
}
