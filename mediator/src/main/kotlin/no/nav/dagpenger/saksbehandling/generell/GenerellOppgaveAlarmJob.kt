package no.nav.dagpenger.saksbehandling.generell

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.job.Job
import javax.sql.DataSource

internal class GenerellOppgaveAlarmJob(
    private val rapidsConnection: RapidsConnection,
    private val generellOppgaveAlarmRepository: GenerellOppgaveAlarmRepository,
) : Job() {
    override val jobName: String = "GenerellOppgaveAlarmJob"

    override suspend fun executeJob() {
        generellOppgaveAlarmRepository
            .hentGenerelleOppgaverSomIkkeErFerdigstilt(24)
            .also {
                logger.info { "Fant ${it.size} generelle oppgaver fast i FERDIGSTILL_STARTET: $it" }
            }.forEach {
                rapidsConnection.sendAlertTilRapid(
                    feilType = it,
                    utvidetFeilMelding = null,
                )
            }
    }

    override val logger: KLogger = KotlinLogging.logger {}
}

internal class GenerellOppgaveAlarmRepository(
    private val dataSource: DataSource,
) {
    fun hentGenerelleOppgaverSomIkkeErFerdigstilt(intervallAntallTimer: Int): List<AlertManager.GenerellOppgaveIkkeFerdigstilt> =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  go.person_id         AS person_id,
                                go.id                AS generell_oppgave_id,
                                go.tilstand          AS tilstand,
                                go.endret_tidspunkt  AS endret_tidspunkt
                        FROM    generell_oppgave_v1 go
                        WHERE   go.tilstand = 'FERDIGSTILL_STARTET'
                        AND     go.endret_tidspunkt < NOW() - INTERVAL '$intervallAntallTimer hours'
                        """.trimIndent(),
                    paramMap = mapOf(),
                ).map { row ->
                    AlertManager.GenerellOppgaveIkkeFerdigstilt(
                        generellOppgaveId = row.uuid("generell_oppgave_id"),
                        tilstand = row.string("tilstand"),
                        sistEndret = row.localDateTime("endret_tidspunkt"),
                        personId = row.uuid("person_id"),
                    )
                }.asList,
            )
        }
}
