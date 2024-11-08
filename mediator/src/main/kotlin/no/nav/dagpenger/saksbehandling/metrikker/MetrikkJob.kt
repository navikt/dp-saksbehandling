package no.nav.dagpenger.saksbehandling.metrikker

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.dataSource
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import kotlin.concurrent.fixedRateTimer

private val logger = KotlinLogging.logger {}
private val Int.Minutt get() = this * 1000L * 60L

fun startMetrikkJob() {
    val nå = Date.from(Instant.now().atZone(ZoneId.of("Europe/Oslo")).toInstant())

    fixedRateTimer(
        name = "MetrikkJob",
        daemon = true,
        startAt = nå,
        period = 5.Minutt,
        action = {
            logger.info("Oppdaterer metrikker")
            oppdaterOppgaveTilstandMetrikker(dataSource)
            oppdaterUtsendingTilstandMetrikker(dataSource)
            oppdaterOppgaveTilstandSiste24TimerMetrikker(dataSource)
            oppdaterSaksbehandlingstidMetrikker(dataSource)
        },
    )
}
