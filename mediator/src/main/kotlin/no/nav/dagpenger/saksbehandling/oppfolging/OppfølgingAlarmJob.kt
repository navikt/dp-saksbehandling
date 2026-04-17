package no.nav.dagpenger.saksbehandling.oppfolging

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.job.Job
import javax.sql.DataSource

internal class OppfølgingAlarmJob(
    private val rapidsConnection: RapidsConnection,
    private val oppfølgingAlarmRepository: OppfølgingAlarmRepository,
) : Job() {
    override val jobName: String = "OppfølgingAlarmJob"

    override suspend fun executeJob() {
        oppfølgingAlarmRepository
            .hentOppfølgingerSomIkkeErFerdigstilt(24)
            .also {
                logger.info { "Fant ${it.size} oppfølginger fast i FERDIGSTILL_STARTET: $it" }
            }.forEach {
                rapidsConnection.sendAlertTilRapid(
                    feilType = it,
                    utvidetFeilMelding = null,
                )
            }
    }

    override val logger: KLogger = KotlinLogging.logger {}
}

internal class OppfølgingAlarmRepository(
    private val dataSource: DataSource,
) {
    fun hentOppfølgingerSomIkkeErFerdigstilt(intervallAntallTimer: Int): List<AlertManager.OppfølgingIkkeFerdigstilt> =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  go.person_id         AS person_id,
                                go.id                AS oppfolging_id,
                                go.tilstand          AS tilstand,
                                go.endret_tidspunkt  AS endret_tidspunkt
                        FROM    oppfolging_v1 go
                        WHERE   go.tilstand = 'FERDIGSTILL_STARTET'
                        AND     go.endret_tidspunkt < NOW() - INTERVAL '$intervallAntallTimer hours'
                        """.trimIndent(),
                    paramMap = mapOf(),
                ).map { row ->
                    AlertManager.OppfølgingIkkeFerdigstilt(
                        oppfølgingId = row.uuid("oppfolging_id"),
                        tilstand = row.string("tilstand"),
                        sistEndret = row.localDateTime("endret_tidspunkt"),
                        personId = row.uuid("person_id"),
                    )
                }.asList,
            )
        }
}
