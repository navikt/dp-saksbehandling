package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class KlageBehandlingUtførtMottakForOppgave(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : AbstractKlageBehandlingUtførtMottak(rapidsConnection) {
    override val mottakNavn: String = "KlageBehandlingUtførtMottakForOppgave"

    override fun håndter(
        behandlingId: UUID,
        sakId: UUID,
        utfall: UtfallType,
        ident: String,
        saksbehandler: Saksbehandler,
    ) {
        withLoggingContext(
            "behandlingId" to behandlingId.toString(),
        ) {
            logger.info { "Ferdigstill oppgave for klageId/behandlingId: $behandlingId" }
            oppgaveMediator.ferdigstillOppgave(
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            )
        }
    }
}
