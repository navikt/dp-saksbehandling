package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.OppgaveMediator

private val logger = KotlinLogging.logger {}

internal class BehandlingsresultatMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
) : AbstractBehandlingsresultatMottak(rapidsConnection) {
    override fun requiredBehandletHendelseType(): List<String> = listOf("Søknad", "Manuell", "Meldekort", "Omgjøring")

    override val mottakNavn: String = "BehandlingsresultatMottak"

    override fun håndter(
        behandlingsresultat: Behandlingsresultat,
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        oppgaveMediator.hentOppgaveIdFor(behandlingsresultat.behandlingId)?.let {
            // sak statistikk greier:
            // Er dette vår sak?
            //

            oppgaveMediator.ferdigstillOppgave(
                vedtakFattetHendelse =
                    packet.vedtakFattetHendelse(
                        sak = null,
                        behandlingsresultat = behandlingsresultat,
                    ),
            )
        }
    }
}
