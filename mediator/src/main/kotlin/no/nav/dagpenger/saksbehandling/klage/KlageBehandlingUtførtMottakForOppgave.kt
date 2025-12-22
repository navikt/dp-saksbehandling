package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

internal class KlageBehandlingUtførtMottakForOppgave(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : AbstractKlageBehandlingUtførtMottak(rapidsConnection) {
    override val mottakNavn: String = "KlageBehandlingUtførtMottakForOppgave"

    override fun håndter(
        behandlingId: UUID,
        utfall: UtfallType,
        ident: String,
        saksbehandler: Saksbehandler,
    ) {
        oppgaveMediator.ferdigstillOppgave(
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
        )
    }
}
