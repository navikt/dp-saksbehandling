package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.job.Job
import java.time.LocalDateTime

internal class KlageAlarmJob(
    private val rapidsConnection: RapidsConnection,
    private val klageRepository: KlageRepository,
) : Job() {
    companion object {
        const val ALARM_INTERVAL_MND = 4L
    }

    override val jobName: String = "KlageAlarmJob"

    override suspend fun executeJob() {
        val sistEndretEldreEnn = LocalDateTime.now().minusMonths(ALARM_INTERVAL_MND)
        klageRepository
            .hentBehandlingerIkkeFerdigstilt(
                type = KlageBehandling.KlageTilstand.Type.BEHANDLES_AV_KLAGEINSTANS,
                sistEndretEldreEnn = sistEndretEldreEnn,
            ).forEach { behandling ->
                val alert =
                    AlertManager.KlageBehandlingIkkeFerdigstiltAlert(
                        behandlingId = behandling.behandlingId,
                        tilstand = behandling.tilstand,
                        sistEndret = behandling.sistEndret,
                    )
                logger.warn {
                    "Klagebehandling med id ${alert.behandlingId} har vært i tilstand ${alert.tilstand} i mer enn $ALARM_INTERVAL_MND måneder."
                }
                rapidsConnection.sendAlertTilRapid(
                    feilType = alert,
                    utvidetFeilMelding = alert.feilMelding,
                )
            }
    }

    override val logger: KLogger = KotlinLogging.logger {}
}
