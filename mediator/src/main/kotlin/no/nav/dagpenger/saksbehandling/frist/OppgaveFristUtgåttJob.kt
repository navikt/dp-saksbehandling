package no.nav.dagpenger.saksbehandling.frist

import mu.KLogger
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.PåVentFristUtgåttHendelse
import no.nav.dagpenger.saksbehandling.job.Job
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

class OppgaveFristUtgåttJob(
    private val oppgaveMediator: OppgaveMediator,
) : Job() {
    override val jobName: String = "OppgaveFristUtgåttJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() {
        val oppgavIder = oppgaveMediator.finnOppgaverPåVentMedUtgåttFrist(LocalDate.now())
        logger.info { "${oppgavIder.size} oppgaver skal settes tilbake til KLAR_TIL_BEHANDLING/UNDER_BEHANDLING: $oppgavIder" }
        oppgavIder.forEach { oppgaveId ->
            oppgaveMediator.håndterPåVentFristUtgått(PåVentFristUtgåttHendelse(oppgaveId = oppgaveId))
        }
    }
}
