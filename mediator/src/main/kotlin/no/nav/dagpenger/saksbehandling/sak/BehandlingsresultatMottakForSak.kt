package no.nav.dagpenger.saksbehandling.sak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.mottak.AbstractBehandlingResultatMottak
import no.nav.dagpenger.saksbehandling.mottak.BehandlingResultat

private val logger = KotlinLogging.logger {}

internal class BehandlingsresultatMottakForSak(
    rapidsConnection: RapidsConnection,
    private val sakRepository: SakRepository,
    private val sakMediator: SakMediator,
) : AbstractBehandlingResultatMottak(rapidsConnection) {
    override fun requiredBehandletHendelseType(): List<String> = listOf("Søknad")

    override val mottakNavn: String = "BehandlingsresultatMottakForSak"

    override fun håndter(
        behandlingResultat: BehandlingResultat,
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        if (behandlingResultat.nyDagpengerettInnvilget().also {
                logger.info { "BehandlingsresultatMottakForSak med utfall: $it. Basert på $behandlingResultat" }
            }
        ) {
            val sakId = sakRepository.hentSakIdForBehandlingId(behandlingResultat.behandlingId).toString()
            logger.info { "Vedtak skal tilhøre dp-sak " }
            val vedtakFattetHendelse =
                packet.vedtakFattetHendelse(
                    sak =
                        UtsendingSak(
                            id = sakId,
                            kontekst = "Dagpenger",
                        ),
                    behandlingResultat = behandlingResultat,
                )
            sakMediator.merkSakenSomDpSak(vedtakFattetHendelse = vedtakFattetHendelse)
        }
    }
}
