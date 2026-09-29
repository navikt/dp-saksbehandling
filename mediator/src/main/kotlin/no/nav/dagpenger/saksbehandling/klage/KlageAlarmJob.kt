package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.job.Job
import java.time.LocalDateTime
import javax.sql.DataSource

internal class KlageAlarmJob(
    private val rapidsConnection: RapidsConnection,
    private val klageRepository: KlageAlarmJobRepository,
) : Job() {
    override val jobName: String = "KlageAlarmJob"

    override suspend fun executeJob() {
        val sistEndretEldreEnn = LocalDateTime.now().minusMonths(4)
        val klageBehandlinger =
            klageRepository.hentBehandlingerIkkeFerdigstil(
                type = KlageBehandling.KlageTilstand.Type.BEHANDLES_AV_KLAGEINSTANS,
                sistEndretEldreEnn = sistEndretEldreEnn,
            )

        klageBehandlinger.forEach { klageBehandling: AlertManager.KlageBehandlingIkkeFerdigstiltAlert ->
            logger.warn {
                "Klagebehandling med id ${klageBehandling.behandlingId} har vært i tilstand ${klageBehandling.tilstand} i mer enn 4 måneder."
            }
            rapidsConnection.sendAlertTilRapid(
                feilType = klageBehandling,
                utvidetFeilMelding = klageBehandling.feilMelding,
            )
        }
    }

    override val logger: KLogger = KotlinLogging.logger {}
}

internal class KlageAlarmJobRepository(
    private val dataSource: DataSource,
) {
    fun hentBehandlingerIkkeFerdigstil(
        type: KlageBehandling.KlageTilstand.Type,
        sistEndretEldreEnn: LocalDateTime,
    ): List<AlertManager.KlageBehandlingIkkeFerdigstiltAlert> =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  klage.id               AS klage_id,
                                klage.tilstand         AS klage_tilstand,
                                klage.endret_tidspunkt AS klage_endret_tidspunkt
                        FROM    klage_v1 klage
                        WHERE   klage.tilstand = :type
                        AND     klage.endret_tidspunkt < :endretTidspunkt
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "type" to type.toString(),
                            "endretTidspunkt" to sistEndretEldreEnn,
                        ),
                ).map { row ->
                    AlertManager.KlageBehandlingIkkeFerdigstiltAlert(
                        behandlingId = row.uuid("klage_id"),
                        tilstand = row.string("klage_tilstand"),
                        sistEndret = row.localDateTime("klage_endret_tidspunkt"),
                    )
                }.asList,
            )
        }
}
