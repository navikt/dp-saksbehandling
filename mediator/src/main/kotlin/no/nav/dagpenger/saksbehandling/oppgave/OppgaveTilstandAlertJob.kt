package no.nav.dagpenger.saksbehandling.oppgave

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.AlertManager.sendAlertTilRapid
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.job.Job

internal class OppgaveTilstandAlertJob(
    private val rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : Job() {
    override val jobName: String = "OppgaveTilstandAlertJob"

    override suspend fun executeJob() {
        oppgaveMediator
            .hentAlleOppgaverMedTilstand(Oppgave.Tilstand.Type.OPPRETTET)
            .map {
                AlertManager.OppgaveOpprettetTilstandAlert(
                    oppgaveId = it.oppgaveId,
                    sistEndret = it.opprettet,
                    utløstAvType = it.behandling.utløstAv,
                )
            }.forEach {
                rapidsConnection.sendAlertTilRapid(
                    feilType = it,
                    utvidetFeilMelding = it.feilMelding,
                )
            }
    }

    override val logger: KLogger = KotlinLogging.logger {}
}
