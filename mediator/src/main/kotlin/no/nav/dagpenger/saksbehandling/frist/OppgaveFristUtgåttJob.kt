package no.nav.dagpenger.saksbehandling.frist

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.PåVentFristUtgåttHendelse
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import kotlin.concurrent.fixedRateTimer

private val logger = KotlinLogging.logger {}

fun oppgaverSomIkkeLengerSkalVærePåVentJob(oppgaveMediator: OppgaveMediator) {
    val date = Date.from(Instant.now().atZone(ZoneId.of("Europe/Oslo")).toInstant())

    fixedRateTimer(
        name = "",
        daemon = true,
        startAt = date,
        period = 1.Dag,
        action = {
            try {
                logger.info { "Starter settOppgaverKlarTilBehandlingEllerUnderBehandling jobb" }
                håndterOppgaverSomIkkeLengerSkalVærePåVent(
                    mediator = oppgaveMediator,
                    frist = LocalDate.now(),
                )
            } catch (e: Exception) {
                logger.error(e) { "SettOppgaverKlarTilBehandlingEllerUnderBehandling feilet: ${e.message} " }
            }
        },
    )
}

fun håndterOppgaverSomIkkeLengerSkalVærePåVent(
    mediator: OppgaveMediator,
    frist: LocalDate = LocalDate.now(),
) {
    val oppgavIder = mediator.finnOppgaverPåVentMedUtgåttFrist(frist)
    logger.info { "${oppgavIder.size} oppgaver skal settes tilbake til KLAR_TIL_BEHANDLING/UNDER_BEHANDLING: $oppgavIder" }
    oppgavIder.forEach { oppgaveId ->
        mediator.håndterPåVentFristUtgått(PåVentFristUtgåttHendelse(oppgaveId = oppgaveId))
    }
}

private val Int.Dag get() = this * 1000L * 60L * 60L * 24L
