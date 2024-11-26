package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Avbrutt
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Distribuert
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Timer
import javax.sql.DataSource
import kotlin.concurrent.fixedRateTimer

private val logger = KotlinLogging.logger {}

internal class UtsendingAlarmJob(
    private val rapidsConnection: RapidsConnection,
    private val utsendingAlarmRepository: UtsendingAlarmRepository,
) {
    private val Int.Dag get() = this * 1000L * 60L * 60L * 24L

    fun startJob(): Timer {
        return fixedRateTimer(
            name = "UtsendingAlarmJob",
            daemon = true,
            startAt = Date.from(Instant.now().atZone(ZoneId.of("Europe/Oslo")).toInstant()),
            period = 1.Dag,
            action = {
                try {
                    logger.info { "Starter UtsendingAlarmJob" }
                    UtsendingAlarmJob(
                        rapidsConnection = rapidsConnection,
                        utsendingAlarmRepository = utsendingAlarmRepository,
                    ).sjekkVentendeTilstander()
                } catch (e: Exception) {
                    logger.error(e) { "UtsendingAlarmJob feilet: ${e.message} " }
                }
            },
        )
    }

    fun sjekkVentendeTilstander() {
        utsendingAlarmRepository.hentVentendeUtsendinger(24).also {
            logger.info { "Fant ${it.size} ventende utsendinger: $it" }
        }.forEach {
            rapidsConnection.sendAlertTilRapid(
                it,
                it.feilMelding,
            )
        }
    }
}

internal class UtsendingAlarmRepository(private val ds: DataSource) {
    fun hentVentendeUtsendinger(intervallAntallTimer: Int): List<AlertManager.UtsendingIkkeFullført> {
        return sessionOf(ds).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  *
                        FROM    utsending_v1
                        WHERE   tilstand != :distribuert 
                        AND     tilstand != :avbrutt
                        AND     endret_tidspunkt < NOW() - INTERVAL '$intervallAntallTimer hours'
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "distribuert" to Distribuert.name,
                            "avbrutt" to Avbrutt.name,
                        ),
                ).map { row ->
                    AlertManager.UtsendingIkkeFullført(
                        utsendingId = row.uuid("id"),
                        tilstand = row.string("tilstand"),
                        sistEndret = row.localDateTime("endret_tidspunkt"),
                    )
                }.asList,
            )
        }
    }
}
