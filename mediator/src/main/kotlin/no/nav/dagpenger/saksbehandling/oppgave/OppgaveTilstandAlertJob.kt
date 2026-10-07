package no.nav.dagpenger.saksbehandling.oppgave

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.job.Job

internal class OppgaveTilstandAlertJob(
    private val rapidsConnection: RapidsConnection,
    private val oppgaveTilAlertRepository: OppgaveTilAlertRepository,
) : Job() {
    override val jobName: String = "OppgaveTilstandAlertJob"

    override suspend fun executeJob() {
        oppgaveTilAlertRepository.hentOppgaverSomSkalVarsles().forEach {
            rapidsConnection.sendAlertTilRapid(
                feilType = it,
                utvidetFeilMelding = it.feilMelding,
            )
        }
    }

    override val logger: KLogger = KotlinLogging.logger {}
}

internal class OppgaveTilAlertRepository(
    private val databaseSession: DatabaseSession,
) {
    fun hentOppgaverSomSkalVarsles(): List<AlertManager.OppgaveOpprettetTilstandAlert> =
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  oppg.id AS oppgave_id,
                                oppg.endret_tidspunkt, 
                                beha.utlost_av
                        FROM    oppgave_v1 oppg
                        JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                        WHERE   oppg.tilstand = 'OPPRETTET'
                        """.trimIndent(),
                ).map { row ->
                    AlertManager.OppgaveOpprettetTilstandAlert(
                        oppgaveId = row.uuid("oppgave_id"),
                        sistEndret = row.localDateTime("endret_tidspunkt"),
                        utløstAvType = HendelseBehandler.valueOf(row.string("utlost_av")),
                    )
                }.asList,
            )
        }
}
