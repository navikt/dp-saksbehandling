package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.job.Job
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Avbrutt
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Distribuert
import javax.sql.DataSource

internal class UtsendingAlarmJob(
    private val rapidsConnection: RapidsConnection,
    private val utsendingAlarmRepository: UtsendingAlarmRepository,
) : Job() {
    override val jobName: String = "UtsendingAlarmJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() {
        utsendingAlarmRepository.hentVentendeUtsendinger(4).also {
            logger.info { "Fant ${it.size} ventende utsendinger: $it" }
        }.forEach {
            rapidsConnection.sendAlertTilRapid(
                feilType = it,
                utvidetFeilMelding = null,
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
                        SELECT per.id               AS person_id,
                               opp.id               AS oppgave_id,
                               beh.id               AS behandling_id,
                               uts.id               AS utsending_id,
                               uts.tilstand         AS utsending_tilstand,
                               uts.endret_tidspunkt AS utsending_endret_tidspunkt
                        FROM utsending_v1 uts
                                 JOIN oppgave_v1 opp ON uts.oppgave_id = opp.id
                                 JOIN behandling_v1 beh ON opp.behandling_id = beh.id
                                 JOIN person_v1 per ON beh.person_id = per.id
                        WHERE uts.tilstand != :distribuert
                          AND uts.tilstand != :avbrutt
                          AND uts.endret_tidspunkt < NOW() - INTERVAL '$intervallAntallTimer hours'
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "distribuert" to Distribuert.name,
                            "avbrutt" to Avbrutt.name,
                        ),
                ).map { row ->
                    AlertManager.UtsendingIkkeFullført(
                        utsendingId = row.uuid("utsending_id"),
                        tilstand = row.string("utsending_tilstand"),
                        sistEndret = row.localDateTime("utsending_endret_tidspunkt"),
                        oppgaveId = row.uuid("oppgave_id"),
                        behandlingId = row.uuid("behandling_id"),
                        personId = row.uuid("person_id"),
                    )
                }.asList,
            )
        }
    }
}
