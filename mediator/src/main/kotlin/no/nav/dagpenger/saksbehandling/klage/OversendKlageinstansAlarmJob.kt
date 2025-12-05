package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.job.Job
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.OVERSEND_KLAGEINSTANS
import javax.sql.DataSource

internal class OversendKlageinstansAlarmJob(
    private val rapidsConnection: RapidsConnection,
    private val repository: OversendKlageinstansAlarmRepository,
) : Job() {
    override val jobName: String = "OversendKlageinstansAlarmJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() {
        repository
            .hentVentendeOversendelser(4)
            .also {
                logger.info { "Fant ${it.size} ventende oversendelser til klageinstans: $it" }
            }.forEach {
                rapidsConnection.sendAlertTilRapid(
                    it,
                    it.feilMelding,
                )
            }
    }
}

internal class OversendKlageinstansAlarmRepository(
    private val ds: DataSource,
) {
    fun hentVentendeOversendelser(intervallAntallTimer: Int): List<AlertManager.OversendKlageinstansIkkeFullført> =
        sessionOf(ds).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  *
                        FROM    klage_v1
                        WHERE   tilstand = :oversend_klageinstans 
                        AND     endret_tidspunkt < NOW() - INTERVAL '$intervallAntallTimer hours'
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "oversend_klageinstans" to OVERSEND_KLAGEINSTANS.name,
                        ),
                ).map { row ->
                    AlertManager.OversendKlageinstansIkkeFullført(
                        behandlingId = row.uuid("id"),
                        tilstand = row.string("tilstand"),
                        sistEndret = row.localDateTime("endret_tidspunkt"),
                    )
                }.asList,
            )
        }
}
