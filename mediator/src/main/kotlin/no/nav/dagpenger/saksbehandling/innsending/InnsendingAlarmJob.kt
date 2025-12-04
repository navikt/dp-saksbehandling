package no.nav.dagpenger.saksbehandling.innsending

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.job.Job
import javax.sql.DataSource
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid

internal class InnsendingAlarmJob(
    private val rapidsConnection: RapidsConnection,
    private val innsendingAlarmRepository: InnsendingAlarmRepository,
) : Job() {
    override val jobName: String = "InnsendingAlarmJob"

    override suspend fun executeJob() {
        innsendingAlarmRepository.hentInnsendingerSomIkkeErFerdigstilt(24).also {
            logger.info { "Fant ${it.size} ventende innsendinger: $it" }
        }.forEach {
            rapidsConnection.sendAlertTilRapid(
                feilType = it,
                utvidetFeilMelding = null,
            )
        }
    }

    override val logger: KLogger = KotlinLogging.logger {}

}

internal class InnsendingAlarmRepository(private val dataSource: DataSource) {
    fun hentInnsendingerSomIkkeErFerdigstilt(intervallAntallTimer: Int): List<AlertManager.InnsendingIkkeFerdigstilt> {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  inns.person_id        AS person_id,
                                inns.id               AS innsending_id,
                                inns.tilstand         AS innsending_tilstand,
                                inns.endret_tidspunkt AS innsending_endret_tidspunkt,
                                inns.journalpost_id   AS journalpost_id
                        FROM    innsending_v1 inns
                        WHERE   inns.tilstand = 'FERDIGSTILL_STARTET'
                        AND     inns.endret_tidspunkt < NOW() - INTERVAL '$intervallAntallTimer hours'
                        """.trimIndent(),
                    paramMap = mapOf(),
                ).map { row ->
                    AlertManager.InnsendingIkkeFerdigstilt(
                        innsendingId = row.uuid("innsending_id"),
                        tilstand = row.string("innsending_tilstand"),
                        sistEndret = row.localDateTime("innsending_endret_tidspunkt"),
                        journalpostId = row.string("journalpost_id"),
                        personId = row.uuid("person_id"),
                    )
                }.asList,
            )
        }
    }
}
